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
    (let [{:keys [content generation-metadata error]} (call-template-fn model-name template vars)]
      (when-not error
        (let [ancestry-metadata {:parents [(get-in parent-prompt [:header :id])]
                                 :generator {:model model-name
                                             :meta-prompt (get-in meta-prompt [:header :id])
                                             :vary-run run-id}}
              final-metadata (merge ancestry-metadata generation-metadata)]
          {:header final-metadata :body content})))))

;; --- Pluggable Offspring Generation Strategy ---

(defn- dispatch-strategy [evo-params]
  (get-in evo-params [:vary :strategy] :refine))

(defmulti gen-offspring
  "Generates offspring using a pluggable strategy. Dispatches on the value of
  [:vary :strategy] in evolution-parameters.edn, defaulting to :refine."
  (fn [_ctx evo-params _run-id _parent-prompt _call-template-fn]
    (dispatch-strategy evo-params)))

(defmethod gen-offspring :refine
  [ctx evo-params run-id parent-prompt call-template-fn]
  (let [model-name (get-in evo-params [:vary :model] "openai/gpt-4o-mini")
        improve-prompt (pop/read-linked-prompt ctx "refine")]
    (if improve-prompt
      (do
        (log/info "Using :refine strategy. Applying meta-prompt" (get-in improve-prompt [:header :id]) "to" (get-in parent-prompt [:header :id]))
        (breed-prompt model-name improve-prompt parent-prompt run-id call-template-fn))
      (do
        (log/warn "Vary strategy is ':refine' but the linked prompt 'refine' was not found. Skipping parent:" (get-in parent-prompt [:header :id]))
        nil))))

(defmethod gen-offspring :default
  [_ctx evo-params _run-id parent-prompt _call-template-fn]
  (log/warn "Unknown vary strategy" (dispatch-strategy evo-params) "requested for parent" (get-in parent-prompt [:header :id]) ". Skipping.")
  nil)


;; --- Main Command Function ---

(defn vary!
  "Adds new offspring to the latest generation by applying meta-prompts.
  Returns a map containing statistics about the run, including the total :cost."
  [ctx & [{:keys [call-template-fn] :or {call-template-fn llm/call-model-template}}]]
  (if-let [latest-gen-num (expdir/find-latest-generation-number ctx)]
    (let [evo-params (config/load-evolution-params ctx)
          current-pop (pop/load-population ctx latest-gen-num)
          current-pop-dir (expdir/get-population-dir ctx latest-gen-num)
          run-id (.toString (Instant/now))]

      (log/info "Varying generation" latest-gen-num "which has" (count current-pop) "members.")

      (let [offspring-proto-prompts (->> current-pop
                                         (mapv (fn [parent] (gen-offspring ctx evo-params run-id parent call-template-fn)))
                                         (remove nil?))
            vary-cost (->> offspring-proto-prompts
                           (map #(get-in % [:header :cost-usd-snapshot] 0.0))
                           (reduce + 0.0))
            offspring (->> offspring-proto-prompts
                           (map (fn [child] (pop/ingest-prompt ctx (:body child) :metadata (:header child))))
                           (doall))]

        (log/info "Created" (count offspring) "new offspring prompts.")

        (doseq [child-prompt offspring]
          (let [target-file (expdir/pdb-file-of-prompt-record ctx child-prompt)
                link-name   (str (get-in child-prompt [:header :id]) ".prompt")
                link-file   (io/file current-pop-dir link-name)]
            (expdir/create-relative-symlink! link-file target-file)))

        (log/info "Added" (count offspring) "offspring to population in generation" latest-gen-num)

        {:generation-varied latest-gen-num
         :offspring-created (count offspring)
         :new-population-size (+ (count current-pop) (count offspring))
         :cost vary-cost}))

    (do (log/error "Cannot run vary. No generations found. Did you bootstrap?")
        {:cost 0.0})))
