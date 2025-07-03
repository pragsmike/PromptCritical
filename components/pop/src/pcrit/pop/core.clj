(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.pop.analysis :refer (analyze-prompt-body)]
            [pcrit.llm.interface :as llm]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn ingest-prompt
  "Stores given prompt with some computed metadata.
  NOTE: The metadata is computed on the canonicalized form of the prompt!
  Later computations that read from the file will use exactly that same text."
  [pdbdir prompt]
  (let [metadata-fn (fn [rec] (analyze-prompt-body (:body rec)))]
    (pdb/create-prompt pdbdir prompt :metadata-fn metadata-fn)))


(defn intern-prompts
  "Given a map whose keys are prompt names and values are prompt strings,
  ingests each of those prompt strings into the prompt database. It also
  automatically runs basic analysis on the prompt body and adds the results
  to the prompt's metadata upon creation.

  Returns a map with those same keys, but with the prompt string replaced by the
  full prompt record map (with header, body) of the ingested prompt."
  [db-dir prompt-map]
  (into {}
        (for [[prompt-name prompt-text] prompt-map]
          (let [new-record  (ingest-prompt db-dir prompt-text)]
            [prompt-name new-record]))))

(defn read-prompt-map
  "Reads a prompt manifest from an EDN file, returning a map of prompt keys to prompt strings.
  This map is the required input format for the `intern-prompts` function.

  The EDN file must define a map where keys are prompt-keys (keywords) and values
  are string paths to raw prompt text files. These paths are relative to the
  location of the EDN file itself.

  Throws an ExceptionInfo if any of the referenced prompt files do not exist."
  [edn-filename]
  (let [edn-file (io/file edn-filename)
        base-dir (.getParentFile edn-file)
        prompt-paths-map (-> edn-file slurp edn/read-string)]
    (into {}
      (for [[prompt-key prompt-path] prompt-paths-map]
        (let [prompt-file (io/file base-dir prompt-path)]
          (if-not (.exists prompt-file)
            (throw (ex-info (str "Prompt file not found for key: " prompt-key)
                            {:key prompt-key
                             :path (.getCanonicalPath prompt-file)
                             :manifest edn-filename}))
            [prompt-key (slurp prompt-file)]))))))

(defn ingest-from-manifest
  "Ingests a set of raw prompt files listed in the manifest.

  The EDN file must define a map where keys are prompt-keys (keywords) and values
  are string paths to raw prompt text files. These paths are relative to the
  location of the EDN file itself.

  Throws an ExceptionInfo if any of the referenced prompt files do not exist."
  [pdbdir prompt-manifest-filename]
  (->> (read-prompt-map prompt-manifest-filename)
       (intern-prompts pdbdir)))

(defn evolve [pdbdir])

(defn bootstrap [pdbdir prompt-manifest-filename]
  (let [prompt-map (ingest-from-manifest pdbdir prompt-manifest-filename)
        seed (:seed prompt-map)
        refine (:refine prompt-map)
        vary (:vary prompt-map)


  ;; make symlinks with those logical names pointing into the store
        ]))
