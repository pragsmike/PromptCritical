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

;; REFACTORED: Now accepts the entire response map to centralize parsing.
(defn parse-llm-response
  "Parses a clj-http response map to extract content, usage, and cost.
  Returns a map with {:content, :usage, :cost}, pulling cost from
  several potential locations in the body and headers."
  [response model-name]
  (log/debug (str "Response from " model-name "\n" (:body response)))
  (try
    (let [parsed-body (json/read-str (:body response) :key-fn keyword)
          usage       (:usage parsed-body)
          content     (-> parsed-body :choices first :message :content)
          header-cost (some-> response :headers (get "x-litellm-cost") Double/parseDouble)
          body-cost   (or (:cost parsed-body) ; Top-level
                          (:cost usage)         ; LiteLLM v1.x
                          (:total_cost usage))] ; LiteLLM v2.x
      (if content
        {:content content
         :usage   usage
         :cost    (or body-cost header-cost 0.0)} ; Centralized cost logic
        (do
          (log/error (str "Could not extract content from LLM response for " model-name ". Body: " (:body response)))
          {:error (str "No content in LLM response: " (pr-str parsed-body))})))
    (catch Exception e
      (log/error (str "Failed to parse LLM JSON response for " model-name ". Error: " (.getMessage e) ". Body: " (:body response)))
      {:error (str "Malformed JSON from LLM: " (.getMessage e))})))

;; REFACTORED: Now delegates all parsing to `parse-llm-response`.
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
          (parse-llm-response response model-name) ; Simplified call
          (do
            (log/error (str "LLM call to " model-name " failed with status " (:status response) ". Body: " (:body response)))
            {:error (str "LLM API Error: " (:status response) " " (:body response))})))
      (catch Exception e
        (log/error (str "Exception during LLM call to " model-name ". Error: " (.getMessage e)))
        {:error (str "Network or client exception: " (.getMessage e))}))))
