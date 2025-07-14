(ns pcrit.command.evaluate
  (:require [clojure.java.io :as io]
            [pcrit.config.interface :as config]
            [pcrit.expdir.interface :as expdir]
            [pcrit.failter.interface :as failter]
            [pcrit.log.interface :as log]
            [pcrit.pop.interface :as pop]
            [pcrit.reports.interface :as reports]))

(defn- validate-options [ctx {:keys [generation-number contest-name inputs-dir models]}]
  (let [gen-dir (expdir/get-generation-dir ctx generation-number)
        contest-dir (expdir/get-contest-dir ctx generation-number contest-name)
        population (pop/load-population ctx generation-number)]
    (cond
      (not (.exists gen-dir))
      {:valid? false :reason (str "Generation " generation-number " does not exist.")}

      (or (nil? inputs-dir) (not (and (.exists (io/file inputs-dir)) (.isDirectory (io/file inputs-dir)))))
      {:valid? false :reason (str "Inputs directory not found or not a directory: " inputs-dir)}

      (or (nil? models) (empty? models))
      {:valid? false :reason "No models specified for evaluation. Check :evaluate/:models in evolution-parameters.edn."}

      (.exists contest-dir)
      {:valid? false :reason (str "Contest '" contest-name "' already exists for generation " generation-number ".")}

      (empty? population)
      {:valid? false :reason (str "Population for generation " generation-number " is empty.")}

      :else
      {:valid? true :population population})))

(defn- get-and-log-contest-cost [processed-report-data]
  (if (seq processed-report-data)
    (let [costs (->> processed-report-data
                     (keep :cost) ; Use keep to handle both strings and numbers, and filter nils
                     (map #(if (string? %) (Double/parseDouble %) %))
                     (remove nil?))
          total-cost (reduce + 0.0 costs)]
      (log/info (format "Contest completed. Total calculated cost: $%.4f" total-cost))
      total-cost)
    0.0))

(defn evaluate!
  "Orchestrates the evaluation of a prompt population.
  Returns a map with contest results, including the calculated :cost."
  [ctx {:keys [generation name inputs judge-model]}]
  (let [evo-params        (config/load-evolution-params ctx)
        eval-config       (:evaluate evo-params)
        models-to-test    (:models eval-config)
        final-judge-model (or judge-model (:judge-model eval-config))
        gen-num           (or generation (expdir/find-latest-generation-number ctx))
        contest-name      (or name "contest")]

    (if-not gen-num
      (do (log/error "Cannot evaluate: No generations found in this experiment.")
          {:success false :cost 0.0})
      (let [options-to-validate {:generation-number gen-num
                                 :contest-name      contest-name
                                 :inputs-dir        inputs
                                 :models            models-to-test}
            {:keys [valid? reason population]} (validate-options ctx options-to-validate)]
        (if-not valid?
          (do (log/error "Evaluation validation failed:" reason)
              {:success false :cost 0.0})
          (do
            (log/info "Starting evaluation for generation" gen-num "with contest name '" contest-name "'")
            (let [contest-params {:generation-number gen-num
                                  :contest-name      contest-name
                                  :inputs-dir        inputs
                                  :population        population
                                  :models            models-to-test
                                  :judge-model       final-judge-model}
                  {:keys [success json-report]} (failter/run-contest! ctx contest-params)]

              (if success
                (let [report-csv-path (io/file (expdir/get-contest-dir ctx gen-num contest-name) "report.csv")
                      processed-data (reports/process-and-write-csv-report! json-report (.getCanonicalPath report-csv-path))
                      cost (get-and-log-contest-cost processed-data)]
                  {:success true :cost cost :contest-name contest-name})
                {:success false :cost 0.0}))))))))
