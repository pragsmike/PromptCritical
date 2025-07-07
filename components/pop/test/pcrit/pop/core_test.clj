(ns pcrit.pop.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.pop.core :as pop]
            [pcrit.context.interface :as context]
            [pcrit.expdir.interface :as expdir]
            [pcrit.pdb.interface :as pdb]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]])
  (:import [java.nio.file Files Path]))

(use-fixtures :once with-quiet-logging)
(use-fixtures :each with-temp-dir)

(defn- get-test-ctx []
  (context/new-context (get-temp-dir)))

;; --- Existing Tests ---

(deftest ingest-prompt-test
  (testing "ingest-prompt adds analysis metadata"
    (let [ctx (get-test-ctx)]
      (expdir/create-experiment-dirs! ctx)
      (let [record (pop/ingest-prompt ctx "Hello {{NAME}}")
            md (:header record)]
        (is (= "Hello {{NAME}}\n" (:body record)))
        (is (= "P1" (:id md)))
        (is (= 15 (:character-count md)))
        (is (= 2 (:word-count md)))
        (is (= ["NAME"] (:template-field-names md)))))))

(deftest intern-prompts-test
  (testing "intern-prompts converts a map of strings to a map of records"
    (let [ctx (get-test-ctx)]
      (expdir/create-experiment-dirs! ctx)
      (let [prompt-map {:seed "The seed!" :refine "Make it better."}
            interned (pop/intern-prompts ctx prompt-map)]
        (is (some? (:seed interned)))
        (is (map? (:seed interned)))
        (is (= "P1" (get-in interned [:seed :header :id])))
        (is (= "The seed!\n" (:body (:seed interned))))
        (is (= "P2" (get-in interned [:refine :header :id])))))))

(deftest read-prompt-map-test
  (testing "Successfully reads a valid, programmatically created prompt manifest"
    (let [manifest-dir (io/file (get-temp-dir) "manifest-test")
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
    (let [manifest-file (io/file (get-temp-dir) "bad-manifest.edn")]
      (spit manifest-file (pr-str {:bad-key "path/to/nonexistent.txt"}))
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Prompt file not found for key: :bad-key"
            (pop/read-prompt-map (.getPath manifest-file)))))))


;; --- Tests for Population Management ---

(deftest population-management-test
  (let [ctx (get-test-ctx)
        pdb-dir (expdir/get-pdb-dir ctx)]
    (expdir/create-experiment-dirs! ctx)
    ;; Create some initial prompts in the PDB to work with
    (let [p1 (pdb/create-prompt pdb-dir "Prompt One")
          p2 (pdb/create-prompt pdb-dir "Prompt Two")
          p3 (pdb/create-prompt pdb-dir "Prompt Three")]

      (testing "create-new-generation! creates the first generation correctly"
        (let [new-gen-info (pop/create-new-generation! ctx [p1 p2])]
          (is (= {:generation-number 0, :population-size 2} new-gen-info))

          (let [pop-dir (expdir/get-population-dir ctx 0)]
            (is (.isDirectory pop-dir) "Population directory should be created.")

            (let [link1 (io/file pop-dir "P1.prompt")
                  link2 (io/file pop-dir "P2.prompt")]
              (is (.exists link1))
              (is (.exists link2))

              (let [link-target (Files/readSymbolicLink (.toPath link1))]
                (is (not (.isAbsolute link-target)) "Link should be relative.")

                ;; --- FIX IS HERE ---
                (let [pop-dir-path (.toPath pop-dir)
                      resolved-target-path (.resolve pop-dir-path link-target)
                      resolved-target-file (.toFile resolved-target-path)]
                  (is (= (.getCanonicalPath (expdir/pdb-file-of-prompt-record ctx p1))
                         (.getCanonicalPath resolved-target-file)))))))))

      (testing "load-population loads prompts from a specified generation"
        ;; Assumes the previous test created gen 0
        (let [loaded-pop (pop/load-population ctx 0)
              loaded-ids (map #(get-in % [:header :id]) loaded-pop)]
          (is (= 2 (count loaded-pop)))
          (is (= #{"P1" "P2"} (set loaded-ids)))))

      (testing "load-population returns empty list for non-existent generation"
        (is (= [] (pop/load-population ctx 99))))

      (testing "create-new-generation! creates the next generation"
        (let [next-gen-info (pop/create-new-generation! ctx [p3])]
          (is (= {:generation-number 1, :population-size 1} next-gen-info))
          (let [pop-dir (expdir/get-population-dir ctx 1)]
            (is (.isDirectory pop-dir))
            (is (.exists (io/file pop-dir "P3.prompt")))))))))
