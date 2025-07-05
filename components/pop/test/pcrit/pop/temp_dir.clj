(ns pcrit.pop.temp-dir
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]))

;; Tests can refer to the directory via this dynamic var.
(def ^:dynamic *tmp-dir* nil)

(defn- delete-recursively
  "Remove `file-or-dir` and everything beneath it, silencing any
  'file-not-found' errors that might appear during cleanup."
  [file-or-dir]
  (when (.exists file-or-dir)
    (doseq [f (reverse (file-seq file-or-dir))] ; children first
      (io/delete-file f true))))               ; `true` → silence errors

(defn with-temp-dir
  "A clojure.test :each fixture that binds *tmp-dir* and cleans up afterwards."
  [test-fn]
  (let [^Path tmp-path (Files/createTempDirectory "poly-test-" (make-array FileAttribute 0))
        tmp-dir        (.toFile tmp-path)]
    (try
      (binding [*tmp-dir* (.getAbsolutePath tmp-dir)]
        (test-fn))                             ; ← run the test
      (finally
        (delete-recursively tmp-dir)))))       ; ← always runs

(defn create-experiment-dirs!
  "Creates the standard subdirectories (pdb, generations, links, seeds)
  within a given experiment root directory. This operation is idempotent."
  [exp-root-dir]
  (doseq [subdir ["pdb" "generations" "links" "seeds"]]
    (.mkdirs (io/file exp-root-dir subdir))))

(defn make-temp-exp-dir!
  "Creates and populates a self-contained experiment directory for testing.

  - Creates the standard subdirectory layout (pdb, seeds, etc.).
  - Creates a 'seeds' subdirectory with sample raw prompt files.
  - Creates a 'bootstrap.edn' manifest in the root, pointing to the seed files.

  This makes the directory suitable for testing the bootstrap process."
  [exp-root-dir]
  (create-experiment-dirs! exp-root-dir)
  (let [seeds-dir (io/file exp-root-dir "seeds")]
    ;; Create raw prompt files in the 'seeds' directory
    (spit (io/file seeds-dir "seed-prompt.txt") "This is the initial object prompt. {{INPUT_TEXT}}")
    (spit (io/file seeds-dir "refine-prompt.txt") "Refine this prompt: {{OBJECT-PROMPT}}")
    (spit (io/file seeds-dir "vary-prompt.txt") "Vary this prompt: {{OBJECT-PROMPT}}")

    ;; Create the bootstrap.edn manifest in the experiment root
    (spit (io/file exp-root-dir "bootstrap.edn")
          (pr-str {:seed "seeds/seed-prompt.txt"
                   :refine "seeds/refine-prompt.txt"
                   :vary "seeds/vary-prompt.txt"}))))

