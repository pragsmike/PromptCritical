(ns pcrit.experiment.core
  (:require [clojure.string :as str]))

(defn new-experiment-context
  "Creates a new ExperimentContext map."
  [exp-dir]
  {:pre [(string? exp-dir) (not (str/blank? exp-dir))]}
  {:exp-dir exp-dir})
