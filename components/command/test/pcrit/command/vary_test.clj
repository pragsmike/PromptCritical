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
  (testing "vary! command creates a new generation with offspring"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          _ (make-temp-exp-dir! exp-dir)
          _ (cmd/bootstrap! ctx)
          pdb-dir (expdir/get-pdb-dir ctx)]

      (let [seed-prompt (pop/read-linked-prompt ctx "seed")]
        (pop/create-new-generation! ctx [seed-prompt]))

      (is (= 0 (expdir/find-latest-generation-number ctx)))
      (is (= 3 (count-prompts-in-pdb pdb-dir)))

      (let [;; This mock only checks the final rendered string
            mock-sender-fn (fn [model-name final-prompt-string]
                             (is (= "mistral" model-name))
                             (is (str/starts-with? final-prompt-string "Refine this prompt:"))
                             (is (str/includes? final-prompt-string "This is the initial object prompt. {{INPUT_TEXT}}"))
                             (is (not (str/includes? final-prompt-string "{{OBJECT_PROMPT}}")))
                             {:content "This is the refined seed prompt. {{INPUT_TEXT}}"})

            ;; This mock function calls the real templater but injects our mock sender
            mock-template-caller (fn [model-name template vars]
                                   (llm/call-model-template model-name template vars mock-sender-fn))]

        (vary/vary! ctx {:call-template-fn mock-template-caller})

        (is (= 1 (expdir/find-latest-generation-number ctx)))
        (let [new-pop (pop/load-population ctx 1)]
          (is (= 2 (count new-pop))))))))
