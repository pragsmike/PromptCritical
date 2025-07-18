(ns pcrit.results.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [pcrit.results.core :as results]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]
            [pcrit.config.interface :as config]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(def ^:private test-price-table
  {"test/model-a" {:in-per-1k 0.10, :out-per-1k 0.20}
   "test/free-model" {:in-per-1k 0.0, :out-per-1k 0.0}})

(def ^:private sample-failter-report
  [{:prompt_id "P1.prompt"
    :score 95.5
    :usage {:model_used "test/model-a" :tokens_in 100 :tokens_out 50}}
   {:prompt_id "P2.prompt"
    :score 80.0
    :usage {:model_used "test/free-model" :tokens_in 1000 :tokens_out 1000}}
   {:prompt_id "P3.prompt"
    :score 99.9
    :usage {:model_used "test/model-a" :tokens_in 0 :tokens_out 10}}
   {:prompt_id "P4.prompt"
    :score nil
    :error "API Error"
    :usage {:model_used "test/model-a"}}])

(defn- setup-test-contest! [ctx report-content-str]
  (let [contest-dir (expdir/get-contest-dir ctx 0 "test-contest")]
    (.mkdirs contest-dir)
    (spit (io/file contest-dir "failter-report.json") report-content-str)))

(deftest parse-report-happy-path-test
  (testing "Successfully parses a valid report and calculates costs"
    (with-redefs [config/price-table test-price-table]
      (let [ctx (exp/new-experiment-context (get-temp-dir))]
        (setup-test-contest! ctx (json/write-str sample-failter-report))

        (let [results (results/parse-report ctx 0 "test-contest")]
          (is (= 4 (count results)) "Should parse all entries")

          (let [p1-result (first results)]
            (is (= "P1" (:prompt p1-result)))
            (is (= 95.5 (:score p1-result)))
            (is (= "test/model-a" (:model p1-result)))
            (is (= 100 (:tokens-in p1-result)))
            (is (= 50 (:tokens-out p1-result)))
            ;; Cost = (100/1000 * 0.10) + (50/1000 * 0.20) = 0.01 + 0.01 = 0.02
            (is (< (Math/abs (- 0.02 (:cost p1-result))) 1e-9)))

          (let [p2-result (second results)]
            (is (= "P2" (:prompt p2-result)))
            (is (= 0.0 (:cost p2-result))))

          (let [p3-result (nth results 2)]
            (is (= "P3" (:prompt p3-result)))
            ;; Cost = (0/1000 * 0.10) + (10/1000 * 0.20) = 0.002
            (is (< (Math/abs (- 0.002 (:cost p3-result))) 1e-9)))

          (let [p4-result (nth results 3)]
            (is (= "P4" (:prompt p4-result)))
            (is (nil? (:score p4-result)))
            (is (= "API Error" (:error p4-result)))
            (is (= 0.0 (:cost p4-result)))))))))

(deftest parse-report-edge-cases-test
  (testing "Returns empty list if report file does not exist"
    (let [ctx (exp/new-experiment-context (get-temp-dir))]
      (is (empty? (results/parse-report ctx 0 "non-existent-contest")))))

  (testing "Returns empty list and logs an error if JSON is corrupt"
    (let [ctx (exp/new-experiment-context (get-temp-dir))]
      (setup-test-contest! ctx "[{:prompt_id: \"P1.prompt\"") ; Corrupt JSON
      (is (empty? (results/parse-report ctx 0 "test-contest"))))))
