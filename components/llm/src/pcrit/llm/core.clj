(ns pcrit.llm.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [pcrit.config.interface :as config]
            [pcrit.log.interface :as log]))

(def LITELLM_API_KEY (System/getenv "LITELLM_API_KEY"))

(defn pre-flight-checks []
  (if (seq LITELLM_API_KEY)
    true
    (do
      (log/error "---")
      (log/error "LITELLM_API_KEY environment variable not set.")
      (log/error "Please set it before running the application.")
      false)))

;; UPDATED: Now looks for cost in multiple locations within the response body.
(defn parse-llm-response [response-body model-name]
  (log/debug (str "Response from " model-name "\n" response-body))
  (try
    (let [parsed (json/read-str response-body :key-fn keyword)
          usage (:usage parsed)
          content (-> parsed :choices first :message :content)
          cost (or (:cost parsed)         ; Top-level (current attempt)
                   (:cost usage)         ; LiteLLM v1.x style
                   (:total_cost usage))] ; LiteLLM v2.x style
      (if content
        {:content content
         :usage   usage
         :cost    cost}
        (do
          (log/error (str "Could not extract content from LLM response for " model-name ". Body: " response-body))
          {:error (str "No content in LLM response: " (pr-str parsed))})))
    (catch Exception e
      (log/error (str "Failed to parse LLM JSON response for " model-name ". Error: " (.getMessage e) ". Body: " response-body))
      {:error (str "Malformed JSON from LLM: " (.getMessage e))})))

;; UPDATED: Now checks for and prefers header-based cost as a fallback.
(defn call-model
  [model-name prompt-string & {:keys [timeout post-fn]
                               :or {timeout (get-in config/config [:llm :default-timeout-ms])
                                    post-fn http/post}}]
  (let [endpoint (get-in config/config [:llm :endpoint])]
    (log/info (str "\n;; --- Calling LLM: " model-name " via " endpoint " ---"))
    (log/info (str ";; --- Using timeout: " timeout "ms ---"))
    (try
      (let [request-body {:model model-name
                          :messages [{:role "user" :content prompt-string}]}
            headers {"Authorization" (str "Bearer " LITELLM_API_KEY)}
            response (post-fn endpoint
                              {:body (json/write-str request-body)
                               :content-type :json
                               :accept :json
                               :headers headers
                               :throw-exceptions false
                               :socket-timeout timeout
                               :connection-timeout timeout})]
        (if (= 200 (:status response))
          (let [parsed (parse-llm-response (:body response) model-name)
                header-cost (some-> response :headers (get "x-litellm-cost") Double/parseDouble)]
            ;; Prefer cost from body, fall back to header cost, then to 0.0
            (assoc parsed :cost (or (:cost parsed) header-cost 0.0)))
          (do
            (log/error (str "LLM call to " model-name " failed with status " (:status response) ". Body: " (:body response)))
            {:error (str "LLM API Error: " (:status response) " " (:body response))})))
      (catch Exception e
        (log/error (str "Exception during LLM call to " model-name ". Error: " (.getMessage e)))
        {:error (str "Network or client exception: " (.getMessage e))}))))
