(ns pcrit.reports.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.reports.core :as reports]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(deftest parse-report-test
  (testing "Successfully parsing a valid report.csv file"
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

  (testing "Parsing a report with only a header row"
    (let [csv-content "prompt,score,cost\n"
          report-file (io/file (get-temp-dir) "report.csv")]
      (spit report-file csv-content)
      (let [parsed-data (reports/parse-report report-file)]
        (is (empty? parsed-data)))))

  (testing "Parsing an empty report file"
    (let [report-file (io/file (get-temp-dir) "report.csv")]
      (spit report-file "")
      (let [parsed-data (reports/parse-report report-file)]
        (is (empty? parsed-data)))))

  (testing "Parsing a file that does not exist"
    (let [report-file (io/file (get-temp-dir) "nonexistent.csv")]
      (is (empty? (reports/parse-report report-file)))))

  (testing "Parsing a file with non-numeric scores"
    (let [csv-content (str "prompt,score\n"
                           "P1,90\n"
                           "P2,not-a-number\n"
                           "P3,85.5\n")
          report-file (io/file (get-temp-dir) "report.csv")]
      (spit report-file csv-content)
      (let [parsed-data (reports/parse-report report-file)]
        ;; The malformed row should be skipped
        (is (= 2 (count parsed-data)))
        (is (= #{"P1" "P3"} (set (map :prompt parsed-data))))
        (is (= [90.0 85.5] (map :score parsed-data))))))

  (testing "Parsing a file with mixed valid and invalid rows"
    (let [csv-content (str "prompt,score,cost\n"
                           "P1,10.0,0.1\n"
                           ",,\n"          ; Invalid blank row
                           "P2,20.0,0.2\n")
          report-file (io/file (get-temp-dir) "report.csv")]
      (spit report-file csv-content)
      (let [parsed-data (reports/parse-report report-file)]
        (is (= 2 (count parsed-data)))))))
