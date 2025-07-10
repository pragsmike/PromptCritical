(ns pcrit.failter.core
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log])
  (:import [java.nio.file Files StandardCopyOption]))

(defn- run-shell-command! [failter-spec-dir & args]
  ;; CORRECTED: Build a single sequence of all arguments before calling apply.
  (let [all-args (concat ["failter"] args [:dir (.getCanonicalPath failter-spec-dir)])
        result   (apply shell/sh all-args)]
    (when-not (zero? (:exit result))
      (let [error-msg (str "Failter command failed: " (str/join " " args))]
        (log/error error-msg "\n" (:err result))
        (throw (ex-info error-msg {:result result}))))
    (log/info (:out result))))

(defn- create-input-symlinks! [inputs-dir failter-inputs-dir]
  (let [source-dir (io/file inputs-dir)]
    (if-not (and (.exists source-dir) (.isDirectory source-dir))
      (throw (ex-info (str "Inputs directory not found or not a directory: " inputs-dir) {:path inputs-dir}))
      (doseq [f (.listFiles source-dir)]
        (let [link-file (io/file failter-inputs-dir (.getName f))]
          (Files/createSymbolicLink (.toPath link-file) (.toPath f) (make-array java.nio.file.attribute.FileAttribute 0)))))))

(defn- create-template-symlinks! [population failter-templates-dir pdb-dir]
  (doseq [prompt-record population]
    (let [target-file (expdir/pdb-file-of-prompt-record {:exp-dir (.getParent pdb-dir)} prompt-record)
          link-name   (str (get-in prompt-record [:header :id]) ".prompt")
          link-file   (io/file failter-templates-dir link-name)]
      (expdir/create-relative-symlink! link-file target-file))))

(defn- write-contest-metadata! [contest-dir {:keys [generation-number contest-name inputs-dir population]}]
  (let [metadata {:timestamp           (str (java.time.Instant/now))
                  :generation-number   generation-number
                  :contest-name        contest-name
                  :inputs-path         (.getCanonicalPath (io/file inputs-dir))
                  :participants        (map #(get-in % [:header :id]) population)}
        metadata-file (io/file contest-dir "contest-metadata.edn")]
    (spit metadata-file (pr-str metadata))))

(defn run-contest!
  "Prepares a failter-spec directory, runs the full failter toolchain, and
  captures the results."
  [ctx {:keys [generation-number contest-name inputs-dir population]}]
  (let [contest-dir        (expdir/get-contest-dir ctx generation-number contest-name)
        failter-spec-dir   (expdir/get-failter-spec-dir ctx generation-number contest-name)
        failter-inputs-dir (io/file failter-spec-dir "inputs")
        failter-templates-dir (io/file failter-spec-dir "templates")]

    (log/info "Setting up failter contest" contest-name "for generation" generation-number)
    (.mkdirs failter-inputs-dir)
    (.mkdirs failter-templates-dir)

    (create-input-symlinks! inputs-dir failter-inputs-dir)
    (create-template-symlinks! population failter-templates-dir (expdir/get-pdb-dir ctx))

    (log/info "Running failter toolchain in" (.getCanonicalPath failter-spec-dir))
    (run-shell-command! failter-spec-dir "experiment")
    (run-shell-command! failter-spec-dir "evaluate")
    (run-shell-command! failter-spec-dir "report")

    (let [source-report (io/file failter-spec-dir "report.csv")
          dest-report   (io/file contest-dir "report.csv")]
      (log/info "Capturing report.csv to" (.getCanonicalPath contest-dir))
      (Files/move (.toPath source-report) (.toPath dest-report) (into-array java.nio.file.CopyOption [StandardCopyOption/REPLACE_EXISTING])))

    (write-contest-metadata! contest-dir {:generation-number generation-number
                                          :contest-name      contest-name
                                          :inputs-dir        inputs-dir
                                          :population        population})
    {:success true :report-path (.getCanonicalPath (io/file contest-dir "report.csv"))}))
