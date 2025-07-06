(ns pcrit.expdir.core
  (:require [clojure.java.io :as io])
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

;; --- Directory Structure Getters ---

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

;; --- Linking Function ---

(defn link-prompt!
  "Creates a symbolic link to a prompt file in the experiment's 'links' directory."
  [ctx prompt-record link-name]
  (let [target-file (pdb-file-of-prompt-record ctx prompt-record)
        link-file   (io/file (get-link-dir ctx) link-name)]
    (Files/createSymbolicLink (->path link-file)
                              (->path target-file)
                              (make-array FileAttribute 0))))
