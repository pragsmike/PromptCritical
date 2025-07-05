(ns pcrit.pop.core-test
  (:require [clojure.test :refer (deftest is use-fixtures testing)]
            [clojure.java.io :as io]
            [pcrit.pop.core :as pop]
            [pcrit.expdir.interface :as expdir]
            [pcrit.pop.temp-dir :as tutil :refer [with-temp-dir *tmp-dir*]]))

(use-fixtures :each with-temp-dir)


(deftest test-ingest-prompt-no-template-fields
  (let [record (pop/ingest-prompt *tmp-dir* "hello")
        md (:header record)]
    (is (= "hello\n" (:body record)))
    (is (= "1" (:spec-version md)))
    (is (= "P1" (:id md)))
    (is (= 6 (:character-count md)))
    (is (= 1 (:word-count md)))
    (is (empty? (:template-field-names md)))))

(deftest test-ingest-prompt-one-template-field
  (let [record (pop/ingest-prompt *tmp-dir* "hello {{MOM}}")
        md (:header record)]
    (is (= "hello {{MOM}}\n" (:body record)))
    (is (= "1" (:spec-version md)))
    (is (= "P1" (:id md)))
    (is (= 14 (:character-count md)))
    (is (= 2 (:word-count md)))
    (is (= ["MOM"] (:template-field-names md)))))

(deftest test-ingest-prompt-two-template-fields
  (let [record (pop/ingest-prompt *tmp-dir* "hello {{MOM}} and {{DAD}}")
        md (:header record)]
    (is (= "hello {{MOM}} and {{DAD}}\n" (:body record)))
    (is (= "1" (:spec-version md)))
    (is (= "P1" (:id md)))
    (is (= 26 (:character-count md)))
    (is (= 4 (:word-count md)))
    (is (= ["MOM" "DAD"] (:template-field-names md)))))


(def prompt-map-1
  {:seed "The seed!"
   :refine-1 "Make this better."})

(deftest test-intern-prompts
  (let [interned (pop/intern-prompts *tmp-dir* prompt-map-1)]
    (is (some? (:seed interned)))
    (is (map? (:seed interned)))
    (is (string? (:body (:seed interned))))))

(deftest read-prompt-map-test
  (testing "Successfully reads a valid prompt manifest"
    (let [manifest-resource (io/resource "test-prompts/manifest.edn")]
      (is (some? manifest-resource) "Test manifest 'manifest.edn' must be on the classpath.")
      (let [prompt-map (pop/read-prompt-map (.getPath manifest-resource))]
        (is (= #{:test-greeting :test-summary} (set (keys prompt-map))))
        (is (= "Hello, this is a test greeting for {{name}}.\n"
               (:test-greeting prompt-map)))
        (is (= "Please summarize the following content:\n{{content}}\n"
               (:test-summary prompt-map))))))

  (testing "Throws an ExceptionInfo for a manifest with a missing prompt file"
    (let [bad-manifest-resource (io/resource "test-prompts/bad-manifest.edn")]
      (is (some? bad-manifest-resource) "Test manifest 'bad-manifest.edn' must be on the classpath.")
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Prompt file not found for key: :bad-key"
            (pop/read-prompt-map (.getPath bad-manifest-resource)))))))

(deftest test-link-prompt
  (tutil/make-temp-exp-dir! *tmp-dir*)
  (spit (io/file (expdir/get-pdb-dir *tmp-dir*) "P1.prompt") "hello")
  (let [prec {:header {:id "P1"}  :body {}}]
    (expdir/link-prompt! *tmp-dir* prec "boat"))
  (is (.exists (io/file (expdir/get-link-dir *tmp-dir*) "boat"))))

(deftest test-bootstrap
  (tutil/make-temp-exp-dir! *tmp-dir*)
  (let [manifest-resource (io/resource "test-prompts/bootstrap.edn")
        prompt-file (io/file (expdir/get-pdb-dir *tmp-dir*) "P1.prompt")]
    (is (some? manifest-resource) "Test manifest 'manifest.edn' must be on the classpath.")

    (pop/bootstrap *tmp-dir*)
    (is (.exists prompt-file))))

