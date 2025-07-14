(ns pcrit.failter.core
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.expdir.interface :as expdir]
            [pcrit.log.interface :as log]))

(defn- ensure-trailing-slash
  "Ensures a path string ends with the platform-specific file separator."
  [path-str]
  (if (str/ends-with? path-str java.io.File/separator)
    path-str
    (str path-str java.io.File/separator)))

(defn- write-spec-file!
  "Constructs and writes the spec.yml file for a Failter run."
  [ctx {:keys [generation-number contest-name inputs-dir population models judge-model]}]
  (let [contest-dir (expdir/get-contest-dir ctx generation-number contest-name)
        spec-file   (io/file contest-dir "spec.yml")
        artifacts-dir-path (-> (expdir/get-failter-artifacts-dir ctx generation-number contest-name)
                               .getCanonicalPath
                               ensure-trailing-slash)
        spec-data   {:version 2
                     :inputs_dir    (.getCanonicalPath (io/file inputs-dir))
                     :templates_dir (.getCanonicalPath (expdir/get-pdb-dir ctx))
                     :templates     (mapv #(str (get-in % [:header :id]) ".prompt") population)
                     :models        models
                     :judge_model   judge-model
                     :artifacts_dir artifacts-dir-path
                     :retries       2
                     :output_file   (.getCanonicalPath (io/file contest-dir "failter-report.json"))}]
    (.mkdirs contest-dir)
    (spit spec-file (yaml/generate-string spec-data :dumper-options {:flow-style :block}))
    (log/info "Wrote Failter spec file to" (.getCanonicalPath spec-file))
    spec-file))

(defn run-contest!
  "Prepares a failter spec.yml, runs the `failter run` command, and captures the results.
  Returns a map with `:success true` and the raw `:json-report` string from stdout,
  or `:success false` and an `:error` message."
  [ctx contest-params]
  (let [gen-num      (:generation-number contest-params)
        contest-name (:contest-name contest-params)]
    (log/info "Setting up failter contest" contest-name "for generation" gen-num)

    (let [spec-file (write-spec-file! ctx contest-params)
          spec-path (.getCanonicalPath spec-file)
          command   ["failter" "run" "--spec" spec-path]]

      (log/info "Running failter toolchain:" (str/join " " command))

      (let [result (apply shell/sh command)]
        (if (zero? (:exit result))
          (do
            (log/info "Failter run completed successfully.")
            (log/debug "Failter logs (stderr):\n" (:err result))
            {:success true :json-report (:out result)})
          (do
            (log/error "Failter command failed with exit code" (:exit result))
            (log/error "Failter logs (stderr):\n" (:err result))
            (log/error "Failter output (stdout):\n" (:out result))
            {:success false :error (:err result)}))))))
