(ns pcrit.command.vary
  (:require [clojure.java.io :as io]
            [pcrit.config.interface :as config]
            [pcrit.expdir.interface :as expdir]
            [pcrit.llm.interface :as llm]
            [pcrit.log.interface :as log]
            [pcrit.pop.interface :as pop])
  (:import [java.time Instant]))

(defn- breed-prompt
  "The generic 'breeding' function. Applies a chosen meta-prompt to a parent
  prompt to produce a new proto-prompt map with full metadata."
  [model-name meta-prompt parent-prompt run-id call-template-fn]
  (let [template (:body meta-prompt)
        vars {:OBJECT_PROMPT (:body parent-prompt)}]
    (log/info "Applying meta-prompt" (get-in meta-prompt [:header :id]) "to" (get-in parent-prompt [:header :id]))
    (let [{:keys [content generation-metadata error]} (call-template-fn model-name template vars)]
      (when-not error
        (let [ancestry-metadata {:parents [(get-in parent-prompt [:header :id])]
                                 :generator {:model model-name
                                             :meta-prompt (get-in meta-prompt [:header :id])
                                             :vary-run run-id}}
              final-metadata (merge ancestry-metadata generation-metadata)]
          {:header final-metadata :body content})))))

(defn gen-offspring
  "The 'strategy' function for generating offspring. For now, it implements
  a single, hardcoded strategy: apply the 'improve' meta-prompt. This function
  is the intended extension point for future multimethod-based strategies."
  [ctx evo-params run-id parent-prompt call-template-fn]
  (let [model-name (get-in evo-params [:vary :model] "mistral")
        _ (log/info (str "Using model '" model-name "' for variation."))
        ;; This is the strategy-specific part: choosing the meta-prompt.
        improve-prompt (pop/read-linked-prompt ctx "refine")]
    (when improve-prompt
      (breed-prompt model-name improve-prompt parent-prompt run-id call-template-fn))))

(defn vary!
  "Adds new offspring to the latest generation by applying meta-prompts.
  This command mutates the current generation's population in-place and does
  NOT create a new generation folder."
  [ctx & [{:keys [call-template-fn] :or {call-template-fn llm/call-model-template}}]]
  (if-let [latest-gen-num (expdir/find-latest-generation-number ctx)]
    (let [evo-params (config/load-evolution-params ctx)
          current-pop (pop/load-population ctx latest-gen-num)
          current-pop-dir (expdir/get-population-dir ctx latest-gen-num)
          run-id (.toString (Instant/now))]

      (log/info "Varying generation" latest-gen-num "which has" (count current-pop) "members.")

      ;; Generate Offspring proto-prompts
      (let [offspring0 (->> current-pop
                            (map (fn [parent-prompt]
                                   (gen-offspring ctx evo-params run-id parent-prompt call-template-fn)))
                            (remove nil?))
            ;; Ingest the proto-prompts into the PDB
            offspring (->> offspring0
                           (map (fn [child] (pop/ingest-prompt ctx (:body child) :metadata (:header child))))
                           (doall))]

        (log/info "Created" (count offspring) "new offspring prompts.")

        ;; Add offspring to the *current* generation's population directory
        (doseq [child-prompt offspring]
          (let [target-file (expdir/pdb-file-of-prompt-record ctx child-prompt)
                link-name   (str (get-in child-prompt [:header :id]) ".prompt")
                link-file   (io/file current-pop-dir link-name)]
            (expdir/create-relative-symlink! link-file target-file)))

        (log/info "Added" (count offspring) "offspring to population in generation" latest-gen-num)

        {:generation-varied latest-gen-num
         :offspring-created (count offspring)
         :new-population-size (+ (count current-pop) (count offspring))}))

    (log/error "Cannot run vary. No generations found. Did you bootstrap?")))
