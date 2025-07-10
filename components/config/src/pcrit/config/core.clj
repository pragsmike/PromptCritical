(ns pcrit.config.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [pcrit.log.interface :as log]))

(def config
  "A centralized map for all application configuration."
  {:llm {:endpoint "http://localhost:8000/chat/completions"
         :default-timeout-ms 300000}
   :locking {:retry-ms 50
             :retry-jitter-ms 25
             :max-retries 100
             :stale-lock-threshold-ms (* 10 60 1000) ; 10 minutes
             }})

(defn- evolution-params-file [{:keys [exp-dir]}]
  (io/file exp-dir "evolution-parameters.edn"))

(defn load-evolution-params
  "Reads evolution-parameters.edn from the experiment directory.
  Returns a map of parameters, or an empty map if the file is not found or corrupt."
  [ctx]
  (let [f (evolution-params-file ctx)]
    (if (.exists f)
      (try
        (edn/read-string (slurp f))
        (catch Exception e
          (log/error "Failed to read or parse evolution-parameters.edn:" (.getMessage e))
          {}))
      {})))
