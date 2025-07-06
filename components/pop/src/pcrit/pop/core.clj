(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.pop.analysis :refer (analyze-prompt-body)]
            [pcrit.expdir.interface :as expdir]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn ingest-prompt
  "Private helper. Stores a single prompt string with computed metadata."
  [ctx prompt-text]
  (let [pdb-dir     (expdir/get-pdb-dir ctx)
        metadata-fn (fn [rec] (analyze-prompt-body (:body rec)))]
    (pdb/create-prompt pdb-dir prompt-text :metadata-fn metadata-fn)))

(defn intern-prompts
  "Given a context and a map of names-to-texts, ingests them into the pdb."
  [ctx prompt-map]
  (into {}
        (for [[prompt-name prompt-text] prompt-map]
          ;; Correctly call the private helper with the full context
          (let [new-record (ingest-prompt ctx prompt-text)]
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
  [ctx prompt-manifest-filename]
  (->> (read-prompt-map prompt-manifest-filename)
       (intern-prompts ctx)))
