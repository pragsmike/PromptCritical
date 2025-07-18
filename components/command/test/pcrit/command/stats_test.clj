(ns pcrit.command.stats-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]
            [pcrit.config.interface :as config]))

(use-fixtures :each with-temp-dir)

;; A mock price table specifically for this test's models.
(def ^:private test-price-table
  {"test/model" {:in-per-1k 0.10, :out-per-1k 2.00}})

(defn- setup-stats-test-env!
  "Creates a test experiment with two generations and multiple contest reports as JSON.
  The mock JSON does NOT contain a 'cost' key, as that is calculated by pcrit."
  [ctx]
  ;; Gen 0, contest-a
  ;; Cost P1: (100/1k * 0.1) + (20/1k * 2.0) = 0.01 + 0.04 = 0.05
  ;; Cost P2: (150/1k * 0.1) + (25/1k * 2.0) = 0.015 + 0.05 = 0.065
  ;; Total: 0.115
  (let [contest-dir-0a (expdir/get-contest-dir ctx 0 "contest-a")]
    (.mkdirs contest-dir-0a)
    (spit (io/file contest-dir-0a "failter-report.json")
          (json/write-str [{:prompt_id "P1.prompt" :score 0.8 :usage {:tokens_in 100 :tokens_out 20 :model_used "test/model"}}
                           {:prompt_id "P2.prompt" :score 0.9 :usage {:tokens_in 150 :tokens_out 25 :model_used "test/model"}}])))

  ;; Gen 0, contest-b
  ;; Cost P3: (50/1k * 0.1) + (10/1k * 2.0) = 0.005 + 0.02 = 0.025
  ;; Cost P4: (60/1k * 0.1) + (12/1k * 2.0) = 0.006 + 0.024 = 0.03
  ;; Total: 0.055
  (let [contest-dir-0b (expdir/get-contest-dir ctx 0 "contest-b")]
    (.mkdirs contest-dir-0b)
    (spit (io/file contest-dir-0b "failter-report.json")
          (json/write-str [{:prompt_id "P3.prompt" :score 0.5 :usage {:tokens_in 50 :tokens_out 10 :model_used "test/model"}}
                           {:prompt_id "P4.prompt" :score 0.6 :usage {:tokens_in 60 :tokens_out 12 :model_used "test/model"}}])))

  ;; Gen 1, contest-c
  ;; Cost P5: (500/1k * 0.1) + (100/1k * 2.0) = 0.05 + 0.20 = 0.25
  (let [contest-dir-1 (expdir/get-contest-dir ctx 1 "contest-c")]
    (.mkdirs contest-dir-1)
    (spit (io/file contest-dir-1 "failter-report.json")
          (json/write-str [{:prompt_id "P5.prompt" :score 0.95 :usage {:tokens_in 500 :tokens_out 100 :model_used "test/model"}}]))))


(deftest stats-command-test
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)]
    (setup-stats-test-env! ctx)

    ;; Mock the price table for the duration of the test
    (with-redefs [config/price-table test-price-table]

      (testing "Stats for a single, specific contest"
        (let [output (with-out-str (cmd/stats! ctx {:generation 0 :from-contest "contest-a"}))]
          (is (str/includes? output "Stats for contest: contest-a"))
          (is (str/includes? output "Prompts evaluated:   2"))
          (is (str/includes? output "Total Cost:          $0.1150"))
          (is (str/includes? output "Highest Score:       0.900 (id: P2)"))
          (is (str/includes? output "Avg Tokens In:       125.0"))
          (is (str/includes? output "Avg Tokens Out:      22.5"))))

      (testing "Stats for an entire generation (aggregated)"
        (let [output (with-out-str (cmd/stats! ctx {:generation 0}))]
          (is (str/includes? output "Aggregated Stats for Generation: 0"))
          (is (str/includes? output "Prompts evaluated:   4"))
          (is (str/includes? output "Total Cost:          $0.1700")) ; 0.115 + 0.055
          (is (str/includes? output "Highest Score:       0.900 (id: P2)"))
          (is (str/includes? output "Lowest Score:        0.500 (id: P3)"))
          (is (str/includes? output "Avg Tokens In:       90.0"))))

      (testing "Stats for the latest generation by default"
        (let [output (with-out-str (cmd/stats! ctx {}))]
          (is (str/includes? output "Aggregated Stats for Generation: 1"))
          (is (str/includes? output "Prompts evaluated:   1"))
          (is (str/includes? output "Total Cost:          $0.2500"))
          (is (str/includes? output "Avg Tokens In:       500.0"))))

      (testing "Gracefully handles non-existent contest"
        (let [output (with-out-str (cmd/stats! ctx {:generation 0 :from-contest "no-such-contest"}))]
          (is (str/includes? output "No reports found"))))

      (testing "Gracefully handles non-existent generation"
        (let [output (with-out-str (cmd/stats! ctx {:generation 99}))]
          (is (str/includes? output "No reports found")))))))
