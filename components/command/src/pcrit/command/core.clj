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
  - Creates named symbolic links for all ingested prompts.
  - Creates generation 0 with all ingested object-prompts."
  [ctx]
  (expdir/create-experiment-dirs! ctx)

  (let [manifest-file (expdir/bootstrap-spec-file ctx)
        prompt-map (pop/ingest-from-manifest ctx manifest-file)]

    (doseq [[link-name record] prompt-map]
      (expdir/link-prompt! ctx record (name link-name)))

    (let [object-prompts (->> (vals prompt-map)
                              (filter #(= :object-prompt (get-in % [:header :prompt-type]))))]
      (if (seq object-prompts)
        (let [gen-info (pop/create-new-generation! ctx object-prompts)]
          (log/info "Created generation 0 with " (:population-size gen-info) " seed object-prompts."))
        (do
          (log/warn "No object-prompts found in bootstrap manifest. Creating an empty generation 0.")
          (pop/create-new-generation! ctx []))))

    prompt-map))
