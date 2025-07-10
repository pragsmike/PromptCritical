(ns pcrit.failter.interface
  (:require [pcrit.failter.core :as core]))

(def run-contest!
  "Prepares a directory structure for a Failter contest, runs the toolchain,
  and captures the final `report.csv`.

  Usage:
  (run-contest! ctx {:keys [generation-number contest-name inputs-dir population]})

  - `ctx`: The standard experiment context.
  - `generation-number`: The integer of the generation being evaluated.
  - `contest-name`: A unique string name for this contest.
  - `inputs-dir`: A path to a directory of input files for Failter.
  - `population`: A sequence of prompt records to be evaluated."
  core/run-contest!)
