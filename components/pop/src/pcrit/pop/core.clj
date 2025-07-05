(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.pop.analysis :refer (analyze-prompt-body)]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.util :as util]
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


(defn create-experiment-dirs!
  "Creates the standard subdirectories (pdb, generations, links, seeds)
  within a given experiment root directory. This operation is idempotent."
  [exp-root-dir]
  (doseq [subdir ["pdb" "generations" "links" "seeds"]]
    (.mkdirs (io/file exp-root-dir subdir))))

(defn get-seeds-dir [expdir] (io/file (io/file expdir) "seeds"))
(defn get-pdb-dir [expdir] (io/file (io/file expdir) "pdb"))
(defn get-link-dir [expdir] (io/file (io/file expdir) "links"))

(defn bootstrap-spec-file [expdir] (io/file expdir (io/file "bootstrap.edn")))

(defn pdb-file-of-prompt-record
  "Given an experiment directory and a prompt record, return a File object
  representing the prompt's canonical path within the experiment's pdb."
  [exp-dir record]
  (let [pdb-dir (get-pdb-dir exp-dir)
        prompt-id (get-in record [:header :id])]
    (if-not (and pdb-dir prompt-id)
      (throw (ex-info "Cannot determine prompt path. Missing experiment directory or prompt ID."
                      {:exp-dir exp-dir
                       :prompt-id prompt-id}))
      (io/file pdb-dir (str prompt-id ".prompt")))))

(defn link-prompt
  "Given a prompt record, symlink its pdb file into links directory under given name."
  [expdir prompt-record name]
  (let [pdbfile (pdb-file-of-prompt-record expdir prompt-record)
        linkdir  (get-link-dir expdir)
        target  (io/file linkdir name)]
    (util/create-link pdbfile target)))

(defn bootstrap [expdir]
  (create-experiment-dirs! expdir)

  (let [pdbdir (get-pdb-dir expdir)
        prompt-manifest-filename (bootstrap-spec-file expdir)
        prompt-map (ingest-from-manifest pdbdir prompt-manifest-filename)
        seed (:seed prompt-map)
        refine (:refine prompt-map)
        vary (:vary prompt-map)]

    (link-prompt expdir seed "seed")
    (link-prompt expdir refine "refine")
    (link-prompt expdir vary "vary")
    ))




(defn evolve [expdir]
  )
