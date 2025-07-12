(ns pcrit.command.init
  (:require [clojure.java.io :as io]
            [pcrit.log.interface :as log]))

;; --- Private Helpers ---

(def ^:private scaffold-files
  {"pcrit/init_scaffold/.gitignore"               ".gitignore"
   "pcrit/init_scaffold/bootstrap.edn"            "bootstrap.edn"
   "pcrit/init_scaffold/evolution-parameters.edn" "evolution-parameters.edn"
   "pcrit/init_scaffold/seeds/improve-meta-prompt.txt" "seeds/improve-meta-prompt.txt"
   "pcrit/init_scaffold/seeds/seed-object-prompt.txt"  "seeds/seed-object-prompt.txt"})

(defn- copy-resource! [resource-path target-file]
  (io/make-parents target-file)
  (with-open [in (io/input-stream (io/resource resource-path))
              out (io/output-stream target-file)]
    (io/copy in out)))

(defn- directory-is-empty? [^java.io.File dir]
  (let [user-visible-files (some->> (.list dir)
                                    (remove #{"." ".."}))]
    (empty? user-visible-files)))

(defn- pre-flight-checks! [target-dir force?]
  (cond
    (and (.exists target-dir) (not (directory-is-empty? target-dir)) (not force?))
    (throw (ex-info (str "Directory " (.getCanonicalPath target-dir)
                         " exists and is not empty. Use --force to overwrite.")
                    {:type :validation-error}))
    :else
    (do (.mkdirs target-dir) true)))

;; --- Public Command Function ---

(defn init!
  "The core implementation of the init command.
  Creates a scaffold experiment in the given directory."
  [ctx {:keys [force?]}]
  (try
    (let [target-dir (io/file (:exp-dir ctx))]
      (when (pre-flight-checks! target-dir force?)
        (.mkdirs (io/file target-dir "seeds"))
        (doseq [[resource-path target-path] scaffold-files]
          (copy-resource! resource-path (io/file target-dir target-path)))
        (log/info "✓ Experiment skeleton created at " (.getCanonicalPath target-dir))
        {:exit-code 0}))
    (catch clojure.lang.ExceptionInfo e
      (log/error (.getMessage e))
      {:exit-code 1})
    (catch Exception e
      (log/error "An unexpected I/O error occurred: " (.getMessage e))
      {:exit-code 2})))
