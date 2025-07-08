(ns pcrit.pop.interface
  (:require [pcrit.pop.core :as core]
            [pcrit.pop.analysis :as analysis]))

(def analyze-prompt-body
  "Analyzes a prompt's body string to compute basic metadata."
  analysis/analyze-prompt-body)

(def intern-prompts
  "Given a context and a map of names to texts, ingests them into the pdb."
  core/intern-prompts)

(def ingest-prompt core/ingest-prompt)

(def read-prompt-map
  "Reads a prompt manifest, returning a map of prompt keys to prompt strings."
  core/read-prompt-map)

(def ingest-from-manifest
  "Ingests a set of raw prompt files listed in a manifest, using a context."
  core/ingest-from-manifest)

(def read-linked-prompt
  "Resolves a named link in the 'links' directory to its full prompt record."
  core/read-linked-prompt)

(def create-new-generation!
  "Creates the directory structure for a new generation and populates it with
  symlinks to the provided population of prompt records. Returns a map with
  the new generation number and population size."
  core/create-new-generation!)

(def load-population
  "Loads all prompt records from a given generation's 'population' directory.
   Returns a sequence of prompt record maps, or an empty sequence if the
   directory does not exist."
  core/load-population)
