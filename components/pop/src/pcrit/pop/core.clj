(ns pcrit.pop.core
  (:require [pcrit.pdb.interface :as pdb]
            [pcrit.pop.analysis :refer (analyze-prompt-body)]
            [pcrit.expdir.interface :as expdir]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [java.io File]
           [java.nio.file Files Path Paths]))

;;; --- Ingestion from Manifest ---

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


;;; --- Population Management ---

(defn load-population
  "Loads all prompt records from a given generation's 'population' directory.
  Returns a sequence of prompt record maps, or an empty sequence if the
  directory does not exist."
  [ctx gen-number]
  (let [pop-dir (expdir/get-population-dir ctx gen-number)
        pdb-dir (expdir/get-pdb-dir ctx)]
    (if-not (.exists pop-dir)
      []
      (let [prompt-files (.listFiles pop-dir)]
        (->> prompt-files
             (map #(.getName %))
             (map #(str/replace % #"\.prompt$" "")) ; "P123.prompt" -> "P123"
             (map #(pdb/read-prompt pdb-dir %))
             (remove nil?))))))

(defn- ->path
  "Robustly coerces its argument into a java.nio.file.Path object."
  ^Path [p]
  (cond
    (instance? Path p) p
    (instance? File p) (.toPath ^File p)
    (string? p)        (Paths/get p (into-array String []))
    :else (throw (IllegalArgumentException.
                   (str "Cannot convert value of type " (class p) " to a Path.")))))

(defn- create-population-link!
  "Private helper to create a single relative symlink in a population directory."
  [ctx pop-dir-path prompt-record]
  (let [target-file (expdir/pdb-file-of-prompt-record ctx prompt-record)
        link-name   (str (get-in prompt-record [:header :id]) ".prompt")
        link-file   (io/file pop-dir-path link-name)
        target-path (->path target-file)
        relative-target-path (.relativize (->path pop-dir-path) target-path)]
    (Files/createSymbolicLink (->path link-file) relative-target-path (make-array java.nio.file.attribute.FileAttribute 0))))

(defn create-new-generation!
  "Creates the directory structure for a new generation and populates it with
  symlinks to the provided population of prompt records. Returns a map with
  the new generation number and population size."
  [ctx population]
  (let [latest-gen (expdir/find-latest-generation-number ctx)
        new-gen-number (if latest-gen (inc latest-gen) 0)
        pop-dir (expdir/get-population-dir ctx new-gen-number)]
    (.mkdirs pop-dir)
    (doseq [prompt-record population]
      (create-population-link! ctx pop-dir prompt-record))
    {:generation-number new-gen-number
     :population-size (count population)}))
