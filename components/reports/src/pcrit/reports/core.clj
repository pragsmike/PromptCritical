(ns pcrit.reports.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure-csv.core :as csv]
            [clojure.data.json :as json]
            [pcrit.log.interface :as log]
            [pcrit.config.interface :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LEGACY CSV PARSING (for `stats` on old reports)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
  "Reads a legacy CSV report file and returns a sequence of maps."
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
               (filter #(not (str/blank? (:prompt %))))
               (doall)))
        []))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NEW FAILTER JSON PARSING, COST CALCULATION, and CSV WRITING
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-failter-json-report
  "Parses the JSON output stream from the new `failter run` command."
  [json-string]
  (try
    (when json-string
      (json/read-str json-string :key-fn keyword))
    (catch Exception e
      (log/error "Failed to parse JSON report stream from Failter:" (.getMessage e))
      [])))

(defn- calculate-cost
  "Calculates the monetary cost for a single Failter result based on token usage."
  [failter-result-map price-table]
  (let [usage     (:usage failter-result-map)
        model     (:model_used usage)
        token-in  (or (:tokens_in usage) 0)
        token-out (or (:tokens_out usage) 0)]
    (if-let [pricing (get price-table model)]
      (+ (* (/ token-in 1000.0) (:in-per-1k pricing))
         (* (/ token-out 1000.0) (:out-per-1k pricing)))
      0.0)))

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

(defn- flatten-and-normalize-result
  "Transforms a nested Failter JSON result into a flat map suitable for CSV writing."
  [result-map]
  (let [usage (:usage result-map)
        performance (:performance result-map)]
    {:prompt       (str/replace (to-str (:prompt_id result-map)) #"\.prompt$" "")
     :score        (to-str (:score result-map))
     :model        (to-str (:model_used usage))
     :cost         (cost-to-str (calculate-cost result-map config/price-table))
     :tokens-in    (to-str (:tokens_in usage))
     :tokens-out   (to-str (:tokens_out usage))
     :retry-attempts (to-str (:retry_attempts performance))
     :error        (to-str (:error result-map))}))

(defn process-and-write-csv-report!
  "Takes the raw JSON output from `failter run`, processes it, and writes a report."
  [json-string target-csv-path]
  (let [parsed-data (parse-failter-json-report json-string)
        processed-data (map flatten-and-normalize-result parsed-data)]
    (if (seq processed-data)
      (let [headers (-> processed-data first keys)
            rows (mapv (fn [row-map] (mapv row-map headers)) processed-data)
            csv-data (cons (map name headers) rows)]
        (io/make-parents target-csv-path)
        (spit target-csv-path (csv/write-csv csv-data))
        (log/info "Wrote contest results to" target-csv-path))
      (log/warn "No data returned from Failter to write to report.csv."))
    processed-data))
