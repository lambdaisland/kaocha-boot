(ns kaocha.boot-task
  {:boot/export-tasks true}
  (:refer-clojure :exclude [test])
  (:require [boot.pod  :as pod]
            [boot.task.built-in :refer [target]]
            [boot.core :as boot]
            [clojure.java.io :as io]))

(defn make-kaocha-pod []
  (-> (boot/get-env)
      (update-in [:dependencies] conj
                 '[lambdaisland/kaocha "0.0-319"]
                 )
      pod/make-pod))

(boot/deftask kaocha
  "Run tests with Kaocha."
  [_ config-file     FILE    file "Config file to read."
   _ print-config            bool "Print out the fully merged and normalized config, then exit."
   _ print-test-plan         bool "Load tests, build up a test plan, then print out the test plan and exit."
   _ print-result            bool "Print the test result map as returned by the Kaocha API."
   _ fail-fast               bool "Stop testing after the first failure."
   _ no-color                bool "Disable ANSI color codes in output."
   _ watch                   bool "Watch filesystem for changes and re-run tests."
   _ reporter        SYMBOL  sym  "Change the test reporter, can be specified multiple times."
   _ plugin          KEYWORD [kw] "Load the given plugins."
   _ version                 bool "Print version information and quit."
   _ suite           KEYWORD [kw] "Test suite(s) to run."]
  (let [pod (make-kaocha-pod)]
    (pod/with-eval-in pod
      (println (clojure-version))
      (require '[kaocha.config :as config]
               '[kaocha.plugin :as plugin]
               '[kaocha.api :as api]
               '[kaocha.jit :refer [jit]]
               '[clojure.set :as set]
               '[clojure.string :as str]
               '[slingshot.slingshot :refer [try+]])
      (try+
       (let [config         (config/load-config (:config-file ~*opts* "tests.edn"))
             plugin-chain   (plugin/load-all (concat (:kaocha/plugins config) ~plugin))
             config         (-> config
                                (config/apply-cli-opts ~*opts*)
                                (config/apply-cli-args ~suite))
             suites         (into #{} (map :kaocha.testable/id) (:kaocha/tests config))
             unknown-suites (set/difference (set ~suite) (set suites))
             exit-code
             (plugin/with-plugins plugin-chain
               (cond
                 ~version
                 (do (println ((jit kaocha.runner/kaocha-version))) 0)

                 ~print-config
                 (binding [clojure.core/*print-namespace-maps* false]
                   ((jit clojure.pprint/pprint) (plugin/run-hook :kaocha.hooks/config config))
                   0)

                 ~print-test-plan
                 (binding [clojure.core/*print-namespace-maps* false]
                   ((jit clojure.pprint/pprint) (api/test-plan (plugin/run-hook :kaocha.hooks/config config)))
                   0)

                 (seq unknown-suites)
                 (do
                   (println (str "No such suite: "
                                 (str/join ", " (sort unknown-suites))
                                 ", valid options: "
                                 (str/join ", " (sort suites))
                                 "."))
                   -2)

                 ~watch
                 (do
                   ((jit kaocha.watch/run) config) 1) ; exit 1 because only an anomaly would break this loop

                 ~print-result
                 (let [result (api/run (assoc config :kaocha/reporter []))
                       totals ((jit kaocha.result/totals) (:kaocha.result/tests result))]
                   (binding [clojure.core/*print-namespace-maps* false]
                     ((jit clojure.pprint/pprint) result))
                   (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255))

                 :else
                 (do
                   (plugin/run-hook :kaocha.hooks/main config)
                   (let [result (plugin/run-hook :kaocha.hooks/post-summary (api/run config))
                         totals ((jit kaocha.result/totals) (:kaocha.result/tests result))]
                     (min (+ (:kaocha.result/error totals) (:kaocha.result/fail totals)) 255)))))]
         (when (not= 0 exit-code)
           (boot.util/exit-error exit-code)))

       (catch :kaocha/early-exit {exit-code :kaocha/early-exit}
         (when (not= 0 exit-code)
           (boot.util/exit-error exit-code)))))))
