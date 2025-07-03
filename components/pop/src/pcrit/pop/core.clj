(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.llm.interface :as llm]))

(defn bootstrap [pdbdir seed-prompt]
  (pdb/create-prompt pdbdir seed-prompt))
