(ns pcrit.command.select
  (:require [clojure.string :as str]
            [pcrit.config.interface :as config]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]
            [pcrit.pop.interface :as pop]
            [pcrit.pdb.interface :as pdb]
            [pcrit.results.interface :as results])
  (:import [java.time Instant]))

;; --- Pluggable Selection Policy Implementation ---

(defn- parse-policy
  "Parses a policy string (e.g., \"top-N=10\") into a map representation
  (e.g., {:type :top-n, :n 10})."
  [policy-str]
  (cond
    (nil? policy-str)
    (parse-policy (:selection-policy config/defaults))

    (str/starts-with? policy-str "top-N=")
    (if-let [[_ n] (re-matches #"top-N=(\d+)" policy-str)]
      {:type :top-n, :policy-string policy-str, :n (Integer/parseInt n)}
      nil)

    (str/starts-with? policy-str "tournament-k=")
    (if-let [[_ k] (re-matches #"tournament-k=(\d+)" policy-str)]
      {:type :tournament, :policy-string policy-str, :k (Integer/parseInt k)}
      nil)

    :else nil))

(defmulti apply-selection-policy
  "Selects survivor data from a report based on a parsed policy map.
  Dispatches on the :type key of the policy map."
  (fn [_report-data policy] (:type policy)))

(defmethod apply-selection-policy :top-n
  [report-data {:keys [n]}]
  (->> report-data
       (sort-by :score >)
       (take n)))

(defmethod apply-selection-policy :tournament
  [report-data {:keys [k]}]
  (if (empty? report-data)
    []
    (let [population-vec (vec report-data)
          population-size (count population-vec)]
      (log/info "Running" population-size "tournaments with k=" k)
      (for [_ (range population-size)]
        (->> (repeatedly k #(rand-nth population-vec))
             (apply max-key :score))))))

(defmethod apply-selection-policy :default
  [_report-data policy]
  (log/warn "Unknown or unparseable selection policy:" (:policy-string policy)
            "or type:" (:type policy) ". No selection performed.")
  [])


;; --- Core Command Logic ---

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
  (let [contest-dir (expdir/get-contest-dir ctx generation-number from-contest)]
    (if-not (.exists contest-dir)
      {:valid? false :reason (str "Contest directory not found at: " (.getCanonicalPath contest-dir))}
      {:valid? true})))

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
            {:keys [valid? reason]} (validate-options ctx opts)]
        (if-not valid?
          (log/error "Select validation failed:" reason)
          (let [report-data     (results/parse-report ctx gen-num from-contest)
                parsed-policy   (or (parse-policy policy-str) {:type :default, :policy-string policy-str})
                survivors-data  (apply-selection-policy report-data parsed-policy)
                survivor-ids    (map :prompt survivors-data)
                selection-event {:contest-name from-contest
                                 :policy       policy-str
                                 :select-run   (.toString (Instant/now))}
                _               (log/info "Selected" (count survivor-ids) "survivors from generation" gen-num "using policy '" policy-str "'")
                survivor-records (doall (map #(pdb/read-prompt (expdir/get-pdb-dir ctx) %) survivor-ids))]

            (when (empty? survivor-records)
              (log/warn "Selection resulted in zero survivors. The next generation will be empty."))

            (doseq [survivor survivor-records]
              (update-survivor-metadata! ctx (get-in survivor [:header :id]) selection-event))

            (when (seq survivor-records)
              (let [gen-info (pop/create-new-generation! ctx survivor-records)]
                (log/info "Created new generation" (:generation-number gen-info) "with" (:population-size gen-info) "survivors.")))))))))
