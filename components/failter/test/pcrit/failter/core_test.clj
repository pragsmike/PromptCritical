(ns pcrit.failter.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell]
            [pcrit.failter.core :as failter]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

;; CORRECTED: This mock is now simpler and correctly simulates failter's behavior.
(defn- mock-shell-fn [executable & args]
  (let [command   (first args)
        spec-path (second args)] ; The path to the experiment is the second arg.

    (when (= command "report")
      ;; Failter creates report.csv inside the path it was given.
      (spit (io/file spec-path "report.csv") "prompt,score\nP1,0.95"))

    {:exit 0 :out (str "Mocked " command) :err ""}))


(deftest run-contest-test
  (testing "run-contest! successfully prepares, runs, and captures a contest"
    (let [exp-dir              (get-temp-dir)
          ctx                  (exp/new-experiment-context exp-dir)
          inputs-dir           (io/file exp-dir "test-inputs")]

      (expdir/create-experiment-dirs! ctx)
      (.mkdirs inputs-dir)
      (spit (io/file inputs-dir "doc1.txt") "input document one")
      (spit (io/file (expdir/get-pdb-dir ctx) "P1.prompt") "dummy prompt")

      (let [mock-population [{:header {:id "P1"}}]
            contest-params {:generation-number 0
                            :contest-name      "test-contest"
                            :inputs-dir        (.getCanonicalPath inputs-dir)
                            :population        mock-population
                            :models            ["model-a" "model-b"]
                            :judge-model       "test-judge"}]
        (with-redefs [clojure.java.shell/sh mock-shell-fn]
          (let [result (failter/run-contest! ctx contest-params)]
            (is (:success result))

            (let [contest-dir      (expdir/get-contest-dir ctx 0 "test-contest")
                  spec-dir         (expdir/get-failter-spec-dir ctx 0 "test-contest")
                  final-report     (io/file contest-dir "report.csv")]

              (is (.exists (io/file spec-dir "model-names.txt")))
              (is (.exists final-report) "report.csv should be moved to the contest directory.")
              (is (not (.exists (io/file spec-dir "report.csv"))) "report.csv should no longer be in the spec dir."))))))))
