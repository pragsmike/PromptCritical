(ns pcrit.pop.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.pop.core :as pop]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-quiet-logging with-temp-dir get-temp-dir]]))

(use-fixtures :each with-quiet-logging with-temp-dir)

;; --- Core Tests ---

(deftest ingest-prompt-test
  (testing "ingest-prompt adds analysis metadata"
    (expdir/create-experiment-dirs! (get-temp-dir))
    (let [record (pop/ingest-prompt (expdir/get-pdb-dir (get-temp-dir)) "Hello {{NAME}}")
          md (:header record)]
      (is (= "Hello {{NAME}}\n" (:body record)))
      (is (= "P1" (:id md)))
      (is (= 15 (:character-count md)))
      (is (= 2 (:word-count md)))
      (is (= ["NAME"] (:template-field-names md))))))

(deftest intern-prompts-test
  (testing "intern-prompts converts a map of strings to a map of records"
    (expdir/create-experiment-dirs! (get-temp-dir))
    (let [prompt-map {:seed "The seed!" :refine "Make it better."}
          interned (pop/intern-prompts (expdir/get-pdb-dir (get-temp-dir)) prompt-map)]
      (is (some? (:seed interned)))
      (is (map? (:seed interned)))
      (is (= "P1" (get-in interned [:seed :header :id])))
      (is (= "The seed!\n" (:body (:seed interned))))
      (is (= "P2" (get-in interned [:refine :header :id]))))))

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

