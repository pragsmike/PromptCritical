(ns pcrit.results.core
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [pcrit.expdir.interface :as expdir]
            [pcrit.config.interface :as config]
            [pcrit.log.interface :as log]))

(defn- calculate-cost
  "Calculates the monetary cost for a single Failter result map based on its token usage."
  [result-map]
  (let [usage     (:usage result-map)
        model     (:model_used usage)
        token-in  (or (:tokens_in usage) 0)
        token-out (or (:tokens_out usage) 0)
        price-table config/price-table]
    (if-let [pricing (get price-table model)]
      (+ (* (/ token-in 1000.0) (:in-per-1k pricing))
         (* (/ token-out 1000.0) (:out-per-1k pricing)))
      0.0)))

(defn- normalize-result
  "Transforms a single raw result from failter's JSON into a canonical map for PromptCritical's use."
  [raw-result]
  (let [usage (:usage raw-result)]
    {:prompt         (some-> (:prompt_id raw-result) (str/replace #"\.prompt$" ""))
     :score          (:score raw-result)
     :cost           (calculate-cost raw-result)
     :model          (:model_used usage)
     :tokens-in      (:tokens_in usage)
     :tokens-out     (:tokens_out usage)
     :retry-attempts (get-in raw-result [:performance :retry_attempts])
     :error          (:error raw-result)
     :raw            raw-result}))

(defn parse-report
  "Reads and parses the failter-report.json for a given contest.
  This is the new source of truth for contest results. It calculates cost
  and returns a sequence of canonical result maps."
  [ctx generation-number contest-name]
  (let [contest-dir (expdir/get-contest-dir ctx generation-number contest-name)
        report-file (io/file contest-dir "failter-report.json")]
    (if (.exists report-file)
      (try
        (let [parsed-json (json/read-str (slurp report-file) :key-fn keyword)]
          (->> parsed-json
               (map normalize-result)
               (doall)))
        (catch Exception e
          (log/error "Failed to parse failter-report.json for contest" contest-name ":" (.getMessage e))
          []))
      (do
        (log/warn "Could not find failter-report.json for contest:" contest-name "in generation" generation-number)
        []))))
