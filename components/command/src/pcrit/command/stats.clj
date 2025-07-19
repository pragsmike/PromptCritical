(ns pcrit.command.stats
  (:require [clojure.string :as str]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]
            [pcrit.results.interface :as results]
            [pcrit.llm.costs :as llm-costs]))

(defn- find-contest-dirs [ctx {:keys [generation from-contest]}]
  (let [latest-gen (expdir/find-latest-generation-number ctx)
        gen-num    (or generation latest-gen)]
    (if-not gen-num
      (do (log/error "Cannot get stats: No generations found.")
          [])
      (if from-contest
        ;; Case 1: A specific contest is named
        (let [contest-dir (expdir/get-contest-dir ctx gen-num from-contest)]
          (when (.exists contest-dir)
            [contest-dir]))
        ;; Case 2: An entire generation is specified
        (let [contests-dir (expdir/get-contests-dir ctx gen-num)]
          (when (.exists contests-dir)
            (->> (.listFiles contests-dir)
                 (filter #(.isDirectory %))
                 (doall))))))))

(defn- calculate-stats [report-data]
  (when (seq report-data)
    (let [cost-augmented-data (map #(assoc % :cost (llm-costs/calculate-cost (:model %) (:tokens-in %) (:tokens-out %))) report-data)
          scored-data    (filter #(some? (:score %)) cost-augmented-data)
          costs          (keep :cost cost-augmented-data)
          scores         (keep :score scored-data)
          tokens-in      (keep :tokens-in cost-augmented-data)
          tokens-out     (keep :tokens-out cost-augmented-data)
          best-prompt    (apply max-key :score scored-data)
          worst-prompt   (apply min-key :score scored-data)]
      (cond-> {:prompt-count   (count cost-augmented-data)}
        (seq costs)        (assoc :total-cost (reduce + 0.0 costs)
                                  :avg-cost   (/ (reduce + 0.0 costs) (count costs)))
        (seq scores)       (assoc :highest-score  (:score best-prompt)
                                  :best-prompt-id (:prompt best-prompt)
                                  :lowest-score   (:score worst-prompt)
                                  :worst-prompt-id (:prompt worst-prompt)
                                  :avg-score      (/ (reduce + 0.0 scores) (count scores)))
        (seq tokens-in)    (assoc :avg-tokens-in  (/ (double (reduce + tokens-in)) (count tokens-in)))
        (seq tokens-out)   (assoc :avg-tokens-out (/ (double (reduce + tokens-out)) (count tokens-out)))))))

(defn- print-stats [title stats]
  (println)
  (println title)
  (println (str/join (repeat (count title) "-")))
  (if stats
    (println (str/join "\n"
                       (cond-> [(format "Prompts evaluated:   %d" (:prompt-count stats))]
                         (:total-cost stats)    (conj (format "Total Cost:          $%.4f" (:total-cost stats)))
                         (:avg-cost stats)      (conj (format "Average Cost:        $%.4f" (:avg-cost stats)))
                         true                   (conj "")
                         (:highest-score stats) (conj (format "Highest Score:       %.3f (id: %s)" (:highest-score stats) (:best-prompt-id stats)))
                         (:lowest-score stats)  (conj (format "Lowest Score:        %.3f (id: %s)" (:lowest-score stats) (:worst-prompt-id stats)))
                         (:avg-score stats)     (conj (format "Average Score:       %.3f" (:avg-score stats)))
                         true                   (conj "")
                         (:avg-tokens-in stats)  (conj (format "Avg Tokens In:       %.1f" (:avg-tokens-in stats)))
                         (:avg-tokens-out stats) (conj (format "Avg Tokens Out:      %.1f" (:avg-tokens-out stats))))))
    (println "No data available to generate statistics."))
  (println))

(defn stats!
  "Calculates and displays statistics for a given contest or generation."
  [ctx options]
  (let [contest-dirs (find-contest-dirs ctx options)]
    (if (seq contest-dirs)
      (let [gen-num (or (:generation options) (expdir/find-latest-generation-number ctx))
            all-data (mapcat #(results/parse-report ctx gen-num (.getName %)) contest-dirs)
            stats (calculate-stats all-data)
            title (if (:from-contest options)
                    (str "Stats for contest: " (:from-contest options))
                    (str "Aggregated Stats for Generation: " gen-num))]
        (print-stats title stats))
      (println "\nNo reports found for the given criteria.\n"))))
