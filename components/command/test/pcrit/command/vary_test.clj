(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.command.core :as cmd]
            [pcrit.command.vary :as vary]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.interface :as pop]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir make-temp-exp-dir!]]))

(use-fixtures :each with-temp-dir)

(defn- count-prompts-in-pdb [pdb-dir]
  (->> (.listFiles (io/file pdb-dir))
       (filter #(.endsWith (.getName %) ".prompt"))
       count))

(deftest vary-command-test
  (testing "vary! command creates a new generation with offspring containing ancestry"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          _ (make-temp-exp-dir! exp-dir)
          _ (cmd/bootstrap! ctx)
          pdb-dir (expdir/get-pdb-dir ctx)]

      ;; Create generation 0 with just the seed prompt.
      (let [seed-prompt (pop/read-linked-prompt ctx "seed")]
        (is (= "P1" (get-in seed-prompt [:header :id])))
        (pop/create-new-generation! ctx [seed-prompt]))

      (is (= 0 (expdir/find-latest-generation-number ctx)))
      (is (= 3 (count-prompts-in-pdb pdb-dir)) "PDB should have seed, refine, and vary prompts.")

      ;; Mock the LLM call to return a predictable new prompt.
      (let [mock-sender-fn (fn [_model-name _final-prompt-string]
                             {:content "This is the refined seed prompt. {{INPUT_TEXT}}"})
            mock-template-caller (fn [model-name template vars]
                                   (llm/call-model-template model-name template vars mock-sender-fn))]

        (vary/vary! ctx {:call-template-fn mock-template-caller})

        ;; Verify the new state of the experiment.
        (is (= 1 (expdir/find-latest-generation-number ctx)) "A new generation should be created.")
        (is (= 4 (count-prompts-in-pdb pdb-dir)) "A new prompt should be added to the PDB.")

        (let [new-pop (pop/load-population ctx 1)
              original-member (first (filter #(= "P1" (get-in % [:header :id])) new-pop))
              offspring-member (first (filter #(= "P4" (get-in % [:header :id])) new-pop))]

          (is (= 2 (count new-pop)) "New population should have the parent and one offspring.")
          (is (some? original-member))
          (is (some? offspring-member) "The new offspring (P4) should be in the new population.")

          ;; UPDATED: Check for the new ancestry metadata in the offspring.
          (let [offspring-header (:header offspring-member)]
            (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")
            (is (map? (:generator offspring-header)) "Generator metadata should be a map.")
            (is (= "P2" (get-in offspring-header [:generator :meta-prompt])) "Generator should list the refine prompt (P2) as the meta-prompt.")
            (is (= "mistral" (get-in offspring-header [:generator :model])))))))))
