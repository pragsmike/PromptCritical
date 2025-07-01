(ns pcrit.pdb.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.pdb.core :as pdb]
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


;; --- Existing Tests ---

(deftest create-and-read-prompt-test
  (testing "Basic prompt creation and reading"
    (let [body "This is a test prompt."
          metadata {:author "tester"}
          created-prompt (pdb/create-prompt *db-dir* body :metadata metadata)
          prompt-id (:id (:header created-prompt))]

      (is (= "P1" prompt-id))
      (is (= (util/canonicalize-text body) (:body created-prompt)))
      (is (= "tester" (get-in created-prompt [:header :author])))
      (is (contains? (:header created-prompt) :created-at))
      (is (= (util/sha1-hex (util/canonicalize-text body)) (get-in created-prompt [:header :sha1-hash])))

      (testing "Reading the prompt back"
        (let [read-prompt (pdb/read-prompt *db-dir* prompt-id)]
          (is (= created-prompt read-prompt)))))))

(deftest update-metadata-test
  (testing "Updating metadata of an existing prompt"
    (let [p1 (pdb/create-prompt *db-dir* "Original body.")
          id (get-in p1 [:header :id])
          original-hash (get-in p1 [:header :sha1-hash])]

      (testing "Successful update"
        (let [updated-prompt (pdb/update-metadata *db-dir* id #(assoc % :status "reviewed"))
              read-again (pdb/read-prompt *db-dir* id)]
          (is (= "reviewed" (get-in updated-prompt [:header :status])))
          (is (= "Original body.\n" (:body updated-prompt)))
          (is (= original-hash (get-in updated-prompt [:header :sha1-hash])))
          (is (= updated-prompt read-again)))))))

;; --- New Tests for Spec Conformance ---

(deftest spec-conformance-test
  (testing "Body with multiple trailing newlines round-trips to exactly one"
    (let [body-with-extra-newlines "This body has too many newlines.\n\n\n"
          p (pdb/create-prompt *db-dir* body-with-extra-newlines)
          id (get-in p [:header :id])
          read-p (pdb/read-prompt *db-dir* id)]
      (is (= "This body has too many newlines.\n" (:body read-p)))))

  (testing "Header keys are keywordized on read"
    (let [p (pdb/create-prompt *db-dir* "body text" :metadata {:a 1})
          id (get-in p [:header :id])
          read-p (pdb/read-prompt *db-dir* id)]
      (is (contains? (:header read-p) :a) "Should contain keyword :a")
      (is (not (contains? (:header read-p) "a")) "Should not contain string key \"a\"")
      (is (keyword? (first (keys (:header read-p)))) "First key of header should be a keyword")))

  (testing "Parser handles missing final newline after header block"
    (let [malformed-content "---\nid: P999\nauthor: manual\n---\nThis is the body.\n"
          prompt-file (io/file *db-dir* "P999.prompt")]
      (spit prompt-file malformed-content)
      (let [read-p (pdb/read-prompt *db-dir* "P999")]
        (is (some? read-p) "Prompt should have been read successfully")
        (is (= "P999" (get-in read-p [:header :id])) "Header :id should be parsed correctly")
        (is (= "manual" (get-in read-p [:header :author])) "Header :author should be parsed correctly")
        (is (= "This is the body.\n" (:body read-p)) "Body should be parsed correctly")))))


;; --- Existing Tests for Concurrency and Robustness ---

(deftest concurrency-and-robustness-test
  (testing "Atomic ID generation across threads"
    (let [num-threads 10
          ids-per-thread 20
          total-ids (* num-threads ids-per-thread)
          futures (repeatedly num-threads
                              #(future (dotimes [_ ids-per-thread] (pdb/create-prompt *db-dir* "body"))))]
      (let [results (mapv deref futures)]
        (is (= (str total-ids) (slurp (io/file *db-dir* "pdb.counter")))))))

  (testing "Stale lock removal"
    (let [{:keys [header]} (pdb/create-prompt *db-dir* "a prompt")
          id (:id header)
          lock-file (io/file *db-dir* (str id ".prompt.lock"))
          stale-time (- (System/currentTimeMillis) (* 60 20 1000))] ; 20 minutes ago
      (.createNewFile lock-file)
      (is (.setLastModified lock-file stale-time))
      (let [updated (pdb/update-metadata *db-dir* id #(assoc % :status "unstuck"))]
        (is (= "unstuck" (:status (:header updated)))))))

  (testing "Lock acquisition blocks and retries"
    (let [p (pdb/create-prompt *db-dir* "a prompt for locking")
          id (get-in p [:header :id])
          lock-file (io/file *db-dir* (str id ".prompt.lock"))]
      (.createNewFile lock-file)
      (let [update-future (future (pdb/update-metadata *db-dir* id #(assoc % :status "updated")))]
        (is (not (realized? update-future)))
        (Thread/sleep 250)
        (is (not (realized? update-future)))
        (io/delete-file lock-file)
        (let [result @update-future]
          (is (= "updated" (get-in result [:header :status])))))))

  (testing "Updater function cannot remove id or hash"
    (let [p (pdb/create-prompt *db-dir* "a prompt")
          id (get-in p [:header :id])]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"MUST NOT remove or change"
            (pdb/update-metadata *db-dir* id #(dissoc % :id))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"MUST NOT remove or change"
            (pdb/update-metadata *db-dir* id #(dissoc % :sha1-hash))))))

  (testing "Parser handles CRLF line endings"
    (let [crlf-body "This body\r\nhas CRLF\r\nendings."
          crlf-header (str "---\r\n" "id: P99\r\n" "---\r\n")
          crlf-content (str crlf-header crlf-body)
          prompt-file (io/file *db-dir* "P99.prompt")]
      (spit prompt-file crlf-content)
      (let [read-p (pdb/read-prompt *db-dir* "P99")]
        (is (= (util/canonicalize-text crlf-body) (:body read-p)))
        (is (= "P99" (get-in read-p [:header :id])))))))
