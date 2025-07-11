(ns pcrit.command.stats
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]
            [pcrit.reports.interface :as reports]))

(defn- find-report-files [ctx {:keys [generation from-contest]}]
  (let [latest-gen (expdir/find-latest-generation-number ctx)
        gen-num    (or generation latest-gen)]
    (if-not gen-num
      (do (log/error "Cannot get stats: No generations found.")
          [])
      (if from-contest
        ;; Case 1: A specific contest is named
        (let [report-file (io/file (expdir/get-contest-dir ctx gen-num from-contest) "report.csv")]
          (if (.exists report-file)
            [report-file]
            (do (log/error "Report not found for contest:" from-contest "in generation" gen-num)
                [])))
        ;; Case 2: An entire generation is specified
        (let [contests-dir (expdir/get-contests-dir ctx gen-num)]
          (if (.exists contests-dir)
            (->> (file-seq contests-dir)
                 (filter #(= "report.csv" (.getName %)))
                 (doall))
            (do (log/error "No contests found for generation" gen-num)
                [])))))))

(defn- calculate-stats [report-data]
  (when (seq report-data)
    (let [costs (map :cost report-data)
          scores (map :score report-data)
          best-prompt (apply max-key :score report-data)
          worst-prompt (apply min-key :score report-data)]
      {:prompt-count   (count report-data)
       :total-cost     (reduce + 0.0 costs)
       :avg-cost       (/ (reduce + 0.0 costs) (count costs))
       :total-score    (reduce + 0.0 scores)
       :avg-score      (/ (reduce + 0.0 scores) (count scores))
       :highest-score  (:score best-prompt)
       :best-prompt-id (:prompt best-prompt)
       :lowest-score   (:score worst-prompt)
       :worst-prompt-id (:prompt worst-prompt)})))

(defn- print-stats [title stats]
  (println)
  (println title)
  (println (str/join (repeat (count title) "-")))
  (if stats
    (println (str/join "\n"
                       [(format "Prompts evaluated:   %d" (:prompt-count stats))
                        (format "Total Cost:          $%.4f" (:total-cost stats))
                        (format "Average Cost:        $%.4f" (:avg-cost stats))
                        ""
                        (format "Highest Score:       %.3f (id: %s)" (:highest-score stats) (:best-prompt-id stats))
                        (format "Lowest Score:        %.3f (id: %s)" (:lowest-score stats) (:worst-prompt-id stats))
                        (format "Average Score:       %.3f" (:avg-score stats))]))
    (println "No data available to generate statistics."))
  (println))

(defn stats!
  "Calculates and displays statistics for a given contest or generation."
  [ctx options]
  (let [report-files (find-report-files ctx options)]
    (if (seq report-files)
      (let [all-data (mapcat reports/parse-report report-files)
            stats (calculate-stats all-data)
            title (if (:from-contest options)
                    (str "Stats for contest: " (:from-contest options))
                    (str "Aggregated Stats for Generation: " (or (:generation options) (expdir/find-latest-generation-number ctx))))]
        (print-stats title stats))
      (log/info "No reports found for the given criteria."))))
