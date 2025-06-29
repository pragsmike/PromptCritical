(ns pcrit.config)

(def config
  "A centralized map for all application configuration."
  {:llm {:endpoint "http://localhost:8000/chat/completions"
         :default-timeout-ms 300000}
  })
