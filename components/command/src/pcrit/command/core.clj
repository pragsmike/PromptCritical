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
  [ctx]
  (expdir/create-experiment-dirs! ctx)

  (let [manifest-file (expdir/bootstrap-spec-file ctx)
        prompt-map (pop/ingest-from-manifest ctx manifest-file)
        {:keys [seed refine vary]} prompt-map]

    (expdir/link-prompt! ctx seed "seed")
    (expdir/link-prompt! ctx refine "refine")
    (expdir/link-prompt! ctx vary "vary")
    prompt-map))
