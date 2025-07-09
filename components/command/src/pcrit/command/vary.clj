(ns pcrit.command.vary
  (:require [pcrit.expdir.interface :as expdir]
            [pcrit.llm.interface :as llm]
            [pcrit.log.interface :as log]
            [pcrit.pdb.interface :as pdb]
            [pcrit.pop.interface :as pop]))

(defn- apply-meta-prompt
  "Applies a meta-prompt to an object-prompt using the templating LLM call."
  [ctx meta-prompt object-prompt call-template-fn]
  (let [template (:body meta-prompt)
        vars {:OBJECT_PROMPT (:body object-prompt)}]
    (log/info "Applying meta-prompt" (get-in meta-prompt [:header :id]) "to" (get-in object-prompt [:header :id]))
    (call-template-fn "mistral" template vars)))

(defn vary!
  "Creates a new generation of prompts by applying meta-prompts to the current population."
  [ctx & [{:keys [call-template-fn] :or {call-template-fn llm/call-model-template}}]]
  (let [latest-gen-num (expdir/find-latest-generation-number ctx)]
    (if-not latest-gen-num
      (log/error "Cannot run vary. No generations found. Did you bootstrap and create an initial population?")
      (let [current-pop (pop/load-population ctx latest-gen-num)
            refine-prompt (pop/read-linked-prompt ctx "refine")]
        (log/info "Varying generation" latest-gen-num "which has" (count current-pop) "members.")

        ;; UPDATED: Now captures and passes ancestry metadata when creating offspring.
        (let [offspring (->> current-pop
                             (map (fn [parent-prompt]
                                    (let [response (apply-meta-prompt ctx refine-prompt parent-prompt call-template-fn)]
                                      (when-not (:error response)
                                        (let [ancestry-metadata {:parents [(get-in parent-prompt [:header :id])]
                                                                 :generator {:model "mistral"
                                                                             :meta-prompt (get-in refine-prompt [:header :id])}}
                                              new-content (:content response)]
                                          (pop/ingest-prompt ctx new-content :metadata ancestry-metadata))))))
                             (remove nil?)
                             (doall))]

          (log/info "Created" (count offspring) "new offspring prompts.")
          (let [new-full-population (concat current-pop offspring)]
            (pop/create-new-generation! ctx new-full-population)
            {:new-generation-number (inc latest-gen-num)
             :offspring-created (count offspring)}))))))
