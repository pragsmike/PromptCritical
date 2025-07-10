(ns pcrit.cli.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.log.interface :as log])
  (:gen-class))

(def global-cli-options
  [["-h" "--help" "Print this help message"]])

;; NEW: Define options common to all commands.
(def shared-command-options
  [["-h" "--help" "Show help for this command."]])

;; --- Command Handlers ---
(defn- do-bootstrap [{:keys [arguments]}]
  (if (empty? arguments)
    (log/error "The 'bootstrap' command requires an <experiment-dir> argument.")
    (let [exp-dir (first arguments)
          ctx (exp/new-experiment-context exp-dir)]
      (log/info "Bootstrapping experiment in:" exp-dir)
      (cmd/bootstrap! ctx)
      (log/info "Bootstrap complete."))))

(defn- do-vary [{:keys [arguments]}]
  (if (empty? arguments)
    (log/error "The 'vary' command requires an <experiment-dir> argument.")
    (let [exp-dir (first arguments)
          ctx (exp/new-experiment-context exp-dir)]
      (log/info "Varying population in experiment:" exp-dir)
      (cmd/vary! ctx)
      (log/info "Vary complete."))))

;; --- Command Specification Map ---
(def command-specs
  {"bootstrap" {:doc "Initializes an experiment, ingests seeds, and creates gen-0."
                :handler do-bootstrap
                :options []}
   "vary"      {:doc "Evolves the latest generation into a new one via mutation."
                :handler do-vary
                :options []}})

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
  "Parses command-line arguments and dispatches to the correct command."
  [args {:keys [exit-fn out-fn]}]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args global-cli-options :in-order true)]
    (cond
      (:help options)
      (exit-fn 0 (out-fn (usage summary)))

      (seq errors)
      (exit-fn 1 (out-fn (error-msg errors)))

      (empty? arguments)
      (exit-fn 1 (out-fn (usage summary)))

      :else
      (let [command (first arguments)
            params (rest arguments)]
        (if-let [spec (get command-specs command)]
          ;; CORRECTED: Merge shared options with command-specific ones before parsing.
          (let [all-cmd-options (into (:options spec) shared-command-options)
                {:keys [options arguments errors sub-summary]} (cli/parse-opts params all-cmd-options)]
            (cond
              (:help options)
              (exit-fn 0 (out-fn (command-usage command spec sub-summary)))

              (seq errors)
              (exit-fn 1 (out-fn (error-msg errors)))

              :else
              ((:handler spec) {:options options :arguments arguments})))
          (exit-fn 1 (out-fn (str "Unknown command: " command "\n" (usage summary)))))))))

(defn -main [& args]
  (try
    (cmd/init!)
    (process-cli-args
     args
     {:exit-fn (fn [code _] (System/exit code))
      :out-fn  println})
    (finally
      (shutdown-agents))))
