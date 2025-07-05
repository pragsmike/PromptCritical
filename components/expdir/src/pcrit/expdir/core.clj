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

(defn get-seeds-dir [exp-dir] (io/file exp-dir "seeds"))
(defn get-pdb-dir [exp-dir] (io/file exp-dir "pdb"))
(defn get-link-dir [exp-dir] (io/file exp-dir "links"))
(defn get-generations-dir [exp-dir] (io/file exp-dir "generations"))
(defn bootstrap-spec-file [exp-dir] (io/file exp-dir "bootstrap.edn"))


(defn create-experiment-dirs!
  "Creates the standard subdirectories within a given experiment root directory."
  [exp-dir]
  (.mkdirs (get-pdb-dir exp-dir))
  (.mkdirs (get-generations-dir exp-dir))
  (.mkdirs (get-link-dir exp-dir))
  (.mkdirs (get-seeds-dir exp-dir)))

(defn pdb-file-of-prompt-record
  "Given an experiment directory and a prompt record, returns a File object
  representing the prompt's canonical path within the experiment's pdb."
  [exp-dir record]
  (let [pdb-dir (get-pdb-dir exp-dir)
        prompt-id (get-in record [:header :id])]
    (if-not prompt-id
      (throw (ex-info "Cannot determine prompt path. Record is missing :id in header."
                      {:record record}))
      (io/file pdb-dir (str prompt-id ".prompt")))))

;; --- Linking Function ---

(defn link-prompt!
  "Creates a symbolic link to a prompt file in the experiment's 'links' directory."
  [exp-dir prompt-record link-name]
  (let [target-file (pdb-file-of-prompt-record exp-dir prompt-record)
        link-file   (io/file (get-link-dir exp-dir) link-name)]
    (Files/createSymbolicLink (.toPath link-file)
                              (.toPath target-file)
                              (make-array FileAttribute 0))))
