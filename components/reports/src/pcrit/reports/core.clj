(ns pcrit.reports.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure-csv.core :as csv]
            [pcrit.log.interface :as log]))

(defn- -try-parse-double [s]
  (try
    (Double/parseDouble s)
    (catch Exception _ nil)))

(defn- -row->map [header-keys row]
  (loop [[k & ks] header-keys
         [v & vs] row
         acc {}]
    (if-not k
      acc
      (let [new-val (if (or (str/includes? (name k) "score")
                            (str/includes? (name k) "cost"))
                      (-try-parse-double v)
                      v)]
        (if (and (nil? new-val) (some? v) (not (str/blank? v)))
          (do
            (log/warn "Skipping malformed row in report. Could not parse" (name k) "value:" v)
            nil)
          (recur ks vs (assoc acc k new-val)))))))

(defn parse-report
  "Reads a CSV report file and returns a sequence of maps.
  Each map represents a row, with keywordized headers. Numeric columns
  like 'score' and 'cost' are parsed into doubles. Malformed or blank rows are skipped."
  [report-file]
  (if-not (.exists (io/file report-file))
    []
    (let [csv-data (csv/parse-csv (slurp report-file))]
      (if-let [header (first csv-data)]
        (let [header-keys (map keyword header)
              rows (rest csv-data)]
          (->> rows
               (map #(-row->map header-keys %))
               (remove nil?)
               ;; CORRECTED: Filter out rows that don't have a valid prompt ID.
               (filter #(not (str/blank? (:prompt %))))
               (doall)))
        []))))
