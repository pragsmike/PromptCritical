(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [pcrit.command.interface :as cmd]
            [pcrit.expdir.interface :as expdir]
            [pcrit.llm.templater :as llm-templater]
            [pcrit.pop.interface :as pop]
            [pcrit.pdb.interface :as pdb]
            [pcrit.command.test-helper :as th-cmd]
            [pcrit.test-helper.interface :as th-generic]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

(def ^:private mock-llm-response
  {:content "New offspring content"
   :generation-metadata {:provider          :mock-provider
                         :model             "mock-provider/mock-model"
                         :token-in          120
                         :token-out         30
                         :cost-usd-snapshot 0.0025
                         :duration-ms       750}})

(def ^:private mock-sender-fn
  (fn [_model-name _prompt-body] mock-llm-response))

(def ^:private mock-template-caller
  (fn [model-name template vars] (llm-templater/call-model-template model-name template vars mock-sender-fn)))

(deftest vary-command-default-strategy-test
  (testing "vary! with no config uses the default :refine strategy successfully"
    (let [ctx (th-cmd/setup-bootstrapped-exp! (th-generic/get-temp-dir))]
      (is (= 0 (expdir/find-latest-generation-number ctx)) "Precondition: gen-0 should exist.")
      (is (= 1 (count (pop/load-population ctx 0))) "Precondition: gen-0 should have one member.")

      (let [{:keys [offspring-created]} (cmd/vary! ctx {:call-template-fn mock-template-caller})]
        (is (= 1 offspring-created))

        (let [varied-pop (pop/load-population ctx 0)
              pop-headers (map :header varied-pop)
              offspring-header (first (filter :parents pop-headers))]

          (is (some? offspring-header) "An offspring with parent metadata should exist.")
          (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")
          (is (= "openai/gpt-4o-mini" (get-in offspring-header [:generator :model]))))))))

(deftest vary-command-configured-model-test
  (testing "vary! uses the model specified in evolution-parameters.edn"
    (let [exp-dir (th-generic/get-temp-dir)
          ctx (th-cmd/setup-bootstrapped-exp! exp-dir)
          config-file (io/file exp-dir "evolution-parameters.edn")]
      (spit config-file (pr-str {:vary {:model "my-configured-model"}}))

      (cmd/vary! ctx {:call-template-fn mock-template-caller})

      (let [offspring-header (->> (pop/load-population ctx 0)
                                  (map :header)
                                  (filter :parents)
                                  first)]
        (is (= "my-configured-model" (get-in offspring-header [:generator :model])))))))


(deftest vary-command-with-unknown-strategy-test
  (testing "vary! with an unknown strategy in config logs a warning and creates no offspring"
    (let [exp-dir (th-generic/get-temp-dir)
          ctx (th-cmd/setup-bootstrapped-exp! exp-dir)
          config-file (io/file exp-dir "evolution-parameters.edn")]
      (spit config-file (pr-str {:vary {:strategy :hallucinate}}))

      (let [{:keys [cost offspring-created]} (cmd/vary! ctx {:call-template-fn mock-template-caller})]
        (is (zero? offspring-created) "No offspring should be created with an unknown strategy.")
        (is (zero? cost) "Cost should be zero when no offspring are created.")))))


;; --- REFACTORED TEST FOR CROSSOVER STRATEGY ---

(deftest crossover-strategy-test
  (testing ":crossover strategy creates one child from the top two parents of the previous generation"
    (let [exp-dir (th-generic/get-temp-dir)
          ctx (th-cmd/setup-bootstrapped-exp! exp-dir)
          pdb-dir (expdir/get-pdb-dir ctx)]

      (is (.exists (io/file (expdir/get-link-dir ctx) "crossover")) "Precondition: Crossover link must exist.")

      ;; Setup:
      ;; 1. Ingest another prompt to be a parent. P1, P2, P3 already exist.
      (let [p4 (pdb/create-prompt pdb-dir "Parent B")]
        ;; 2. Create a fake contest report for gen-0, making P4 and P1 the winners.
        (let [contest-dir (expdir/get-contest-dir ctx 0 "crossover-setup-contest")
              report-file (io/file contest-dir "failter-report.json")]
          (.mkdirs contest-dir)
          (spit report-file
                (json/write-str [{:prompt_id "P4.prompt" :score 100}
                                 {:prompt_id "P1.prompt" :score 90}
                                 {:prompt_id "P2.prompt" :score 80}])))

        ;; 3. Create gen-1 with these two winners. `vary` will be run on gen-1.
        (pop/create-new-generation! ctx [(pdb/read-prompt pdb-dir "P1") p4])
        (is (= 1 (expdir/find-latest-generation-number ctx)) "Precondition: gen-1 should exist.")

        ;; 4. Configure the experiment to use the :crossover strategy for gen-1.
        (let [config-file (io/file exp-dir "evolution-parameters.edn")]
          (spit config-file (pr-str {:vary {:strategy :crossover}})))

        ;; Action: Run the vary command on gen-1.
        (let [{:keys [offspring-created new-population-size]} (cmd/vary! ctx {:call-template-fn mock-template-caller})]
          ;; Assertions:
          (is (= 1 offspring-created) "Crossover should create exactly one offspring.")
          (is (= 3 new-population-size) "New population should have the 2 parents + 1 child.")

          (let [gen-1-pop (pop/load-population ctx 1)
                offspring (->> gen-1-pop
                               (filter #(seq (get-in % [:header :parents])))
                               first)]
            (is (some? offspring) "An offspring should exist in the population.")
            ;; The :parents list should contain the IDs of the top 2 from the report.
            (is (= #{"P4" "P1"} (set (get-in offspring [:header :parents]))) "Offspring should list both parents.")))))))
