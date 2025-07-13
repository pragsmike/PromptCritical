(ns pcrit.pdb.io-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.pdb.io :as pdb-io]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]))

(use-fixtures :once with-quiet-logging)
(use-fixtures :each with-temp-dir)

(deftest write-prompt-record-sorts-header-keys-test
  (testing "Ensures that header keys are written in alphabetical order regardless of map order"
    (let [target-file (io/file (get-temp-dir) "P99.prompt")
          ;; Define a header with keys intentionally out of alphabetical order
          unsorted-header {:z-key 26, :a-key 1, :m-key 13}
          prompt-record {:header unsorted-header
                         :body "The body of the prompt.\n"}
          ;; The expected regex pattern for the sorted keys
          expected-pattern #"(?s)a-key: 1\n.*m-key: 13\n.*z-key: 26\n"]

      ;; Action: Write the prompt record to a file
      (pdb-io/write-prompt-record-atomically! target-file prompt-record)

      ;; Verification: Read the raw file and check the key order
      (let [file-content (slurp target-file)]
        (is (re-find expected-pattern file-content)
            "The keys in the YAML front matter should be sorted alphabetically.")))))
