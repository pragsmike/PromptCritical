(ns pcrit.command.core-test
  (:require [clojure.test :refer (deftest testing is use-fixtures)]
            [clojure.java.io :as io]
            [pcrit.command.core :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-quiet-logging with-temp-dir get-temp-dir]])
  (:import [java.nio.file Files]))

(use-fixtures :each with-quiet-logging with-temp-dir)

;; --- Test Helper ---

(defn- make-test-bootstrap-files!
  "Creates a self-contained, valid bootstrap setup in the test directory."
  [target-dir]
  (let [seeds-dir (io/file target-dir "seeds")]
    (.mkdirs seeds-dir)
    (spit (io/file seeds-dir "seed.txt") "The seed! {{INPUT_TEXT}}")
    (spit (io/file seeds-dir "refine.txt") "Refine this: {{OBJECT_PROMPT}}")
    (spit (io/file seeds-dir "vary.txt") "Vary this: {{OBJECT_PROMPT}}")
    (spit (io/file target-dir "bootstrap.edn")
          (pr-str {:seed "seeds/seed.txt"
                   :refine "seeds/refine.txt"
                   :vary "seeds/vary.txt"}))))


;; --- Core Tests ---

(deftest bootstrap-test
  (testing "bootstrap process creates dirs, ingests prompts, creates links, and creates gen-0"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)]
      (make-test-bootstrap-files! exp-dir) ; Helper creates one object-prompt (seed)

      (cmd/bootstrap! ctx)

      (let [pdb-dir (expdir/get-pdb-dir ctx)
            links-dir (expdir/get-link-dir ctx)
            gen0-pop-dir (expdir/get-population-dir ctx 0)]
        ;; Verify pdb ingestion
        (is (.exists (io/file pdb-dir "P1.prompt")))
        (is (.exists (io/file pdb-dir "P2.prompt")))
        (is (.exists (io/file pdb-dir "P3.prompt")))

        ;; Verify links (now dynamically created for all manifest entries)
        (is (.exists (io/file links-dir "seed")))
        (is (.exists (io/file links-dir "refine")))
        (is (.exists (io/file links-dir "vary")))

        ;; Verify gen-0 creation
        (is (.isDirectory gen0-pop-dir) "Generation 0 population directory should be created.")

        ;; Verify gen-0 population
        (let [pop-files (.listFiles gen0-pop-dir)]
          (is (= 1 (count pop-files)) "gen-0 population should have one member.")
          (let [pop-member (first pop-files)]
            (is (= "P1.prompt" (.getName pop-member)) "gen-0 population should contain the seed object-prompt.")
            (is (Files/isSymbolicLink (.toPath pop-member)) "Population member should be a symlink.")))))))


(deftest bootstrap-link-relativity-test
  (testing "bootstrap! command creates relative symbolic links"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)]
      (make-test-bootstrap-files! exp-dir)

      ;; Execute the command
      (cmd/bootstrap! ctx)

      (let [links-dir (expdir/get-link-dir ctx)
            seed-link-file (io/file links-dir "seed")]

        (is (Files/isSymbolicLink (.toPath seed-link-file)) "File 'seed' should be a symbolic link.")

        (let [link-target-path (Files/readSymbolicLink (.toPath seed-link-file))]
          (is (not (.isAbsolute link-target-path))
              "The symbolic link created by bootstrap! should be relative."))))))
