(ns pcrit.command.core
  (:require [clojure.java.io :as io]
            [pcrit.expdir.interface :as expdir]
            [pcrit.pop.interface :as pop]
            [pcrit.log.interface :as log]
            [pcrit.llm.core :as llm]))

(defn setup! []
  (log/setup-logging!)
  (llm/pre-flight-checks))

(defn- pre-flight-checks!
  "Ensures it's safe to run the bootstrap command."
  [ctx]
  (let [gens-dir (expdir/get-generations-dir ctx)]
    (when (.exists gens-dir)
      (throw (ex-info "This experiment appears to have been bootstrapped already."
                      {:type :validation-error
                       :reason (str "The '" (.getName gens-dir) "' directory already exists.")
                       :remedy "If you want to start over, please remove the experiment directory and run 'pcrit init' again."}))))
  true)

(defn bootstrap!
  "Executes the full bootstrap process for an experiment.
  - Creates the required directory structure.
  - Ingests all prompts specified in the bootstrap.edn manifest.
  - Creates named symbolic links for all ingested prompts.
  - Creates generation 0 with all ingested object-prompts."
  [ctx]
  (try
    (pre-flight-checks! ctx)
    (expdir/create-experiment-dirs! ctx)

    (let [manifest-file (expdir/bootstrap-spec-file ctx)
          prompt-map (pop/ingest-from-manifest ctx manifest-file)]

      (doseq [[link-name record] prompt-map]
        (expdir/link-prompt! ctx record (name link-name)))

      (let [object-prompts (->> (vals prompt-map)
                                (filter #(= :object-prompt (get-in % [:header :prompt-type]))))]
        (if (seq object-prompts)
          (let [gen-info (pop/create-new-generation! ctx object-prompts)]
            (log/info "Created generation 0 with" (:population-size gen-info) "seed object-prompts."))
          (do
            (log/warn "No object-prompts found in bootstrap manifest. Creating an empty generation 0.")
            (pop/create-new-generation! ctx []))))

      prompt-map)
    (catch clojure.lang.ExceptionInfo e
      (log/error "Bootstrap failed:" (.getMessage e))
      (when-let [remedy (:remedy (ex-data e))]
        (log/error remedy)))))
