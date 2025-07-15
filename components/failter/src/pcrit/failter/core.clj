(ns pcrit.failter.core
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]
            [clojure.data.json :as json]))

(defn- ensure-trailing-slash
  "Ensures a path string ends with the platform-specific file separator."
  [path-str]
  (if (str/ends-with? path-str java.io.File/separator)
    path-str
    (str path-str java.io.File/separator)))

(defn- write-spec-file!
  "Constructs and writes the spec.yml file for a Failter run.
  Returns the path to the report file that failter will generate."
  [ctx {:keys [generation-number contest-name inputs-dir population models judge-model]}]
  (let [contest-dir (expdir/get-contest-dir ctx generation-number contest-name)
        spec-file   (io/file contest-dir "spec.yml")
        report-file (io/file contest-dir "failter-report.json")
        artifacts-dir-path (-> (expdir/get-failter-artifacts-dir ctx generation-number contest-name)
                               .getCanonicalPath
                               ensure-trailing-slash)
        spec-data   (cond-> {:version 2
                             :inputs_dir    (.getCanonicalPath (io/file inputs-dir))
                             :templates_dir (.getCanonicalPath (expdir/get-pdb-dir ctx))
                             :templates     (mapv #(str (get-in % [:header :id]) ".prompt") population)
                             :models        models
                             :artifacts_dir artifacts-dir-path
                             :retries       2
                             :output_file   (.getCanonicalPath report-file)}
                      (not (str/blank? judge-model)) (assoc :judge_model judge-model))]
    (.mkdirs contest-dir)
    (spit spec-file (yaml/generate-string spec-data :dumper-options {:flow-style :block}))
    (log/info "Wrote Failter spec file to" (.getCanonicalPath spec-file))
    report-file))

(defn run-contest!
  "Prepares a failter spec.yml, runs the `failter run` command, captures logs, and parses the JSON report file.
  Returns a map with `:success true` and the parsed `:parsed-json` data,
  or `:success false` and an `:error` message."
  [ctx contest-params]
  (let [gen-num      (:generation-number contest-params)
        contest-name (:contest-name contest-params)
        contest-dir  (expdir/get-contest-dir ctx gen-num contest-name)]
    (log/info "Setting up failter contest" contest-name "for generation" gen-num)

    (let [report-file (write-spec-file! ctx contest-params)
          spec-path   (.getCanonicalPath (.getParentFile report-file))
          command     ["failter" "run" "--spec" (str spec-path "/spec.yml")]]

      (log/info "Running failter toolchain:" (str/join " " command))

      (let [result (apply shell/sh command)]
        (spit (io/file contest-dir "failter.stdout.log") (:out result))
        (spit (io/file contest-dir "failter.stderr.log") (:err result))

        (if (zero? (:exit result))
          (do
            (log/info "Failter run completed successfully (exit 0). Reading report file...")
            (if (.exists report-file)
              (try
                (let [parsed-json (json/read-str (slurp report-file) :key-fn keyword)]
                  {:success true :parsed-json parsed-json})
                (catch Exception e
                  (log/error "Failter report file was not valid JSON. Error:" (.getMessage e))
                  (log/error "Report file location:" (.getCanonicalPath report-file))
                  {:success false :error "Failter report file was not valid JSON."}))
              (do
                (log/error "Failter run succeeded but the specified report file was not created.")
                (log/error "Report file location:" (.getCanonicalPath report-file))
                {:success false :error "Failter report file not found."})))
          (do
            (log/error "Failter command failed with exit code" (:exit result))
            ;; NEW: Print stderr output on failure for immediate user feedback.
            (when-not (str/blank? (:err result))
              (log/error "Failter stderr output:\n" (:err result)))
            {:success false :error (:err result)}))))))
