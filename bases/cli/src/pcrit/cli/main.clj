(ns pcrit.cli.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.log.interface :as log])
  (:gen-class))

(def global-cli-options
  [["-h" "--help" "Print this help message"]])

(defn- usage [options-summary]
  (->> ["PromptCritical: A Prompt Evolution Experimentation Framework"
        ""
        "Usage: pcrit <command> [options] <args...>"
        ""
        "Commands:"
        "  help                         Show this help message."
        "  bootstrap <experiment-dir>   Initializes a new experiment directory."
        "  vary <experiment-dir>        Creates a new generation of prompts."
        ""
        "Global Options:"
        options-summary]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- do-bootstrap [args]
  (if (empty? args)
    (log/error "The 'bootstrap' command requires an <experiment-dir> argument.")
    (let [exp-dir (first args)
          ctx (exp/new-experiment-context exp-dir)]
      (log/info "Bootstrapping experiment in:" exp-dir)
      (cmd/bootstrap! ctx)
      (log/info "Bootstrap complete."))))

(defn- do-vary [args]
  (if (empty? args)
    (log/error "The 'vary' command requires an <experiment-dir> argument.")
    (let [exp-dir (first args)
          ctx (exp/new-experiment-context exp-dir)]
      (log/info "Varying population in experiment:" exp-dir)
      (cmd/vary! ctx)
      (log/info "Vary complete."))))

(defn process-cli-args
  "Parses command-line arguments and dispatches to the correct command."
  [args {:keys [exit-fn out-fn]}]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args global-cli-options)]
    (cond
      (:help options)
      (exit-fn 0 (out-fn (usage summary)))

      errors
      (exit-fn 1 (out-fn (error-msg errors)))

      (empty? arguments)
      (exit-fn 1 (out-fn (usage summary)))

      :else
      (let [[command & params] arguments]
        (if (str/starts-with? command "-")
          ;; CORRECTED: Use pr-str to format the error message consistently.
          (exit-fn 1 (out-fn (error-msg [(str "Unknown option: " (pr-str command))])))
          (case command
            "help"      (exit-fn 0 (out-fn (usage summary)))
            "bootstrap" (do-bootstrap params)
            "vary"      (do-vary params)
            (exit-fn 1 (out-fn (str "Unknown command: " command "\n" (usage summary))))))))))

(defn -main [& args]
  (try
    (cmd/init!)
    (process-cli-args
     args
     {:exit-fn (fn [code _] (System/exit code))
      :out-fn  println})
    (finally
      (shutdown-agents))))
