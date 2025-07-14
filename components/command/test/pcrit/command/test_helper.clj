(ns pcrit.command.test-helper
  (:require [clojure.java.io :as io]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]))

(defn setup-bootstrappable-exp!
  "Creates the minimal file structure required to run `bootstrap!`.
  - Creates the 'seeds/' directory.
  - Creates all standard seed prompts and a manifest that points to them."
  [exp-dir]
  (let [ctx (exp/new-experiment-context exp-dir)
        seeds-dir (expdir/get-seeds-dir ctx)]
    ;; Do NOT create all experiment dirs. Only create the preconditions
    ;; for the bootstrap command itself.
    (.mkdirs seeds-dir)
    (spit (io/file seeds-dir "seed-object-prompt.txt") "The seed! {{INPUT_TEXT}}")
    (spit (io/file seeds-dir "improve-meta-prompt.txt") "Refine this: {{OBJECT_PROMPT}}")
    (spit (io/file seeds-dir "crossover-meta-prompt.txt") "Combine the best... PROMPT A: {{OBJECT_PROMPT_A}} PROMPT B: {{OBJECT_PROMPT_B}}")
    (spit (expdir/bootstrap-spec-file ctx)
          (pr-str {:seed "seeds/seed-object-prompt.txt"
                   :refine "seeds/improve-meta-prompt.txt"
                   :crossover "seeds/crossover-meta-prompt.txt"}))
    ctx))

(defn setup-bootstrapped-exp!
  "Creates a fully bootstrapped experiment directory, including gen-0.
  NOTE: This is a test helper that performs the bootstrap action.
  Returns the context map."
  [exp-dir]
  (let [ctx (setup-bootstrappable-exp! exp-dir)]
    ;; The bootstrap command itself is responsible for creating
    ;; the pdb, links, and generations directories.
    (cmd/bootstrap! ctx)
    ctx))

(defn setup-configured-exp!
  "Creates a bootstrapped experiment and adds a default
  `evolution-parameters.edn` file."
  [exp-dir]
  (let [ctx (setup-bootstrapped-exp! exp-dir)
        config-file (io/file exp-dir "evolution-parameters.edn")]
    (spit config-file
          (pr-str {:evaluate {:models ["mock-model"]
                              :judge-model "mock-judge"}
                   :vary {:model "mock-model"}}))
    ctx))
