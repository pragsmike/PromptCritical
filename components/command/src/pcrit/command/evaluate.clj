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

      (not (and (.exists (io/file inputs-dir)) (.isDirectory (io/file inputs-dir))))
      {:valid? false :reason (str "Inputs directory not found: " inputs-dir)}

      (empty? models)
      {:valid? false :reason (str "No models specified for evaluation in evolution-parameters.edn.")}

      (.exists contest-dir)
      {:valid? false :reason (str "Contest '" contest-name "' already exists for generation " generation-number ".")}

      (empty? population)
      {:valid? false :reason (str "Population for generation " generation-number " is empty.")}

      :else
      {:valid? true :population population})))

(defn- log-contest-cost! [report-path]
  (when report-path
    (let [report-data (reports/parse-report report-path)
          total-cost (reduce + 0.0 (map :cost report-data))]
      (log/info (format "Contest completed. Total cost: $%.4f" total-cost)))))

(defn evaluate!
  "Orchestrates the evaluation of a prompt population for a given generation."
  [ctx {:keys [generation name inputs judge-model]}]
  (let [evo-params        (config/load-evolution-params ctx)
        eval-config       (:evaluate evo-params)
        models-to-test    (:models eval-config)
        final-judge-model (or judge-model (:judge-model eval-config))
        gen-num           (or generation (expdir/find-latest-generation-number ctx))
        contest-name      (or name "contest")]

    (if-not gen-num
      (log/error "Cannot evaluate: No generations found in this experiment.")
      (let [options-to-validate {:generation-number gen-num
                                 :contest-name      contest-name
                                 :inputs-dir        inputs
                                 :models            models-to-test}
            {:keys [valid? reason population]} (validate-options ctx options-to-validate)]
        (if-not valid?
          (log/error "Evaluation validation failed:" reason)
          (do
            (log/info "Starting evaluation for generation" gen-num "with contest name '" contest-name "'")
            (let [contest-params {:generation-number gen-num
                                  :contest-name      contest-name
                                  :inputs-dir        inputs
                                  :population        population
                                  :models            models-to-test
                                  :judge-model       final-judge-model}
                  {:keys [success report-path]} (failter/run-contest! ctx contest-params)]
              (when success
                (log-contest-cost! report-path)))))))))
