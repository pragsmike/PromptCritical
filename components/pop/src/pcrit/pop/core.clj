(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.pop.analysis :refer (analyze-prompt-body)]
            [pcrit.expdir.interface :as expdir]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn ingest-prompt
  "Stores given prompt with some computed metadata."
  [pdbdir prompt]
  (let [metadata-fn (fn [rec] (analyze-prompt-body (:body rec)))]
    (pdb/create-prompt pdbdir prompt :metadata-fn metadata-fn)))

(defn intern-prompts
  "Given a map of prompt-names to prompt-texts, ingests them into the pdb."
  [db-dir prompt-map]
  (into {}
        (for [[prompt-name prompt-text] prompt-map]
          (let [new-record  (ingest-prompt db-dir prompt-text)]
            [prompt-name new-record]))))

(defn read-prompt-map
  "Reads a prompt manifest, returning a map of prompt keys to prompt strings."
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
  "Ingests a set of raw prompt files listed in a manifest."
  [pdbdir prompt-manifest-filename]
  (->> (read-prompt-map prompt-manifest-filename)
       (intern-prompts pdbdir)))

(defn bootstrap
  "Initializes an experiment directory by creating its structure, ingesting
  prompts from its bootstrap.edn manifest, and creating named symlinks."
  [exp-dir]
  (expdir/create-experiment-dirs! exp-dir) ;; <-- Delegated

  (let [pdb-dir (expdir/get-pdb-dir exp-dir) ;; <-- Delegated
        manifest-file (expdir/bootstrap-spec-file exp-dir) ;; <-- Delegated
        prompt-map (ingest-from-manifest pdb-dir manifest-file)
        {:keys [seed refine vary]} prompt-map]

    (expdir/link-prompt! exp-dir seed "seed") ;; <-- Delegated
    (expdir/link-prompt! exp-dir refine "refine")
    (expdir/link-prompt! exp-dir vary "vary")))

(defn evolve [exp-dir]
  )
