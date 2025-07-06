(ns pcrit.llm.core-test
  (:require [clojure.test :refer :all]
            [pcrit.llm.core :as llm]
            [pcrit.config.interface :as config]
            [clojure.data.json :as json]
            [pcrit.test-helper.interface :refer [with-quiet-logging]]))

(use-fixtures :once with-quiet-logging)

(deftest parse-llm-response-test
  (testing "Successfully parsing a valid response"
    (let [json-body (json/write-str {:choices [{:message {:content "Hello, world!"}}]
                                     :usage {:total_tokens 10}
                                     :cost 0.0001})
          result (llm/parse-llm-response json-body "test-model")]
      (is (= "Hello, world!" (:content result)))
      (is (= {:total_tokens 10} (:usage result)))
      (is (= 0.0001 (:cost result)))))

  (testing "Handling a response with no content"
    (let [json-body (json/write-str {:choices [{:message {:role "assistant"}}]})
          result (llm/parse-llm-response json-body "test-model")]
      (is (string? (:error result)))
      (is (.startsWith (:error result) "No content in LLM response"))))

  (testing "Handling malformed JSON"
    (let [malformed-body "{\"choices\": [{\"message\": "
          result (llm/parse-llm-response malformed-body "test-model")]
      (is (string? (:error result)))
      (is (.startsWith (:error result) "Malformed JSON from LLM")))))

(deftest call-model-test
  (let [last-call (atom nil)
        mock-post-fn (fn [url options]
                       (reset! last-call {:url url :options options})
                       ;; Default mock response
                       {:status 200
                        :body (json/write-str {:choices [{:message {:content "Mock response"}}]})})]

    (testing "Successful API call with default mock"
      (let [result (llm/call-model "test-model" "A prompt" :post-fn mock-post-fn)]
        (is (= (get-in config/config [:llm :endpoint]) (:url @last-call)))
        (is (= "Mock response" (:content result)))
        (is (some? (get-in @last-call [:options :headers "Authorization"])))
        (is (= "test-model" (-> @last-call :options :body (json/read-str :key-fn keyword) :model)))))

    (testing "API returns an error status"
      (let [error-mock (fn [url options] {:status 500 :body "Server error"})
            result (llm/call-model "error-model" "A prompt" :post-fn error-mock)]
        (is (string? (:error result)))
        (is (= "LLM API Error: 500 Server error" (:error result)))))

    (testing "HTTP client throws an exception"
      (let [exception-mock (fn [url options] (throw (Exception. "Connection timeout")))
            result (llm/call-model "exception-model" "A prompt" :post-fn exception-mock)]
        (is (string? (:error result)))
        (is (= "Network or client exception: Connection timeout" (:error result)))))))
