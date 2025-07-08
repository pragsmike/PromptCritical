(ns pcrit.experiment.interface
  (:require [pcrit.experiment.core :as core]))

(def new-experiment-context
  "Constructor for the ExperimentContext.
  Usage: (new-experiment-context \"/path/to/experiment\")"
  core/new-experiment-context)
