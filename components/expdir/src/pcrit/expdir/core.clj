(ns pcrit.expdir.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]
           [java.nio.file Files Path Paths]
           [java.nio.file.attribute FileAttribute]))

;; --- Path Coercion Helper ---

(defn- ->path
  "Robustly coerces its argument into a java.nio.file.Path object."
  ^Path [p]
  (cond
    (instance? Path p) p
    (instance? File p) (.toPath ^File p)
    (string? p)        (Paths/get p (into-array String []))
    :else (throw (IllegalArgumentException.
                   (str "Cannot convert value of type " (class p) " to a Path.")))))

;; --- Centralized Symlink Function ---
(defn create-relative-symlink!
  "Creates a relative symbolic link from `link-file` to `target-file`."
  [link-file target-file]
  (let [link-dir             (.getParentFile link-file)
        link-dir-path        (->path link-dir)
        target-path          (->path target-file)
        relative-target-path (.relativize link-dir-path target-path)]
    (Files/createSymbolicLink (->path link-file)
                              relative-target-path
                              (make-array FileAttribute 0))))


;; --- Top-Level Directory Getters ---

(defn get-seeds-dir [{:keys [exp-dir]}] (io/file exp-dir "seeds"))
(defn get-pdb-dir [{:keys [exp-dir]}] (io/file exp-dir "pdb"))
(defn get-link-dir [{:keys [exp-dir]}] (io/file exp-dir "links"))
(defn get-generations-dir [{:keys [exp-dir]}] (io/file exp-dir "generations"))
(defn bootstrap-spec-file [{:keys [exp-dir]}] (io/file exp-dir "bootstrap.edn"))


(defn create-experiment-dirs!
  "Creates the standard subdirectories within a given experiment root directory."
  [ctx]
  (.mkdirs (get-pdb-dir ctx))
  (.mkdirs (get-generations-dir ctx))
  (.mkdirs (get-link-dir ctx))
  (.mkdirs (get-seeds-dir ctx)))

(defn pdb-file-of-prompt-record
  "Given a context and a prompt record, returns a File object
  representing the prompt's canonical path within the experiment's pdb."
  [ctx record]
  (let [pdb-dir (get-pdb-dir ctx)
        prompt-id (get-in record [:header :id])]
    (if-not prompt-id
      (throw (ex-info "Cannot determine prompt path. Record is missing :id in header."
                      {:record record}))
      (io/file pdb-dir (str prompt-id ".prompt")))))

(defn link-prompt!
  "Creates a relative symbolic link to a prompt file in the experiment's 'links' directory."
  [ctx prompt-record link-name]
  (let [target-file (pdb-file-of-prompt-record ctx prompt-record)
        link-file   (io/file (get-link-dir ctx) link-name)]
    (create-relative-symlink! link-file target-file)))


;; --- Generation and Contest Path Getters ---

(defn get-generation-dir
  "Returns a File object for a specific generation directory (e.g., '.../generations/gen-007')."
  [ctx gen-number]
  (io/file (get-generations-dir ctx) (format "gen-%03d" gen-number)))

(defn get-population-dir
  "Returns a File object for the 'population' subdirectory of a specific generation."
  [ctx gen-number]
  (io/file (get-generation-dir ctx gen-number) "population"))

(defn get-contests-dir
  "Returns a File object for the 'contests' subdirectory of a specific generation."
  [ctx gen-number]
  (io/file (get-generation-dir ctx gen-number) "contests"))

(defn get-contest-dir
  "Returns a File object for a uniquely named contest within a specific generation."
  [ctx gen-number contest-name]
  (io/file (get-contests-dir ctx gen-number) contest-name))

(defn get-failter-artifacts-dir
  "Returns a File object for the 'failter-artifacts' subdirectory within a contest,
  used for storing intermediate files to ensure idempotency."
  [ctx gen-number contest-name]
  (io/file (get-contest-dir ctx gen-number contest-name) "failter-artifacts"))


;; --- Discovery Functions ---

(defn find-latest-generation-number
  "Scans the 'generations' directory and returns the highest generation number found.
  Returns nil if no valid 'gen-NNN' directories exist."
  [ctx]
  (let [gens-dir (get-generations-dir ctx)]
    (when (.exists gens-dir)
      (let [gen-files (.listFiles gens-dir)
            gen-name-pattern #"^gen-(\d{3})$"
            gen-numbers (->> gen-files
                             (map #(.getName %))
                             (map #(re-matches gen-name-pattern %))
                             (remove nil?)
                             (map second)
                             (map #(Integer/parseInt %)))]
        (when (seq gen-numbers)
          (apply max gen-numbers))))))
