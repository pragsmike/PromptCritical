(ns pcrit.core
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [pcrit.llm-interface :as llm]
            [pcrit.log :as log]
            [pcrit.pdb.core :as pdb]))

(def cli-options
  [["-h" "--help" "Print this help message"]])

(defn- usage [options-summary]
  (->> ["PromptCritical: A Prompt Evolution Experimentation Framework"
        ""
        "Usage: pcrit <command> [options]"
        ""
        "Commands:"
        "  help                      Show this help message."
        "  create <db-dir>           Create a new prompt in the database specified by <db-dir>."
        "                            The prompt text is read from standard input."
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- do-create-prompt [args]
  (if (empty? args)
    (log/error "The 'create' command requires a <db-dir> argument.")
    (let [db-dir (first args)
          prompt-text (slurp *in*)]
      (if (str/blank? prompt-text)
        (log/error "Cannot create a prompt with empty text from standard input.")
        (let [new-prompt (pdb/create-prompt db-dir prompt-text)]
          (log/info "Successfully created prompt" (get-in new-prompt [:header :id]))
          (println (get-in new-prompt [:header :id])))))))

(defn -main [& args]
  (try
    (log/setup-logging!)
    (llm/pre-flight-checks)

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
          "help"   (log/info (usage summary))
          "create" (do-create-prompt params)
          (log/error "Unknown command:" command "\n" (usage summary)))))

    (finally
      (shutdown-agents))))
