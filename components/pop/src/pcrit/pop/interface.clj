(ns pcrit.pop.interface
  (:require [pcrit.pop.core :as core]
            [pcrit.pop.analysis :as analysis]))

(def analyze-prompt-body
  "Analyzes a prompt's body string to compute basic metadata."
  analysis/analyze-prompt-body)

(def intern-prompts
  "Given a context and a map of names to texts, ingests them into the pdb."
  core/intern-prompts)

(def read-prompt-map
  "Reads a prompt manifest, returning a map of prompt keys to prompt strings."
  core/read-prompt-map)

(def ingest-from-manifest
  "Ingests a set of raw prompt files listed in a manifest, using a context."
  core/ingest-from-manifest)
