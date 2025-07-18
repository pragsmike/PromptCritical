(ns pcrit.command.vary
  (:require [clojure.java.io :as io]
            [pcrit.config.interface :as config]
            [pcrit.expdir.interface :as expdir]
            [pcrit.llm.interface :as llm]
            [pcrit.log.interface :as log]
            [pcrit.pop.interface :as pop]
            [pcrit.pdb.interface :as pdb]
            [pcrit.results.interface :as results])
  (:import [java.time Instant]))

(defn- breed-prompt
  "The generic 'breeding' function for a single parent. Applies a chosen meta-prompt
  to a parent prompt to produce a new proto-prompt map with full metadata."
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

(defn- breed-from-crossover
  "Breeding function for two parents. Applies a crossover meta-prompt."
  [model-name meta-prompt parent-a parent-b run-id call-template-fn]
  (let [template (:body meta-prompt)
        vars {:OBJECT_PROMPT_A (:body parent-a)
              :OBJECT_PROMPT_B (:body parent-b)}]
    (let [{:keys [content generation-metadata error]} (call-template-fn model-name template vars)]
      (when-not error
        (let [ancestry-metadata {:parents [(get-in parent-a [:header :id])
                                           (get-in parent-b [:header :id])]
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
  (fn [dispatch-val _ctx _run-id _call-template-fn]
    (:strategy dispatch-val)))

(defmethod gen-offspring :refine
  [dispatch-val ctx run-id call-template-fn]
  (let [{:keys [evo-params population]} dispatch-val
        model-name (get-in evo-params [:vary :model] "openai/gpt-4o-mini")
        improve-prompt (pop/read-linked-prompt ctx "refine")]
    (if improve-prompt
      (do
        (log/info "Using :refine strategy. Applying meta-prompt" (get-in improve-prompt [:header :id]) "to" (count population) "parents.")
        (->> population
             (mapv (fn [parent] (breed-prompt model-name improve-prompt parent run-id call-template-fn)))
             (remove nil?)))
      (do
        (log/warn "Vary strategy is ':refine' but the linked prompt 'refine' was not found. Skipping generation.")
        []))))

(defmethod gen-offspring :crossover
  [dispatch-val ctx run-id call-template-fn]
  (let [{:keys [evo-params generation-number]} dispatch-val
        model-name (get-in evo-params [:vary :model] "openai/gpt-4o-mini")
        crossover-prompt (pop/read-linked-prompt ctx "crossover")
        prev-gen-num (dec generation-number)]
    (if-not crossover-prompt
      (do (log/warn "Vary strategy is ':crossover' but the linked prompt 'crossover' was not found. Skipping generation.")
          [])
      (if (< prev-gen-num 0)
        (do (log/warn "Cannot use :crossover strategy on generation 0. Skipping.")
            [])
        (let [contests-dir (expdir/get-contests-dir ctx prev-gen-num)
              contest-dirs (->> (file-seq contests-dir) (filter #(.isDirectory %)) (rest))]
          (if (empty? contest-dirs)
            (do (log/warn "Cannot use :crossover, no contest reports found in previous generation" prev-gen-num)
                [])
            (let [latest-contest-dir (last (sort contest-dirs))
                  latest-contest-name (.getName latest-contest-dir)
                  top-2-data (->> (results/parse-report ctx prev-gen-num latest-contest-name)
                                  (sort-by :score >)
                                  (take 2))
                  parent-a-id (:prompt (first top-2-data))
                  parent-b-id (:prompt (second top-2-data))
                  parent-a-record (pdb/read-prompt (expdir/get-pdb-dir ctx) parent-a-id)
                  parent-b-record (pdb/read-prompt (expdir/get-pdb-dir ctx) parent-b-id)]
              (if (and parent-a-record parent-b-record)
                (do
                  (log/info "Using :crossover strategy. Breeding" parent-a-id "and" parent-b-id "from contest" latest-contest-name)
                  (if-let [offspring (breed-from-crossover model-name crossover-prompt parent-a-record parent-b-record run-id call-template-fn)]
                    [offspring] ; Return a collection with the single offspring
                    []))
                (do (log/warn "Could not find one or both parents for crossover in PDB:" parent-a-id parent-b-id)
                    [])))))))))


(defmethod gen-offspring :default
  [dispatch-val _ctx _run-id _call-template-fn]
  (log/warn "Unknown vary strategy" (:strategy dispatch-val) "requested. Skipping generation.")
  [])


;; --- Main Command Function ---

(defn vary!
  "Adds new offspring to the latest generation by applying meta-prompts.
  Returns a map containing statistics about the run, including the total :cost."
  [ctx & [{:keys [call-template-fn] :or {call-template-fn llm/call-model-template}}]]
  (if-let [latest-gen-num (expdir/find-latest-generation-number ctx)]
    (let [evo-params (config/load-evolution-params ctx)
          current-pop (pop/load-population ctx latest-gen-num)
          current-pop-dir (expdir/get-population-dir ctx latest-gen-num)
          run-id (.toString (Instant/now))
          strategy (dispatch-strategy evo-params)
          dispatch-val {:strategy          strategy
                        :evo-params        evo-params
                        :population        current-pop
                        :generation-number latest-gen-num}]

      (log/info "Varying generation" latest-gen-num "with strategy" (str ":" strategy))

      (let [offspring-proto-prompts (gen-offspring dispatch-val ctx run-id call-template-fn)
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
