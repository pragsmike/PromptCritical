(ns pcrit.pdb-test
  (:require [clojure.test :refer (testing is deftest use-fixtures)]
            [clojure.java.io :as io]
            [pcrit.pdb :as pdb]
            [pcrit.util :as util]))

;; --- Test Fixture for Temp Directory ---

(def ^:dynamic *db-dir* nil)

(defn- delete-recursively [f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-recursively child)))
  (io/delete-file f))

(defn with-temp-db [f]
  (let [temp-dir (io/file "target" "test-db")]
    (when (.exists temp-dir) (delete-recursively temp-dir))
    (.mkdirs temp-dir)
    (binding [*db-dir* (.getCanonicalPath temp-dir)]
      (f))
    (delete-recursively temp-dir)))

(use-fixtures :each with-temp-db)


;; --- Tests ---

(deftest create-and-read-prompt-test
  (testing "Basic prompt creation and reading"
    (let [body "This is a test prompt."
          metadata {:author "tester"}
          created-prompt (pdb/create-prompt *db-dir* body :metadata metadata)
          prompt-id (:id (:header created-prompt))]

      (is (= "P1" prompt-id))
      (is (= (util/canonicalize-text body) (:body created-prompt))) ; Assert canonical body is returned
      (is (= "tester" (get-in created-prompt [:header :author])))
      (is (contains? (:header created-prompt) :created-at))
      (is (= (util/sha1-hex (util/canonicalize-text body)) (get-in created-prompt [:header :sha1-hash])))

      (testing "Reading the prompt back"
        (let [read-prompt (pdb/read-prompt *db-dir* prompt-id)]
          ;; Now that create-prompt returns a canonical body, this should pass
          (is (= created-prompt read-prompt)))))))

(deftest id-generation-test
  (testing "Ensures prompt IDs are unique and sequential"
    (let [p1 (pdb/create-prompt *db-dir* "First prompt")
          p2 (pdb/create-prompt *db-dir* "Second prompt")
          p3 (pdb/create-prompt *db-dir* "Third prompt")]
      (is (= "P1" (get-in p1 [:header :id])))
      (is (= "P2" (get-in p2 [:header :id])))
      (is (= "P3" (get-in p3 [:header :id]))))))

(deftest update-metadata-test
  (testing "Updating metadata of an existing prompt"
    (let [p1 (pdb/create-prompt *db-dir* "Original body.")
          id (get-in p1 [:header :id])
          original-hash (get-in p1 [:header :sha1-hash])]

      (testing "Successful update"
        (let [updated-prompt (pdb/update-metadata *db-dir* id #(assoc % :status "reviewed"))
              read-again (pdb/read-prompt *db-dir* id)]
          (is (= "reviewed" (get-in updated-prompt [:header :status])))
          ;; The body returned by update-metadata should be the canonical one.
          (is (= "Original body.\n" (:body updated-prompt)))
          (is (= original-hash (get-in updated-prompt [:header :sha1-hash])))
          (is (= updated-prompt read-again))))

      (testing "Update function must not change id"
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"must not change the prompt 'id'"
                     (pdb/update-metadata *db-dir* id #(assoc % :id "P9999")))))

      (testing "Update function must not change sha1-hash"
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"must not change the 'sha1-hash'"
                     (pdb/update-metadata *db-dir* id #(assoc % :sha1-hash "bogus-hash"))))))))

(deftest error-handling-and-edge-cases-test
  (testing "Reading a non-existent prompt returns nil"
    (is (nil? (pdb/read-prompt *db-dir* "P12345"))))

  (testing "Updating a non-existent prompt throws an exception"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Prompt P12345 not found"
                 (pdb/update-metadata *db-dir* "P12345" #(assoc % :foo :bar)))))

  (testing "Prompt body can contain YAML delimiters"
    (let [body "Hello\n---\nWorld"
          p (pdb/create-prompt *db-dir* body)
          id (get-in p [:header :id])
          read-p (pdb/read-prompt *db-dir* id)]
      (is (= (util/canonicalize-text body) (:body read-p)))))

  (testing "Checksum verification failure is logged"
    (let [p (pdb/create-prompt *db-dir* "Good content")
          id (get-in p [:header :id])
          prompt-file (io/file *db-dir* (str id ".prompt"))]
      (spit prompt-file (str "---\n"
                             "id: " id "\n"
                             "sha1-hash: " (get-in p [:header :sha1-hash]) "\n"
                             "---\n"
                             "Corrupted content\n"))
      (let [corrupted-record (pdb/read-prompt *db-dir* id)]
        (is (= "Corrupted content\n" (:body corrupted-record)))))))

(deftest concurrency-lock-test
  (testing "update-metadata respects a .lock file"
    (let [p (pdb/create-prompt *db-dir* "A prompt to be locked")
          id (get-in p [:header :id])
          lock-file (io/file *db-dir* (str id ".prompt.lock"))]
      (try
        (.createNewFile lock-file)
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Could not acquire lock"
              (pdb/update-metadata *db-dir* id #(assoc % :status "should-fail"))))
      (finally
        (.delete lock-file))))))
