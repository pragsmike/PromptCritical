(ns pcrit.command.select-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [pcrit.command.interface :as cmd]
            [pcrit.command.select :as select-impl]
            [pcrit.expdir.interface :as expdir]
            [pcrit.pop.interface :as pop]
            [pcrit.pdb.interface :as pdb]
            [pcrit.test-helper.interface :as th-generic]
            [pcrit.command.test-helper :as th-cmd]
            [taoensso.telemere :as tel]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

(defn- setup-select-test-env!
  "Creates a test experiment with 10 prompts in the PDB, gen-0, and a contest report.
  Scores are generated such that P1 has the highest score (10.0) and P10 has the lowest (1.0)."
  []
  (let [ctx (th-cmd/setup-bootstrapped-exp! (th-generic/get-temp-dir))
        pdb-dir (expdir/get-pdb-dir ctx)]
    (doseq [i (range 3 12)]
      (pdb/create-prompt pdb-dir (str "Prompt " i)))
    (let [contest-dir (expdir/get-contest-dir ctx 0 "test-contest")
          report-file (io/file contest-dir "failter-report.json")
          json-content (->> (for [i (range 1 11)]
                              {:prompt_id (str "P" i ".prompt")
                               :score     (double (- 11.0 i))
                               :usage     {:model_used "test/model"}})
                            (json/write-str))]
      (.mkdirs contest-dir)
      (spit report-file json-content))
    ctx))

(deftest select-command-happy-path-test
  (testing "select! with default top-N policy"
    (let [ctx (setup-select-test-env!)]
      (cmd/select! ctx {:from-contest "test-contest"})
      (testing "A new generation is created"
        (is (= 1 (expdir/find-latest-generation-number ctx))))
      (testing "The new generation has the correct number of survivors"
        (is (= 5 (count (pop/load-population ctx 1)))))
      (testing "The survivors are the correct top 5 prompts"
        (is (= #{"P1" "P2" "P3" "P4" "P5"} (set (map #(get-in % [:header :id]) (pop/load-population ctx 1))))))
      (testing "Survivor prompts have selection metadata appended"
        (let [survivor (pdb/read-prompt (expdir/get-pdb-dir ctx) "P1")
              selection-meta (get-in survivor [:header :selection])]
          (is (seq? selection-meta))
          (is (= 1 (count selection-meta))))))))

(deftest select-command-custom-policy-test
  (testing "select! with an explicit --policy top-N=3"
    (let [ctx (setup-select-test-env!)]
      (cmd/select! ctx {:from-contest "test-contest" :policy "top-N=3"})
      (is (= 3 (count (pop/load-population ctx 1)))))))

(deftest select-edge-case-large-n-test
  (testing "Handles report with fewer prompts than the policy limit"
    (let [ctx (setup-select-test-env!)]
      (cmd/select! ctx {:from-contest "test-contest" :policy "top-N=20"})
      (is (= 10 (count (pop/load-population ctx 1)))))))

(deftest tournament-selection-policy-test
  (testing "Tournament selection logic works deterministically with mocked randomness"
    (let [report-data [{:prompt "P1", :score 10.0} {:prompt "P5", :score 6.0} {:prompt "P10", :score 1.0}]
          policy {:type :tournament, :k 2}
          deterministic-draws [(get report-data 2) (get report-data 1)
                               (get report-data 0) (get report-data 2)
                               (get report-data 1) (get report-data 0)]
          call-count (atom 0)
          mock-rand-nth (fn [_coll] (let [res (nth deterministic-draws @call-count)] (swap! call-count inc) res))]
      (with-redefs [rand-nth mock-rand-nth]
        (let [survivors (select-impl/apply-selection-policy report-data policy)]
          (is (= 3 (count survivors)))
          (is (= {"P1" 2, "P5" 1} (frequencies (map :prompt survivors)))))))))

(deftest select-zero-survivors-test
  (testing "Warns the user when selection results in an empty next generation"
    (let [ctx (setup-select-test-env!)
          captured (atom [])]
      (tel/with-handler :capture
        (fn [signal] (swap! captured conj signal))
        {}
        (cmd/select! ctx {:from-contest "test-contest" :policy "top-N=0"}))
      (let [warn-log (first (filter #(= :warn (:level %)) @captured))]
        (is (some? warn-log))
        (is (.startsWith (:msg_ warn-log) "Selection resulted in zero survivors")))
      (is (= 0 (expdir/find-latest-generation-number ctx)) "No new generation should be created"))))
