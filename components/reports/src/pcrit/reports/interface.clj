(ns pcrit.reports.interface
  (:require [pcrit.reports.core :as core]))

(def parse-report
  "Reads a legacy CSV report file and returns a sequence of maps.
  Each map represents a row, with keywordized headers. Numeric columns
  like 'score' and 'cost' are parsed into doubles. Malformed rows are skipped.

  Usage: (parse-report report-file)
  - `report-file`: A file object or path string to the report.csv file.
  - Returns a sequence of maps, e.g., '({:prompt \"P1\", :score 95.5} ...)', or an empty sequence."
  core/parse-report)

(def parse-failter-json-report
  "Parses the JSON output stream from the new `failter run` command.
  Returns a sequence of maps, where each map represents a result."
  core/parse-failter-json-report)

(def process-and-write-csv-report!
  "Takes the raw JSON output from `failter run`, processes it, calculates cost,
  and writes a standardized `report.csv` file to the specified path.
  Returns a sequence of the final, processed data maps."
  core/process-and-write-csv-report!)
