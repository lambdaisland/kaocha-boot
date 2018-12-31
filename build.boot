(set-env! :source-paths #{"src"}
          :dependencies '[[seancorfield/boot-tools-deps "0.4.7" :scope "test" :exclusions [org.clojure/clojure]]])

(require '[boot-tools-deps.core :refer [deps]]
         '[kaocha.boot-task :refer [kaocha]])
