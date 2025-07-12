(ns pcrit.command.vary
  (:require [clojure.java.io :as io]
            [pcrit.config.interface :as config]
            [pcrit.expdir.interface :as expdir]
            [pcrit.llm.interface :as llm]
            [pcrit.log.interface :as log]
            [pcrit.pop.interface :as pop])
  (:import [java.time Instant]))

(defn- apply-meta-prompt
  "Applies a meta-prompt to an object-prompt using the templating LLM call."
  [model-name meta-prompt object-prompt call-template-fn]
  (let [template (:body meta-prompt)
        vars {:OBJECT_PROMPT (:body object-prompt)}]
    (log/info "Applying meta-prompt" (get-in meta-prompt [:header :id]) "to" (get-in object-prompt [:header :id]))
    (call-template-fn model-name template vars)))

(defn gen-offspring
  "Given a parent prompt, returns offspring as {:header {...} :body {...}}"
  [ctx evo-params run-id parent-prompt call-template-fn]
  (let [model-name (get-in evo-params [:vary :model] "mistral")
        _ (log/info (str "Using model '" model-name "' for variation."))
        refine-prompt (pop/read-linked-prompt ctx "refine")
        response (apply-meta-prompt model-name refine-prompt parent-prompt call-template-fn)]
    (when-not (:error response)
      (let [ancestry-metadata {:parents [(get-in parent-prompt [:header :id])]
                               :generator {:model model-name
                                           :meta-prompt (get-in refine-prompt [:header :id])
                                           :vary-run run-id}}
            new-content (:content response)]
        {:header ancestry-metadata :body new-content}))))

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

      ;; Generate Offspring
      (let [offspring0 (->> current-pop
                            (map (fn [parent-prompt]
                                   (gen-offspring ctx evo-params run-id parent-prompt call-template-fn)
                                   ))
                            (remove nil?))
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
