(ns pcrit.config.core)

(def config
  "A centralized map for all application configuration."
  {:llm {:endpoint "http://localhost:8000/chat/completions"
         :default-timeout-ms 300000}
   :locking {:retry-ms 50
             :retry-jitter-ms 25
             :max-retries 100
             :stale-lock-threshold-ms (* 10 60 1000) ; 10 minutes
             }})
