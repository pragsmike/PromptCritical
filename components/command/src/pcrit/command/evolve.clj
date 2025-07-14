(ns pcrit.command.evolve
  (:require [pcrit.command.vary :as vary]
            [pcrit.command.evaluate :as eval]
            [pcrit.command.select :as sel]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]))

(defn evolve!
  "Automates the evolutionary loop for a specified number of generations or until a cost budget is met."
  [ctx {:keys [generations max-cost inputs]}]
  (log/info "Starting evolution run...")
  (loop [current-gen (or (expdir/find-latest-generation-number ctx) 0)
         cumulative-cost 0.0]

    (if (>= current-gen generations)
      (log/info (format "Evolution run complete: Reached target of %d generation(s)." generations))
      (let [run-id (str "evo-" (System/currentTimeMillis))]
        (log/info (format "\n--- Starting Generation %d (Run ID: %s) (Cumulative Cost: $%.4f) ---" current-gen run-id cumulative-cost))

        ;; 1. VARY
        (log/info "Step 1: Varying population...")
        (let [{vary-cost :cost} (vary/vary! ctx)
              cost-after-vary (+ cumulative-cost vary-cost)]
          (log/info (format "Vary step cost: $%.4f" vary-cost))

          (if (and max-cost (> cost-after-vary max-cost))
            (log/error (format "Halting evolution: Max cost of $%.2f exceeded (cost after vary: $%.4f)." max-cost cost-after-vary))
            (let [contest-name (str run-id "-gen-" current-gen)]

              ;; 2. EVALUATE
              (log/info "Step 2: Evaluating population...")
              (let [{:keys [success cost]} (eval/evaluate! ctx {:generation current-gen, :name contest-name, :inputs inputs})]
                (if-not success
                  (log/error "Halting evolution: Evaluate step failed.")
                  (let [cost-after-eval (+ cost-after-vary cost)]
                    (if (and max-cost (> cost-after-eval max-cost))
                      (log/error (format "Halting evolution: Max cost of $%.2f exceeded (cost after evaluate: $%.4f)." max-cost cost-after-eval))

                      (do
                        ;; 3. SELECT
                        (log/info "Step 3: Selecting survivors...")
                        (sel/select! ctx {:generation current-gen, :from-contest contest-name})

                        (let [next-gen (expdir/find-latest-generation-number ctx)]
                          (if (or (nil? next-gen) (<= next-gen current-gen))
                            (log/error "Halting evolution: Selection failed to produce a new generation.")
                            (recur next-gen cost-after-eval)))))))))))))))
