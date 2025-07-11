(ns pcrit.command.select-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.pop.interface :as pop]
            [pcrit.pdb.interface :as pdb]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(defn- setup-select-test-env!
  "Creates a test experiment with 10 prompts in gen-0 and a contest report.
  Scores are generated such that P1 has the highest score (10.0) and P10 has the lowest (1.0)."
  []
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)
        pdb-dir (expdir/get-pdb-dir ctx)]
    (expdir/create-experiment-dirs! ctx)

    ;; 1. Create 10 prompts in the PDB
    (let [prompts (for [i (range 1 11)]
                    (pop/ingest-prompt ctx (str "Prompt " i "{{INPUT_TEXT}}")))]
      ;; 2. Create gen-0 with all 10 prompts
      (pop/create-new-generation! ctx prompts)

      ;; 3. Create a contest report for gen-0
      (let [contest-dir (expdir/get-contest-dir ctx 0 "test-contest")
            report-file (io/file contest-dir "report.csv")
            csv-content (->> (for [i (range 1 11)]
                               (str "P" i "," (- 11.0 i))) ; P1=10.0, P2=9.0, ... P10=1.0
                             (cons "prompt,score")
                             (str/join "\n"))]
        (.mkdirs contest-dir)
        (spit report-file csv-content)))
    ctx))

(deftest select-command-happy-path-test
  (testing "select! with default top-N policy"
    (let [ctx (setup-select-test-env!)]
      ;; No policy specified, should use the default "top-N=5" from config
      (cmd/select! ctx {:from-contest "test-contest"})

      (testing "A new generation is created"
        (is (= 1 (expdir/find-latest-generation-number ctx))))

      (testing "The new generation has the correct number of survivors"
        (let [new-pop (pop/load-population ctx 1)]
          (is (= 5 (count new-pop)))))

      (testing "The survivors are the correct top 5 prompts"
        (let [new-pop-ids (set (map #(get-in % [:header :id]) (pop/load-population ctx 1)))]
          (is (= #{"P1" "P2" "P3" "P4" "P5"} new-pop-ids))))

      (testing "Survivor prompts have selection metadata appended"
        (let [pdb-dir (expdir/get-pdb-dir ctx)
              survivor (pdb/read-prompt pdb-dir "P1")
              selection-meta (get-in survivor [:header :selection])]
          (is (seq? selection-meta))
          (is (= 1 (count selection-meta)))
          (let [first-selection (first selection-meta)]
            (is (= "test-contest" (:contest-name first-selection)))
            (is (= "top-N=5" (:policy first-selection)))
            (is (some? (:select-run first-selection)))))))))

(deftest select-command-custom-policy-test
  (testing "select! with an explicit --policy top-N=3"
    (let [ctx (setup-select-test-env!)]
      (cmd/select! ctx {:from-contest "test-contest" :policy "top-N=3"})
      (let [new-pop (pop/load-population ctx 1)]
        (is (= 3 (count new-pop)))
        (let [new-pop-ids (set (map #(get-in % [:header :id]) new-pop))]
          (is (= #{"P1" "P2" "P3"} new-pop-ids)))))))

;; Test cases are now isolated in separate deftest blocks
(deftest select-edge-case-large-n-test
  (testing "Handles report with fewer prompts than the policy limit"
    (let [ctx (setup-select-test-env!)]
      (cmd/select! ctx {:from-contest "test-contest" :policy "top-N=20"})
      (let [new-pop (pop/load-population ctx 1)]
        (is (= 1 (expdir/find-latest-generation-number ctx)))
        (is (= 10 (count new-pop)))))))

(deftest select-edge-case-no-contest-test
  (testing "Fails gracefully if contest is not found"
    (let [ctx (setup-select-test-env!)]
      (cmd/select! ctx {:from-contest "non-existent-contest"})
      (is (= 0 (expdir/find-latest-generation-number ctx)) "No new generation should be created."))))

(deftest select-edge-case-run-twice-test
  (testing "Running select twice appends metadata correctly"
    (let [ctx (setup-select-test-env!)]
      ;; First select
      (cmd/select! ctx {:from-contest "test-contest" :policy "top-N=2"})
      (is (= 1 (expdir/find-latest-generation-number ctx)) "gen-1 should be created.")

      ;; Set up a new contest in gen-1
      (let [contest-dir (expdir/get-contest-dir ctx 1 "next-contest")
            report-file (io/file contest-dir "report.csv")]
        (.mkdirs contest-dir)
        (spit report-file "prompt,score\nP1,100\nP2,90"))

      ;; Second select
      (cmd/select! ctx {:generation 1 :from-contest "next-contest" :policy "top-N=1"})
      (is (= 2 (expdir/find-latest-generation-number ctx)) "gen-2 should be created.")

      ;; Check the doubly-selected prompt
      (let [survivor (pdb/read-prompt (expdir/get-pdb-dir ctx) "P1")
            selection-meta (vec (get-in survivor [:header :selection]))]
        (is (= 2 (count selection-meta)) "Should have two selection events.")
        (is (= "test-contest" (get-in selection-meta [0 :contest-name])))
        (is (= "next-contest" (get-in selection-meta [1 :contest-name])))))))

(deftest select-invalid-policy-test
  (testing "Using an unparseable policy string results in no selection"
    (let [ctx (setup-select-test-env!)]
      (cmd/select! ctx {:from-contest "test-contest" :policy "not-a-valid-policy"})
      (is (= 0 (expdir/find-latest-generation-number ctx)) "No new generation should be created.")

      ;; Verify that no prompts were updated with metadata
      (let [p1-after (pdb/read-prompt (expdir/get-pdb-dir ctx) "P1")]
        (is (nil? (get-in p1-after [:header :selection])) "Prompt metadata should not be updated on failure.")))))
