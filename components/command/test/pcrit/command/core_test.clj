(ns pcrit.command.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.core :as cmd]
            [pcrit.context.interface :as context]
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
  (testing "bootstrap process creates directories, ingests prompts, and creates links"
    ;; Setup: This helper only creates the seed files, not the experiment dirs.
    (let [exp-dir (get-temp-dir)
          ctx (context/new-context exp-dir)]
      (make-test-bootstrap-files! exp-dir)

      ;; Execute the bootstrap function, which is responsible for creating dirs.
      (cmd/bootstrap! ctx)

      ;; Verify the results
      (let [pdb-dir (expdir/get-pdb-dir ctx)
            links-dir (expdir/get-link-dir ctx)]
        (is (.exists (io/file pdb-dir)))
        (is (.exists (io/file pdb-dir "P1.prompt")))
        (is (.exists (io/file pdb-dir "P2.prompt")))
        (is (.exists (io/file pdb-dir "P3.prompt")))
        (is (.exists (io/file links-dir "seed")))
        (is (.exists (io/file links-dir "refine")))
        (is (.exists (io/file links-dir "vary")))
        (let [seed-link-target (-> (io/file links-dir "seed") .toPath Files/readSymbolicLink)
              p1-path (.toPath (io/file pdb-dir "P1.prompt"))]
          (is (= p1-path seed-link-target)))))))
