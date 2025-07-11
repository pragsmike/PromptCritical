(ns pcrit.expdir.interface
  (:require [pcrit.expdir.core :as core]))

;; --- Top-Level ---

(def create-experiment-dirs!
  "Creates the standard subdirectories (pdb, generations, links, seeds)
  within the experiment directory specified in the `ctx`. Idempotent."
  core/create-experiment-dirs!)

(def get-pdb-dir
  "Returns a File object for the 'pdb' directory from the `ctx`."
  core/get-pdb-dir)

(def get-generations-dir
  "Returns a File object for the 'generations' directory from the `ctx`."
  core/get-generations-dir)

(def get-link-dir
  "Returns a File object for the 'links' directory from the `ctx`."
  core/get-link-dir)

(def get-seeds-dir
  "Returns a File object for the 'seeds' directory from the `ctx`."
  core/get-seeds-dir)

(def bootstrap-spec-file
  "Returns a File object for the 'bootstrap.edn' file from the `ctx`."
  core/bootstrap-spec-file)

(def pdb-file-of-prompt-record
  "Given a context and a prompt record, return a File object
  representing the prompt's canonical path within the experiment's pdb."
  core/pdb-file-of-prompt-record)

(def link-prompt!
  "Given a context and a prompt record, symlink its pdb file into the links
  directory under the given name."
  core/link-prompt!)

(def create-relative-symlink!
  "Creates a relative symbolic link from `link-file` to `target-file`."
  core/create-relative-symlink!)

;; --- Generation & Contest Specific ---

(def get-generation-dir
  "Returns a File object for a specific generation directory, e.g., '.../generations/gen-007'."
  core/get-generation-dir)

(def get-population-dir
  "Returns a File object for the 'population' subdirectory of a specific generation."
  core/get-population-dir)

(def get-contests-dir
  "Returns a File object for the 'contests' subdirectory of a specific generation."
  core/get-contests-dir)

(def get-contest-dir
  "Returns a File object for a uniquely named contest within a specific generation."
  core/get-contest-dir)

(def get-failter-spec-dir
  "Returns a File object for the 'failter-spec' subdirectory within a contest."
  core/get-failter-spec-dir)

(def find-latest-generation-number
  "Scans the 'generations' directory, returning the highest generation number found, or nil."
  core/find-latest-generation-number)

;; --- NEW: High-Level Contest Management ---

(def prepare-contest-directory!
  "Creates the full directory structure and symlinks for a Failter contest.
  This is the authoritative function for contest setup. Takes a context map
  and a contest parameters map."
  core/prepare-contest-directory!)

(def capture-contest-report!
  "Moves the report.csv from the failter-spec dir to the parent contest dir.
  Returns a File object to the final report path, or nil if not found."
  core/capture-contest-report!)
