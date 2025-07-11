(ns pcrit.command.select
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.config.interface :as config]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]
            [pcrit.pop.interface :as pop]
            [pcrit.pdb.interface :as pdb]
            [pcrit.reports.interface :as reports])
  (:import [java.time Instant]))

(defn- parse-policy [policy-str]
  (if-let [[_ n] (re-matches #"top-N=(\d+)" policy-str)]
    {:type :top-n, :n (Integer/parseInt n)}
    (do
      (log/warn "Invalid policy string:" policy-str ". Defaulting to " (:selection-policy config/defaults))
      ;; This assumes the default policy is also top-N. This is acceptable
      ;; for now, as the next refactoring will make this more robust.
      (if-let [[_ default-n] (re-matches #"top-N=(\d+)" (:selection-policy config/defaults))]
        {:type :top-n, :n (Integer/parseInt default-n)}
        ;; Fallback in case the default in config is malformed.
        {:type :top-n, :n 5}))))

(defn- apply-selection-policy [report-data policy]
  (let [sorted-data (sort-by :score > report-data)]
    (case (:type policy)
      :top-n (take (:n policy) sorted-data)
      ;; Default case
      (do
        (log/warn "Unknown selection policy type:" (:type policy))
        (take 5 sorted-data)))))

(defn- update-survivor-metadata! [ctx survivor-id selection-event]
  (let [pdb-dir (expdir/get-pdb-dir ctx)
        update-fn (fn [header]
                    (let [current-val (get header :selection)
                          base-vec (cond
                                     (vector? current-val) current-val
                                     (map? current-val) [current-val]
                                     (coll? current-val) (vec current-val)
                                     :else [])]
                      (assoc header :selection (conj base-vec selection-event))))]
    (pdb/update-metadata pdb-dir survivor-id update-fn)))

(defn- validate-options [ctx {:keys [generation-number from-contest]}]
  (let [contest-dir (expdir/get-contest-dir ctx generation-number from-contest)
        report-file (io/file contest-dir "report.csv")]
    (cond
      (not (.exists report-file))
      {:valid? false :reason (str "Report file not found at: " (.getCanonicalPath report-file))}

      :else
      {:valid? true :report-file report-file})))

(defn select!
  "Selects survivor prompts from a contest, appends selection metadata,
  and creates a new generation with the winners."
  [ctx {:keys [generation from-contest policy]}]
  (let [latest-gen (expdir/find-latest-generation-number ctx)
        gen-num    (or generation latest-gen)
        policy-str (or policy (:selection-policy config/defaults))]

    (if-not gen-num
      (log/error "Cannot select: No generations found in this experiment.")
      (let [opts {:generation-number gen-num :from-contest from-contest}
            {:keys [valid? reason report-file]} (validate-options ctx opts)]
        (if-not valid?
          (log/error "Select validation failed:" reason)
          (let [report-data     (reports/parse-report report-file)
                parsed-policy   (parse-policy policy-str)
                survivors-data  (apply-selection-policy report-data parsed-policy)
                survivor-ids    (map :prompt survivors-data)
                selection-event {:contest-name from-contest
                                 :policy       policy-str
                                 :select-run   (.toString (Instant/now))}
                _               (log/info "Selected" (count survivor-ids) "survivors from generation" gen-num "using policy" policy-str)
                survivor-records (doall (map #(pdb/read-prompt (expdir/get-pdb-dir ctx) %) survivor-ids))]

            ;; Append metadata to each survivor in the PDB
            (doseq [survivor survivor-records]
              (update-survivor-metadata! ctx (get-in survivor [:header :id]) selection-event))

            ;; Create the new generation with symlinks to the survivors
            (when (seq survivor-records)
              (let [gen-info (pop/create-new-generation! ctx survivor-records)]
                (log/info "Created new generation" (:generation-number gen-info) "with" (:population-size gen-info) "survivors.")))))))))
