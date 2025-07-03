(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.pop.analysis :refer (analyze-prompt-body)]
            [pcrit.llm.interface :as llm]))

(defn ingest-prompt
  "Stores given prompt with some computed metadata.
  NOTE: The metadata is computed on the NON-NORMALIZED form of the prompt!
  Later computations that read from the file may get a slightly different string.
  The most noticable effect is that the count won't include the trailing newline that
  normalization might add if there isn't one already."
  [pdbdir prompt]
  (let [metadata (analyze-prompt-body prompt)]
    (pdb/create-prompt pdbdir prompt :metadata metadata)))

(defn bootstrap [pdbdir seed-prompt]
  (ingest-prompt pdbdir seed-prompt))
