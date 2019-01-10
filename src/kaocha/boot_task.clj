(ns kaocha.boot-task
  {:boot/export-tasks true}
  (:refer-clojure :exclude [test])
  (:require [boot.pod :as pod]
            [boot.task.built-in :refer [target]]
            [boot.core :as boot]
            [clojure.java.io :as io]))

(defn make-kaocha-pod []
  (-> (boot/get-env)
      (update-in [:dependencies] conj
                 '[lambdaisland/kaocha "0.0-337"])
      pod/make-pod))

(boot/deftask kaocha
  "Run tests with Kaocha."
  [c config-file     FILE    file "Config file to read."
   H test-help               bool "Display Kaocha-boot usage information."
   _ print-config            bool "Print out the fully merged and normalized config, then exit."
   _ print-test-plan         bool "Load tests, build up a test plan, then print out the test plan and exit."
   _ print-result            bool "Print the test result map as returned by the Kaocha API."
   _ fail-fast               bool "Stop testing after the first failure."
   _ color                   bool "Enable ANSI color codes in output."
   _ no-color                bool "Disable ANSI color codes in output."
   _ watch                   bool "Watch filesystem for changes and re-run tests."
   _ no-watch                bool "Don't watch filesystem for changes."
   _ reporter        SYMBOL  sym  "Change the test reporter, can be specified multiple times."
   _ plugin          KEYWORD [kw] "Load the given plugins."
   _ version                 bool "Print version information and quit."
   s suite           KEYWORD [kw] "Test suite(s) to run."
   o options         EDN     edn  "Extra command line flags"]
  (let [pod (make-kaocha-pod)]
    (pod/with-eval-in pod
      (require '[kaocha.config :as config]
               '[kaocha.plugin :as plugin]
               '[kaocha.api :as api]
               '[kaocha.jit :refer [jit]]
               '[kaocha.runner :as runner]
               '[kaocha.classpath :as classpath]
               '[clojure.set :as set]
               '[clojure.string :as str]
               '[clojure.tools.cli :as cli]
               '[slingshot.slingshot :refer [try+]])

      (defn plugin-option-summary [specs]
        (apply str
               "\n"
               "  -o, --options EDN                  EDN map of additional options.\n\n"
               "Plugin-specific options can be specified using map syntax, e.g.\n\n"
               "   boot kaocha --options '{:randomize false}'\n\n"
               "These additional options are recognized:\n\n"
               (interpose "\n"
                          (map (fn [{:keys [id required desc]}]
                                 (format "   %-30s %s" (str id " " (or required "BOOL")) desc))
                               (#'cli/compile-option-specs specs)))))

      (try+
       (with-redefs [classpath/add-classpath boot.pod/add-classpath]
         (let [options           (merge (cond-> ~*opts*
                                          ~no-color (assoc :color false)
                                          ~no-watch (assoc :watch false)
                                          :always   (dissoc :no-color :no-watch))
                                        ~options)
               config            (config/load-config (:config-file options "tests.edn"))
               plugin-chain      (plugin/load-all (concat (:kaocha/plugins config) ~plugin))
               plugin-options    (plugin/run-hook* plugin-chain :kaocha.hooks/cli-options [])
               {:keys [summary]} (cli/parse-opts [] @#'runner/cli-options)
               config            (-> config
                                     (config/apply-cli-opts options)
                                     (config/apply-cli-args ~suite))
               exit-code
               (plugin/with-plugins plugin-chain
                 (runner/run {:config  config
                              :options options
                              :summary (str "USAGE:\n\nboot kaocha [OPTIONS]...\n\n"
                                            "  -s, --suite SUITE                  Test suite(s) to run.\n"
                                            summary (plugin-option-summary plugin-options))
                              :suites  ~suite}))]
           (when (not= 0 exit-code)
             (System/exit exit-code))))

       (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
         (when (not= 0 exit-code)
           (System/exit exit-code)))))))
