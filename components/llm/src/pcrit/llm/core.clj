(ns pcrit.llm.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
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

(defn- -parse-provider [model-name]
  (keyword (first (str/split model-name #"/"))))

(defn- -calculate-cost [model-name token-in token-out]
  (if-let [pricing (get config/price-table model-name)]
    (+ (* (/ token-in 1000.0) (:in-per-1k pricing))
       (* (/ token-out 1000.0) (:out-per-1k pricing)))
    0.0))

(defn parse-llm-response
  "Parses a clj-http response map to extract content and all generation metadata.
  Returns a map containing the prompt content and a nested :generation-metadata map."
  [response model-name]
  (log/debug "Raw LLM response body from" model-name ":" (:body response))
  (try
    (let [parsed-body (json/read-str (:body response) :key-fn keyword)
          usage       (:usage parsed-body)
          token-in    (or (:prompt_tokens usage) 0)
          token-out   (or (:completion_tokens usage) 0)
          content     (-> parsed-body :choices first :message :content)
          header-cost (some-> response :headers (get "x-litellm-cost") Double/parseDouble)
          body-cost   (or (:cost parsed-body) (:cost usage) (:total_cost usage))
          calculated-cost (-calculate-cost model-name token-in token-out)]
      (if content
        {:content content
         :generation-metadata {:model             model-name
                               :provider          (-parse-provider model-name)
                               :token-in          token-in
                               :token-out         token-out
                               :cost-usd-snapshot (or body-cost header-cost calculated-cost)}}
        (do
          (log/error "Could not extract content from LLM response for" model-name ". Body:" (:body response))
          {:error (str "No content in LLM response: " (pr-str parsed-body))})))
    (catch Exception e
      (log/error "Failed to parse LLM JSON response for" model-name ". Error:" (.getMessage e) ". Body:" (:body response))
      {:error (str "Malformed JSON from LLM: " (.getMessage e))})))

(defn call-model
  [model-name prompt-string & {:keys [timeout post-fn]
                               :or {timeout (get-in config/config [:llm :default-timeout-ms])
                                    post-fn http/post}}]
  (let [endpoint (get-in config/config [:llm :endpoint])]
    (log/info "\n;; --- Calling LLM:" model-name "via" endpoint "---")
    (log/info ";; --- Using timeout:" timeout "ms ---")
    (try
      (let [request-body {:model model-name
                          :messages [{:role "user" :content prompt-string}]}
            headers {"Authorization" (str "Bearer " LITELLM_API_KEY)}
            start-time (System/currentTimeMillis)
            response (post-fn endpoint
                              {:body (json/write-str request-body)
                               :content-type :json
                               :accept :json
                               :headers headers
                               :throw-exceptions false
                               :socket-timeout timeout
                               :connection-timeout timeout})
            duration-ms (- (System/currentTimeMillis) start-time)]
        (if (= 200 (:status response))
          (let [{:keys [content generation-metadata error]} (parse-llm-response response model-name)]
            (if error
              {:error error}
              {:content content
               :generation-metadata (assoc generation-metadata :duration-ms duration-ms)}))
          (do
            (log/error "LLM call to" model-name "failed with status" (:status response) ". Body:" (:body response))
            {:error (str "LLM API Error: " (:status response) " " (:body response))})))
      (catch Exception e
        (log/error "Exception during LLM call to" model-name ". Error:" (.getMessage e))
        {:error (str "Network or client exception: " (.getMessage e))}))))
