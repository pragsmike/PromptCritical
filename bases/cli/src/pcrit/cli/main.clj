(ns pcrit.cli.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.log.interface :as log]))

(def cli-options
  [["-h" "--help" "Print this help message"]])

(defn- usage [options-summary]
  (->> ["PromptCritical: A Prompt Evolution Experimentation Framework"
        ""
        "Usage: pcrit <command> [options] <args...>"
        ""
        "Commands:"
        "  help                         Show this help message."
        "  bootstrap <experiment-dir>   Initializes a new experiment directory."
        ""
        "Options:"
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

(defn -main [& args]
  (try
    (cmd/init!)

    (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
      (cond
        (:help options)
        (do (log/info (usage summary)) (System/exit 0))

        errors
        (do (log/error (error-msg errors)) (System/exit 1))

        (< (count arguments) 1)
        (do (log/info (usage summary)) (System/exit 1)))

      (let [[command & params] arguments]
        (case command
          "help"      (log/info (usage summary))
          "bootstrap" (do-bootstrap params)
          ;; The old "create" command is removed for clarity as it's not part of the main workflow
          (log/error "Unknown command:" command "\n" (usage summary)))))

    (finally
      (shutdown-agents))))
