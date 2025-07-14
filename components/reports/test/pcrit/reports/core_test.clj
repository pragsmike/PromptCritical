(ns pcrit.reports.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.reports.core :as reports]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LEGACY CSV PARSER TESTS (for historical data)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest parse-report-legacy-test
  (testing "Successfully parsing a valid legacy report.csv file"
    (let [csv-content (str "prompt,score,cost\n"
                           "P3,95.5,0.001\n"
                           "P1,80.0,0.002\n"
                           "P2,99.9,0.003\n")
          report-file (io/file (get-temp-dir) "report.csv")]
      (spit report-file csv-content)
      (let [parsed-data (reports/parse-report report-file)]
        (is (= 3 (count parsed-data)))
        (is (= {:prompt "P3" :score 95.5 :cost 0.001} (first parsed-data)))
        (is (= {:prompt "P2" :score 99.9 :cost 0.003} (last parsed-data))))))

  (testing "Legacy parser handles file that does not exist"
    (let [report-file (io/file (get-temp-dir) "nonexistent.csv")]
      (is (empty? (reports/parse-report report-file))))))

  (testing "Legacy parser handles non-numeric scores"
    (let [csv-content (str "prompt,score\n"
                           "P1,90\n"
                           "P2,not-a-number\n"
                           "P3,85.5\n")
          report-file (io/file (get-temp-dir) "report.csv")]
      (spit report-file csv-content)
      (let [parsed-data (reports/parse-report report-file)]
        (is (= 2 (count parsed-data)))
        (is (= [90.0 85.5] (map :score parsed-data))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NEW FAILTER JSON PROCESSING AND CSV WRITING TESTS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private sample-failter-parsed-json
  [{:prompt_id "P1.prompt"
    :score 95
    :usage {:model_used "openai/gpt-4o-mini" :tokens_in 1000 :tokens_out 2000}
    :performance {:retry_attempts 0}
    :error nil}
   {:prompt_id "P2.prompt"
    :score 80
    :usage {:model_used "ollama/qwen3:8b" :tokens_in 500 :tokens_out 500}
    :performance {:retry_attempts 1}
    :error nil}
   {:prompt_id "P3.prompt"
    :score nil
    :usage {:model_used "openai/gpt-4o-mini"}
    :performance {}
    :error "API call timed out"}])

(deftest process-and-write-csv-report-test
  (testing "Correctly processes parsed data, calculates costs, and writes a CSV file"
    (let [target-csv (io/file (get-temp-dir) "new-report.csv")
          processed-data (reports/process-and-write-csv-report! sample-failter-parsed-json (.getCanonicalPath target-csv))]

      (testing "Returns correctly structured data with string values"
        (is (= 3 (count processed-data)))
        (let [p1-data (first processed-data)]
          (is (= "P1" (:prompt p1-data)))
          (is (= "95" (:score p1-data)))
          (is (= "openai/gpt-4o-mini" (:model p1-data)))
          (is (< (Math/abs (- (Double/parseDouble (:cost p1-data)) 0.00135)) 1e-9))
          (is (= "1000" (:tokens-in p1-data)))))

      (testing "Writes a valid CSV file with headers"
        (is (.exists target-csv))
        (let [csv-content (slurp target-csv)]
          (is (.startsWith csv-content "prompt,score,model,cost,tokens-in,tokens-out,retry-attempts,error\n"))
          (is (.contains csv-content "\nP1,95,openai/gpt-4o-mini,0.00135"))
          (is (.contains csv-content "\nP2,80,ollama/qwen3:8b,0.0"))
          (is (.contains csv-content "\nP3,,openai/gpt-4o-mini,0.0,,")))))))
