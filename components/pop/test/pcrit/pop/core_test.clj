(ns pcrit.pop.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.pop.core :as pop]
            [pcrit.expdir.interface :as expdir]
            [pcrit.pop.temp-dir :refer [with-temp-dir *tmp-dir*]]
            [pcrit.test-helper.interface :refer [with-quiet-logging]])
  (:import [java.nio.file Files]))

(use-fixtures :each with-quiet-logging with-temp-dir)

;; --- Test Helper ---

(defn- make-test-bootstrap-files!
  "Creates a self-contained, valid bootstrap setup in the test directory."
  [target-dir]
  (let [seeds-dir (io/file target-dir "seeds")]
    (.mkdirs seeds-dir)
    (spit (io/file seeds-dir "seed.txt") "The seed! {{INPUT_TEXT}}")
    (spit (io/file seeds-dir "refine.txt") "Refine this: {{OBJECT-PROMPT}}")
    (spit (io/file seeds-dir "vary.txt") "Vary this: {{OBJECT-PROMPT}}")
    (spit (io/file target-dir "bootstrap.edn")
          (pr-str {:seed "seeds/seed.txt"
                   :refine "seeds/refine.txt"
                   :vary "seeds/vary.txt"}))))


;; --- Core Tests ---

(deftest ingest-prompt-test
  (testing "ingest-prompt adds analysis metadata"
    (expdir/create-experiment-dirs! *tmp-dir*) ;; <--- FIX: Ensure pdb dir exists
    (let [record (pop/ingest-prompt (expdir/get-pdb-dir *tmp-dir*) "Hello {{NAME}}")
          md (:header record)]
      (is (= "Hello {{NAME}}\n" (:body record)))
      (is (= "P1" (:id md)))
      (is (= 15 (:character-count md)))
      (is (= 2 (:word-count md)))
      (is (= ["NAME"] (:template-field-names md))))))

(deftest intern-prompts-test
  (testing "intern-prompts converts a map of strings to a map of records"
    (expdir/create-experiment-dirs! *tmp-dir*) ;; <--- FIX: Ensure pdb dir exists
    (let [prompt-map {:seed "The seed!" :refine "Make it better."}
          interned (pop/intern-prompts (expdir/get-pdb-dir *tmp-dir*) prompt-map)]
      (is (some? (:seed interned)))
      (is (map? (:seed interned)))
      (is (= "P1" (get-in interned [:seed :header :id])))
      (is (= "The seed!\n" (:body (:seed interned))))
      (is (= "P2" (get-in interned [:refine :header :id]))))))

(deftest read-prompt-map-test
  (testing "Successfully reads a valid, programmatically created prompt manifest"
    (let [manifest-dir (io/file *tmp-dir* "manifest-test")
          prompts-subdir (io/file manifest-dir "prompts")]
      (.mkdirs prompts-subdir)
      (spit (io/file prompts-subdir "greeting.txt") "Hello, {{name}}.")
      (spit (io/file prompts-subdir "summary.txt") "Summarize: {{content}}.")
      (let [manifest-file (io/file manifest-dir "manifest.edn")]
        (spit manifest-file (pr-str {:greeting "prompts/greeting.txt"
                                     :summary "prompts/summary.txt"}))

        (let [prompt-map (pop/read-prompt-map (.getPath manifest-file))]
          (is (= #{:greeting :summary} (set (keys prompt-map))))
          (is (= "Hello, {{name}}." (:greeting prompt-map)))
          (is (= "Summarize: {{content}}." (:summary prompt-map)))))))

  (testing "Throws an ExceptionInfo for a manifest with a missing prompt file"
    (let [manifest-file (io/file *tmp-dir* "bad-manifest.edn")]
      (spit manifest-file (pr-str {:bad-key "path/to/nonexistent.txt"}))
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Prompt file not found for key: :bad-key"
            (pop/read-prompt-map (.getPath manifest-file)))))))

(deftest bootstrap-test
  (testing "bootstrap process creates directories, ingests prompts, and creates links"
    ;; Setup: This helper only creates the seed files, not the experiment dirs.
    (make-test-bootstrap-files! *tmp-dir*)

    ;; Execute the bootstrap function, which is responsible for creating dirs.
    (pop/bootstrap *tmp-dir*)

    ;; Verify the results
    (let [pdb-dir (expdir/get-pdb-dir *tmp-dir*)
          links-dir (expdir/get-link-dir *tmp-dir*)]
      (is (.exists (io/file pdb-dir "P1.prompt")))
      (is (.exists (io/file pdb-dir "P2.prompt")))
      (is (.exists (io/file pdb-dir "P3.prompt")))
      (is (.exists (io/file links-dir "seed")))
      (is (.exists (io/file links-dir "refine")))
      (is (.exists (io/file links-dir "vary")))
      (let [seed-link-target (-> (io/file links-dir "seed") .toPath Files/readSymbolicLink)
            p1-path (.toPath (io/file pdb-dir "P1.prompt"))]
        (is (= p1-path seed-link-target))))))
