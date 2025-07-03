(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.pop.analysis :refer (analyze-prompt-body)]
            [pcrit.llm.interface :as llm]))

(defn ingest-prompt
  "Stores given prompt with some computed metadata.
  NOTE: The metadata is computed on the canonicalized form of the prompt!
  Later computations that read from the file will use exactly that same text."
  [pdbdir prompt]
  (let [metadata-fn (fn [rec] (analyze-prompt-body (:body rec)))]
    (pdb/create-prompt pdbdir prompt :metadata-fn metadata-fn)))

(defn bootstrap [pdbdir seed-prompt]
  (ingest-prompt pdbdir seed-prompt))
