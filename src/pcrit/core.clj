(ns pcrit.core
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [pcrit.llm-interface :as llm]
            [pcrit.log :as log]))

(def cli-options
  [["-d" "--dry-run" "For 'experiment', print trial details without executing"]
   ["-h" "--help" "Print this help message"]])

(defn- usage [options-summary]
  (->> ["PromptCritical: A Prompt Evolution Experimentation Framework"
        ""
        "Usage: pcrit <command> <dir> [options]"
        ""
        "Commands:"
        "  help"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

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
        (log/info (usage summary))
        ))

    (finally
      (shutdown-agents)
      #_(log/shutdown-logging!))))
