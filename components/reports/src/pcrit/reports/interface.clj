(ns pcrit.reports.interface
  (:require [pcrit.reports.core :as core]))

(def parse-report
  "Reads a CSV report file and returns a sequence of maps.
  Each map represents a row, with keywordized headers. Numeric columns
  like 'score' and 'cost' are parsed into doubles. Malformed rows are skipped.

  Usage: (parse-report report-file)
  - `report-file`: A file object or path string to the report.csv file.
  - Returns a sequence of maps, e.g., '({:prompt \"P1\", :score 95.5} ...)', or an empty sequence."
  core/parse-report)
