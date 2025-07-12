(ns pcrit.command.stats-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(defn- setup-stats-test-env!
  "Creates a test experiment with two generations and multiple contest reports."
  [ctx]
  ;; Gen 0 has two contests
  (let [contest-dir-0a (expdir/get-contest-dir ctx 0 "contest-a")]
    (.mkdirs contest-dir-0a)
    (spit (io/file contest-dir-0a "report.csv")
          "prompt,score,cost\nP1,0.8,0.1\nP2,0.9,0.2")) ; Total cost: 0.3

  (let [contest-dir-0b (expdir/get-contest-dir ctx 0 "contest-b")]
    (.mkdirs contest-dir-0b)
    (spit (io/file contest-dir-0b "report.csv")
          "prompt,score,cost\nP3,0.5,0.3\nP4,0.6,0.4")) ; Total cost: 0.7

  ;; Gen 1 has one contest
  (let [contest-dir-1 (expdir/get-contest-dir ctx 1 "contest-c")]
    (.mkdirs contest-dir-1)
    (spit (io/file contest-dir-1 "report.csv")
          "prompt,score,cost\nP5,0.95,1.0"))) ; Total cost: 1.0

(deftest stats-command-test
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)]
    (setup-stats-test-env! ctx)

    (testing "Stats for a single, specific contest"
      (let [output (with-out-str (cmd/stats! ctx {:generation 0 :from-contest "contest-a"}))]
        (is (str/includes? output "Stats for contest: contest-a"))
        (is (str/includes? output "Prompts evaluated:   2"))
        (is (str/includes? output "Total Cost:          $0.3000")) ; 0.1 + 0.2
        (is (str/includes? output "Highest Score:       0.900 (id: P2)"))))

    (testing "Stats for an entire generation (aggregated)"
      (let [output (with-out-str (cmd/stats! ctx {:generation 0}))]
        (is (str/includes? output "Aggregated Stats for Generation: 0"))
        (is (str/includes? output "Prompts evaluated:   4"))
        (is (str/includes? output "Total Cost:          $1.0000")) ; 0.3 + 0.7
        (is (str/includes? output "Highest Score:       0.900 (id: P2)"))
        (is (str/includes? output "Lowest Score:        0.500 (id: P3)"))))

    (testing "Stats for the latest generation by default"
      (let [output (with-out-str (cmd/stats! ctx {}))]
        (is (str/includes? output "Aggregated Stats for Generation: 1"))
        (is (str/includes? output "Prompts evaluated:   1"))
        (is (str/includes? output "Total Cost:          $1.0000"))))

    (testing "Gracefully handles non-existent contest"
      (let [output (with-out-str (cmd/stats! ctx {:generation 0 :from-contest "no-such-contest"}))]
        (is (str/includes? output "No reports found"))))

    (testing "Gracefully handles non-existent generation"
      (let [output (with-out-str (cmd/stats! ctx {:generation 99}))]
        (is (str/includes? output "No reports found"))))))
