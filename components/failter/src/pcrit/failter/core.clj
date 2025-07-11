(ns pcrit.failter.core
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]))

(defn- run-shell-command! [& args]
  (let [full-command (cons "failter" args)
        result (apply shell/sh full-command)]
    (when-not (zero? (:exit result))
      (let [error-msg (str "Failter command failed: " (str/join " " full-command))]
        (log/error error-msg "\n" (:err result))
        (throw (ex-info error-msg {:result result :command full-command}))))
    (log/info "Failter output:\n" (:out result))))

(defn run-contest!
  "Prepares a failter-spec directory via the expdir component, runs the full
  failter toolchain, and captures the results."
  [ctx {:keys [generation-number contest-name judge-model] :as contest-params}]
  (log/info "Setting up failter contest" contest-name "for generation" generation-number)

  ;; 1. Delegate all directory and symlink creation to expdir.
  (let [failter-spec-dir (expdir/prepare-contest-directory! ctx contest-params)
        spec-path        (.getCanonicalPath failter-spec-dir)]

    (log/info "Running failter toolchain on" spec-path)

    ;; 2. Run the external toolchain.
    (run-shell-command! "experiment" spec-path)

    (if judge-model
      (run-shell-command! "evaluate" spec-path "--judge-model" judge-model)
      (run-shell-command! "evaluate" spec-path))

    (run-shell-command! "report" spec-path)

    ;; 3. Delegate report capture to expdir.
    (if-let [report-file (expdir/capture-contest-report! ctx generation-number contest-name)]
      (do
        (log/info "Captured report.csv to" (.getCanonicalPath report-file))
        {:success true :report-path (.getCanonicalPath report-file)})
      (do
        (log/error "Failter contest ran, but report.csv was not found.")
        {:success false :report-path nil}))))
