(ns pcrit.cli.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.log.interface :as log]
            [pcrit.config.interface :as config]
            [taoensso.telemere])) ; For setting log level

(def global-cli-options
  [["-h" "--help" "Print this help message"]
   ["-l" "--log LEVEL" "Set the logging level (e.g., info, debug, warn)"
    :default :info
    :parse-fn #(keyword (str/lower-case %))]
   [nil "--pcrit-user-cwd CWD" "The user's original working directory (set by wrapper script)."]])

;; --- Path Resolution Helper ---
(defn- resolve-exp-dir
  "Resolves the user-provided experiment directory path against the original
  working directory to produce a canonical, absolute path."
  [user-dir-str user-cwd]
  (let [user-dir-file (io/file user-dir-str)]
    (if (.isAbsolute user-dir-file)
      (.getCanonicalPath user-dir-file)
      (.getCanonicalPath (io/file user-cwd user-dir-str)))))

;; --- Command Handlers ---
(defn- do-init [{:keys [options arguments]}]
  (let [user-cwd (:pcrit-user-cwd options)]
    (if (empty? arguments)
      (log/error "The 'init' command requires an <experiment-dir> argument.")
      (let [resolved-dir (resolve-exp-dir (first arguments) user-cwd)
            ctx (exp/new-experiment-context resolved-dir)
            {:keys [exit-code]} (cmd/init! ctx options)]
        (when (and exit-code (not (zero? exit-code)))
          (System/exit exit-code))))))

(defn- do-bootstrap [{:keys [options arguments]}]
  (let [user-cwd (:pcrit-user-cwd options)]
    (if (empty? arguments)
      (log/error "The 'bootstrap' command requires an <experiment-dir> argument.")
      (let [resolved-dir (resolve-exp-dir (first arguments) user-cwd)
            ctx (exp/new-experiment-context resolved-dir)]
        (cmd/bootstrap! ctx)))))

(defn- do-vary [{:keys [options arguments]}]
  (let [user-cwd (:pcrit-user-cwd options)]
    (if (empty? arguments)
      (log/error "The 'vary' command requires an <experiment-dir> argument.")
      (let [resolved-dir (resolve-exp-dir (first arguments) user-cwd)
            ctx (exp/new-experiment-context resolved-dir)]
        (cmd/vary! ctx)))))

(defn- do-evaluate [{:keys [options arguments]}]
  (let [user-cwd (:pcrit-user-cwd options)]
    (if (empty? arguments)
      (log/error "The 'evaluate' command requires an <experiment-dir> argument.")
      (if-not (:inputs options)
        (log/error "The 'evaluate' command requires an --inputs <directory> option.")
        (let [resolved-dir (resolve-exp-dir (first arguments) user-cwd)
              ctx (exp/new-experiment-context resolved-dir)
              resolved-opts (if (:inputs options)
                              (assoc options :inputs (resolve-exp-dir (:inputs options) user-cwd))
                              options)]
          (cmd/evaluate! ctx resolved-opts))))))

(defn- do-select [{:keys [options arguments]}]
  (let [user-cwd (:pcrit-user-cwd options)]
    (if (empty? arguments)
      (log/error "The 'select' command requires an <experiment-dir> argument.")
      (if-not (:from-contest options)
        (log/error "The 'select' command requires a --from-contest <name> option.")
        (let [resolved-dir (resolve-exp-dir (first arguments) user-cwd)
              ctx (exp/new-experiment-context resolved-dir)]
          (cmd/select! ctx options))))))

(defn- do-stats [{:keys [options arguments]}]
  (let [user-cwd (:pcrit-user-cwd options)]
    (if (empty? arguments)
      (log/error "The 'stats' command requires an <experiment-dir> argument.")
      (let [resolved-dir (resolve-exp-dir (first arguments) user-cwd)
            ctx (exp/new-experiment-context resolved-dir)]
        (cmd/stats! ctx options)))))

(defn- do-evolve [{:keys [options arguments]}]
  (let [user-cwd (:pcrit-user-cwd options)]
    (if (empty? arguments)
      (log/error "The 'evolve' command requires an <experiment-dir> argument.")
      (if-not (:inputs options)
        (log/error "The 'evolve' command requires an --inputs <directory> option.")
        (let [resolved-dir (resolve-exp-dir (first arguments) user-cwd)
              ctx (exp/new-experiment-context resolved-dir)
              resolved-opts (if (:inputs options)
                              (assoc options :inputs (resolve-exp-dir (:inputs options) user-cwd))
                              options)]
          (cmd/evolve! ctx resolved-opts))))))

(def command-specs
  {"init"      {:doc "Creates a new, minimal experiment skeleton directory."
                :handler do-init
                :options [["-f" "--force" "Overwrite existing scaffold files."]]}
   "bootstrap" {:doc "Initializes an experiment, ingests seeds, and creates gen-0."
                :handler do-bootstrap
                :options []}
   "vary"      {:doc "Applies meta-prompts to the latest generation."
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
                           :parse-fn #(Integer/parseInt %)]]}
   "evolve"    {:doc "Automates the vary->evaluate->select loop for N generations."
                :handler do-evolve
                :options [
                          ["-g" "--generations N" "The total number of generations to run for."
                           :default 1
                           :parse-fn #(Integer/parseInt %)]
                          ["-m" "--max-cost N" "A maximum cost in USD to halt evolution if exceeded."
                           :parse-fn #(Double/parseDouble %)]
                          ["-i" "--inputs DIR" "Directory of input files to use for all evaluations."]]}})

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
  (let [{global-opts :options, [command & params] :arguments, :as first-pass}
        (cli/parse-opts args global-cli-options :in-order true)]
    (if-let [errors (:errors first-pass)]
      (exit-fn 1 (out-fn (error-msg errors)))
      (cond
        (:help global-opts) (exit-fn 0 (out-fn (usage (:summary first-pass))))
        (nil? command) (exit-fn 1 (out-fn (usage (:summary first-pass))))
        :else
        (if-let [spec (get command-specs command)]
          (let [all-valid-options (into (:options spec) global-cli-options)
                {command-opts :options, final-args :arguments, :as second-pass}
                (cli/parse-opts params all-valid-options)]
            (if-let [errors (:errors second-pass)]
              (exit-fn 1 (out-fn (error-msg errors)))
              (let [final-opts (merge global-opts command-opts)]
                (taoensso.telemere/set-min-level! (:log final-opts))
                (cond
                  (:help final-opts)
                  (exit-fn 0 (out-fn (command-usage command spec (:summary second-pass))))

                  :else
                  ((:handler spec) {:options   final-opts
                                    :arguments final-args})))))
          (exit-fn 1 (out-fn (str "Unknown command: " command "\n" (usage (:summary first-pass))))))))))

(defn -main [& args]
  (try
    (cmd/setup!)
    (process-cli-args
     args
     {:exit-fn (fn [code _] (System/exit code))
      :out-fn  println})
    (finally
      (shutdown-agents))))
