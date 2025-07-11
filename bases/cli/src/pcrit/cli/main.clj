(ns pcrit.cli.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.log.interface :as log]
            [pcrit.config.interface :as config]))

(def global-cli-options
  [["-h" "--help" "Print this help message"]])

(def shared-command-options
  [["-h" "--help" "Show help for this command."]])

;; --- Command Handlers ---

(defn- do-init [{:keys [options arguments]}]
  (if (empty? arguments)
    (log/error "The 'init' command requires an <experiment-dir> argument.")
    (let [exp-dir (first arguments)
          ctx (exp/new-experiment-context exp-dir)
          {:keys [exit-code]} (cmd/init! ctx options)]
      (when (and exit-code (not (zero? exit-code)))
        (System/exit exit-code)))))

(defn- do-bootstrap [{:keys [arguments]}]
  (if (empty? arguments)
    (log/error "The 'bootstrap' command requires an <experiment-dir> argument.")
    (let [exp-dir (first arguments)
          ctx (exp/new-experiment-context exp-dir)]
      (cmd/bootstrap! ctx))))

(defn- do-vary [{:keys [arguments]}]
  (if (empty? arguments)
    (log/error "The 'vary' command requires an <experiment-dir> argument.")
    (let [exp-dir (first arguments)
          ctx (exp/new-experiment-context exp-dir)]
      (cmd/vary! ctx))))

(defn- do-evaluate [{:keys [options arguments]}]
  (if (empty? arguments)
    (log/error "The 'evaluate' command requires an <experiment-dir> argument.")
    (if-not (:inputs options)
      (log/error "The 'evaluate' command requires an --inputs <directory> option.")
      (let [exp-dir (first arguments)
            ctx (exp/new-experiment-context exp-dir)]
        (cmd/evaluate! ctx options)))))

(defn- do-select [{:keys [options arguments]}]
  (if (empty? arguments)
    (log/error "The 'select' command requires an <experiment-dir> argument.")
    (if-not (:from-contest options)
      (log/error "The 'select' command requires a --from-contest <name> option.")
      (let [exp-dir (first arguments)
            ctx (exp/new-experiment-context exp-dir)]
        (cmd/select! ctx options)))))

(defn- do-stats [{:keys [options arguments]}]
  (if (empty? arguments)
    (log/error "The 'stats' command requires an <experiment-dir> argument.")
    (let [exp-dir (first arguments)
          ctx (exp/new-experiment-context exp-dir)]
      (cmd/stats! ctx options))))

;; --- Command Specification Map ---
(def command-specs
  {"init"      {:doc "Creates a new, minimal experiment skeleton directory."
                :handler do-init
                :options [["-f" "--force" "Overwrite existing scaffold files."]]}
   "bootstrap" {:doc "Initializes an experiment, ingests seeds, and creates gen-0."
                :handler do-bootstrap
                :options []}
   "vary"      {:doc "Evolves the latest generation into a new one via mutation."
                :handler do-vary
                :options []}
   "evaluate"  {:doc "Runs a contest on a population using the Failter tool."
                :handler do-evaluate
                :options [
                          ["-g" "--generation GEN" "Generation number to evaluate (defaults to latest)"
                           :parse-fn #(Integer/parseInt %)]
                          ["-n" "--name NAME" "A unique name for this contest (defaults to 'contest')"]
                          ["-i" "--inputs DIR" "Directory containing input files for the contest"]
                          ["-j" "--judge-model MODEL" "LLM model to use as the judge (overrides config)"]]}
   "select"    {:doc "Selects survivors from a contest to create the next generation."
                :handler do-select
                :options [
                          ["-c" "--from-contest NAME" "Name of the contest report to use for selection"]
                          ["-g" "--generation GEN" "Generation number where the contest resides (defaults to latest)"
                           :parse-fn #(Integer/parseInt %)]
                          ["-p" "--policy POLICY" "Selection policy (e.g., 'top-N=5')"
                           :default (:selection-policy config/defaults)]]}
   "stats"     {:doc "Displays cost and score statistics for a contest or generation."
                :handler do-stats
                :options [
                          ["-c" "--from-contest NAME" "Name of a specific contest to analyze"]
                          ["-g" "--generation GEN" "Generation to analyze (defaults to latest)"
                           :parse-fn #(Integer/parseInt %)]]}})

;; --- Usage and Parsing Logic ---
(defn- command-usage [command-name spec options-summary]
  (->> [(str "Usage: pcrit " command-name " [options] <args...>\n")
        "Description:"
        (str "  " (:doc spec))
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn- usage [global-summary]
  (str/join \newline
    (concat
      ["PromptCritical: A Prompt Evolution Experimentation Framework"
       ""
       "Usage: pcrit <command> [options] <args...>"
       ""
       "Commands:"]
      (for [[cmd spec] (sort-by key command-specs)]
        (format "  %-12s %s" cmd (:doc spec)))
      [""
       "Global Options:"
       global-summary
       ""
       "Run 'pcrit <command> --help' for more information on a specific command."])))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn process-cli-args
  [args {:keys [exit-fn out-fn]}]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args global-cli-options :in-order true)]
    (cond
      (:help options) (exit-fn 0 (out-fn (usage summary)))
      (seq errors)    (exit-fn 1 (out-fn (error-msg errors)))
      (empty? arguments) (exit-fn 1 (out-fn (usage summary)))
      :else
      (let [command (first arguments)
            params (rest arguments)]
        (if-let [spec (get command-specs command)]
          (let [all-cmd-options (into (:options spec) shared-command-options)
                {:keys [options arguments errors sub-summary]} (cli/parse-opts params all-cmd-options)]
            (cond
              (:help options) (exit-fn 0 (out-fn (command-usage command spec sub-summary)))
              (seq errors)    (exit-fn 1 (out-fn (error-msg errors)))
              :else           ((:handler spec) {:options options :arguments arguments})))
          (exit-fn 1 (out-fn (str "Unknown command: " command "\n" (usage summary)))))))))

(defn -main [& args]
  (try
    (cmd/setup!)
    (process-cli-args
     args
     {:exit-fn (fn [code _] (System/exit code))
      :out-fn  println})
    (finally
      (shutdown-agents))))
