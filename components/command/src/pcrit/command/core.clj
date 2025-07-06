(ns pcrit.command.core
  (:require [pcrit.expdir.interface :as expdir]
            [pcrit.pop.interface :as pop]
            [pcrit.llm.interface :as llm]
            [pcrit.log.interface :as log]))

(defn init! []
  (log/setup-logging!)
  (llm/pre-flight-checks))

(defn bootstrap!
  "Executes the full bootstrap process for an experiment.
  - Creates the required directory structure.
  - Ingests all prompts specified in the bootstrap.edn manifest.
  - Creates named symbolic links for the core prompts (seed, refine, etc.)."
  [{:keys [exp-dir] :as ctx}]
  (expdir/create-experiment-dirs! exp-dir)

  (let [pdb-dir (expdir/get-pdb-dir exp-dir)
        manifest-file (expdir/bootstrap-spec-file exp-dir)
        prompt-map (pop/ingest-from-manifest pdb-dir manifest-file)
        {:keys [seed refine vary]} prompt-map]

    (expdir/link-prompt! exp-dir seed "seed")
    (expdir/link-prompt! exp-dir refine "refine")
    (expdir/link-prompt! exp-dir vary "vary")
    prompt-map))
