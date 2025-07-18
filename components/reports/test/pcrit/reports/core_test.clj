(ns pcrit.reports.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.reports.core :as reports]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(def ^:private sample-normalized-results
  "This data simulates the output of `pcrit.results/parse-report`."
  [{:prompt "P1", :score 95, :cost 0.00135, :model "openai/gpt-4o-mini",
    :tokens-in 1000, :tokens-out 2000, :retry-attempts 0, :error nil}
   {:prompt "P2", :score 80, :cost 0.0, :model "ollama/qwen3:8b",
    :tokens-in 500, :tokens-out 500, :retry-attempts 1, :error nil}
   {:prompt "P3", :score nil, :cost 0.0, :model "openai/gpt-4o-mini",
    :tokens-in nil, :tokens-out nil, :retry-attempts nil, :error "API call timed out"}])

(deftest process-and-write-csv-report-test
  (testing "Correctly processes normalized data and writes a CSV file"
    (let [target-csv (io/file (get-temp-dir) "new-report.csv")
          processed-data (reports/process-and-write-csv-report! sample-normalized-results (.getCanonicalPath target-csv))]

      (testing "Returns correctly formatted string data"
        (is (= 3 (count processed-data)))
        (let [p1-data (first processed-data)]
          (is (= "P1" (:prompt p1-data)))
          (is (= "95" (:score p1-data)))
          (is (= "openai/gpt-4o-mini" (:model p1-data)))
          (is (= "0.00135" (:cost p1-data)))
          (is (= "1000" (:tokens-in p1-data)))))

      (testing "Writes a valid CSV file with headers"
        (is (.exists target-csv))
        (let [csv-content (slurp target-csv)]
          (is (.startsWith csv-content "prompt,score,model,cost,tokens-in,tokens-out,retry-attempts,error\n"))
          (is (.contains csv-content "\nP1,95,openai/gpt-4o-mini,0.00135,1000,2000,0,\n"))
          (is (.contains csv-content "\nP2,80,ollama/qwen3:8b,0.0,500,500,1,\n"))
          ;; CORRECTED ASSERTION: The model name should be present for P3.
          (is (.contains csv-content "\nP3,,openai/gpt-4o-mini,0.0,,,,API call timed out\n")))))))
