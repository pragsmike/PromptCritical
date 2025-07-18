(ns pcrit.reports.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure-csv.core :as csv]
            [pcrit.log.interface :as log]))

(defn- to-str [v] (if (nil? v) "" (str v)))

(defn- cost-to-str
  "Formats a numeric cost into a string, avoiding scientific notation."
  [cost]
  (if (nil? cost)
    ""
    (let [scaled-cost (* cost 10000000) ; Scale to avoid precision issues
          rounded (Math/round (double scaled-cost))]
      (if (zero? rounded)
        "0.0"
        (-> (format "%.7f" (double (/ rounded 10000000)))
            (str/replace #"0*$" "") ; Remove trailing zeros
            (str/replace #"\.$" ".0")))))) ; Ensure trailing decimal is ".0"

(defn- format-for-csv
  "Transforms a normalized result map into a flat map of strings suitable for CSV writing."
  [normalized-result-map]
  {:prompt         (to-str (:prompt normalized-result-map))
   :score          (to-str (:score normalized-result-map))
   :model          (to-str (:model normalized-result-map))
   :cost           (cost-to-str (:cost normalized-result-map))
   :tokens-in      (to-str (:tokens-in normalized-result-map))
   :tokens-out     (to-str (:tokens-out normalized-result-map))
   :retry-attempts (to-str (:retry-attempts normalized-result-map))
   :error          (to-str (:error normalized-result-map))})

(defn process-and-write-csv-report!
  "Takes a sequence of normalized result maps (from `pcrit.results/parse-report`),
  formats them, and writes a human-readable CSV report to the specified path.
  Returns a sequence of the final, formatted data maps."
  [normalized-results-data target-csv-path]
  (let [formatted-data (map format-for-csv normalized-results-data)]
    (if (seq formatted-data)
      (let [headers (-> formatted-data first keys)
            rows (mapv (fn [row-map] (mapv row-map headers)) formatted-data)
            csv-data (cons (map name headers) rows)]
        (io/make-parents target-csv-path)
        (spit target-csv-path (csv/write-csv csv-data))
        (log/info "Wrote human-readable contest results to" target-csv-path))
      (log/warn "No data available to write to human-readable report.csv."))
    formatted-data))
