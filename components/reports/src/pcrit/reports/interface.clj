(ns pcrit.reports.interface
  (:require [pcrit.reports.core :as core]))

(def process-and-write-csv-report!
  "Takes a sequence of normalized result maps (from `pcrit.results/parse-report`),
  formats them into strings, and writes a human-readable `report.csv` file.
  This CSV is an artifact for manual inspection and is not used by the core
  evolutionary loop. Returns a sequence of the final, formatted data maps."
  core/process-and-write-csv-report!)
