(ns pcrit.command.evolve
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.command.vary :as vary]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.command.select :as select]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]
            [pcrit.pdb.interface :as pdb]
            [pcrit.pop.interface :as pop]
            [pcrit.reports.interface :as reports]))

(defn- get-vary-cost [ctx generation-number original-pop-ids]
  (let [new-pop (pop/load-population ctx generation-number)
        offspring (remove #(contains? original-pop-ids (get-in % [:header :id])) new-pop)]
    (->> offspring
         (map #(get-in % [:header :cost-usd-snapshot] 0.0))
         (reduce + 0.0))))

(defn- get-evaluate-cost [ctx generation-number contest-name]
  (let [report-file (io/file (expdir/get-contest-dir ctx generation-number contest-name) "report.csv")
        report-data (reports/parse-report report-file)]
    (->> report-data
         (map :cost)
         (remove nil?)
         (reduce + 0.0))))

(defn evolve!
  "Automates the evolutionary loop for a specified number of generations or until a cost budget is met."
  [ctx {:keys [generations max-cost inputs]}]
  (log/info "Starting evolution run...")
  (loop [current-gen (or (expdir/find-latest-generation-number ctx) 0)
         cumulative-cost 0.0]
    (let [run-id (str "evo-" (System/currentTimeMillis))]
      (log/info (format "\n--- Starting Generation %d (Run ID: %s) (Cumulative Cost: $%.4f) ---" current-gen run-id cumulative-cost))

      ;; 1. VARY
      (log/info "Step 1: Varying population...")
      (let [original-pop (pop/load-population ctx current-gen)
            original-pop-ids (set (map #(get-in % [:header :id]) original-pop))]
        (vary/vary! ctx)
        (let [vary-cost (get-vary-cost ctx current-gen original-pop-ids)]
          (log/info (format "Vary step cost: $%.4f" vary-cost))
          (let [new-cumulative-cost (+ cumulative-cost vary-cost)]
            (if (and max-cost (> new-cumulative-cost max-cost))
              (log/error (format "Halting evolution: Max cost of $%.2f exceeded (cost after vary: $%.4f)." max-cost new-cumulative-cost))
              (let [contest-name (str run-id "-gen-" current-gen)]
                ;; 2. EVALUATE
                (log/info "Step 2: Evaluating population...")
                (evaluate/evaluate! ctx {:generation current-gen, :name contest-name, :inputs inputs})
                (let [eval-cost (get-evaluate-cost ctx current-gen contest-name)
                      final-cumulative-cost (+ new-cumulative-cost eval-cost)]
                  (log/info (format "Evaluate step cost: $%.4f" eval-cost))

                  (if (and max-cost (> final-cumulative-cost max-cost))
                    (log/error (format "Halting evolution: Max cost of $%.2f exceeded (cost after evaluate: $%.4f)." max-cost final-cumulative-cost))
                    (do
                      ;; 3. SELECT
                      (log/info "Step 3: Selecting survivors...")
                      (select/select! ctx {:generation current-gen, :from-contest contest-name})
                      (let [next-gen (expdir/find-latest-generation-number ctx)]
                        (if (or (nil? next-gen) (<= next-gen current-gen))
                          (log/error "Halting evolution: Selection failed to produce a new generation.")
                          (if (>= next-gen (dec generations))
                            (log/info (format "Evolution run complete after %d generation(s)." generations))
                            (recur next-gen final-cumulative-cost)))))))))))))))
