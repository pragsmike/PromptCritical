(ns pcrit.config.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [pcrit.log.interface :as log]))

(def defaults
  "A map of application-wide default values."
  {:selection-policy "top-N=5"})

(def price-table
  "A map of model names to their cost per 1000 tokens for input and output.
  Source: Public pricing pages as of July 2025."
  {;; --- OpenAI Models ---
   "openai/gpt-4o"          {:in-per-1k 0.005,   :out-per-1k 0.015}
   "openai/gpt-4o-mini"     {:in-per-1k 0.00015, :out-per-1k 0.0006}
   "openai/gpt-4-turbo"     {:in-per-1k 0.01,    :out-per-1k 0.03}
   "openai/gpt-3.5-turbo"   {:in-per-1k 0.0005,  :out-per-1k 0.0015}

   ;; --- Anthropic Models ---
   "anthropic/claude-3-opus"  {:in-per-1k 0.015,   :out-per-1k 0.075}
   "anthropic/claude-3-sonnet" {:in-per-1k 0.003,   :out-per-1k 0.015}
   "anthropic/claude-3-haiku" {:in-per-1k 0.00025, :out-per-1k 0.00125}

   ;; --- Google Gemini Models ---
   "google/gemini-1.5-pro"    {:in-per-1k 0.0035,  :out-per-1k 0.0105}
   "google/gemini-1.5-flash"  {:in-per-1k 0.00035, :out-per-1k 0.00105}

   ;; --- Perplexity Models ---
   "perplexity/llama-3-sonar-small-32k-online" {:in-per-1k 0.0002, :out-per-1k 0.0002}
   "perplexity/llama-3-sonar-large-32k-online" {:in-per-1k 0.001,  :out-per-1k 0.001}

   ;; --- Non-standard/Hypothetical OpenAI Models from litellm config (cost assumed to be 0) ---
   "openai/gpt-4.1-nano"    {:in-per-1k 0.0, :out-per-1k 0.0}
   "openai/gpt-4.1-mini"    {:in-per-1k 0.0, :out-per-1k 0.0}
   "openai/gpt-4.1"         {:in-per-1k 0.0, :out-per-1k 0.0}
   "openai/o3"              {:in-per-1k 0.0, :out-per-1k 0.0}
   "openai/o3-pro"          {:in-per-1k 0.0, :out-per-1k 0.0}

   ;; --- Self-Hosted Ollama Models from litellm config (cost is 0) ---
   "ollama/mistral"           {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:32b"         {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:30b"         {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:14b"         {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/mistral-nemo:12b"  {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/gemma3:12b"        {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:8b"          {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/granite3.3:8b"     {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:4b"          {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:1.7b"        {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/granite3.3:2b"     {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/gemma3:1b"         {:in-per-1k 0.0, :out-per-1k 0.0}
   })

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
