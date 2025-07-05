(ns pcrit.expdir.interface
  (:require [pcrit.expdir.core :as core]))

(def create-experiment-dirs!
  "Creates the standard subdirectories (pdb, generations, links, seeds)
  within a given experiment root directory. This operation is idempotent."
  core/create-experiment-dirs!)

(def get-pdb-dir
  "Returns a File object for the 'pdb' directory within an experiment."
  core/get-pdb-dir)

(def get-generations-dir
  "Returns a File object for the 'generations' directory within an experiment."
  core/get-generations-dir)

(def get-link-dir
  "Returns a File object for the 'links' directory within an experiment."
  core/get-link-dir)

(def get-seeds-dir
  "Returns a File object for the 'seeds' directory within an experiment."
  core/get-seeds-dir)

(def bootstrap-spec-file
  "Returns a File object for the 'bootstrap.edn' file in an experiment."
  core/bootstrap-spec-file)

(def pdb-file-of-prompt-record
  "Given an experiment directory and a prompt record, return a File object
  representing the prompt's canonical path within the experiment's pdb."
  core/pdb-file-of-prompt-record)

(def link-prompt!
  "Given a prompt record, symlink its pdb file into the links directory
  under the given name."
  core/link-prompt!)
