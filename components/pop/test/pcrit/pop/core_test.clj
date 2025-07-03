(ns pcrit.pop.core-test
  (:require [clojure.test :refer (deftest is use-fixtures)]
            [clojure.java.io :as io]
            [pcrit.pop.core :as pop]
            [pcrit.pop.temp-dir :refer [with-temp-dir *tmp-dir*]]))

(use-fixtures :each with-temp-dir)

(deftest test-ingest-prompt-no-template-fields
  (let [record (pop/ingest-prompt *tmp-dir* "hello")
        md (:header record)]
    (is (= "hello\n" (:body record)))
    (is (= "1" (:spec-version md)))
    (is (= "P1" (:id md)))
    (is (= 5 (:character-count md)))
    (is (= 1 (:word-count md)))
    (is (empty? (:template-field-names md)))))

(deftest test-ingest-prompt-one-template-field
  (let [record (pop/ingest-prompt *tmp-dir* "hello {{MOM}}")
        md (:header record)]
    (is (= "hello {{MOM}}\n" (:body record)))
    (is (= "1" (:spec-version md)))
    (is (= "P1" (:id md)))
    (is (= 13 (:character-count md)))
    (is (= 2 (:word-count md)))
    (is (= ["MOM"] (:template-field-names md)))))

(deftest test-ingest-prompt-two-template-fields
  (let [record (pop/ingest-prompt *tmp-dir* "hello {{MOM}} and {{DAD}}")
        md (:header record)]
    (is (= "hello {{MOM}} and {{DAD}}\n" (:body record)))
    (is (= "1" (:spec-version md)))
    (is (= "P1" (:id md)))
    (is (= 25 (:character-count md)))
    (is (= 4 (:word-count md)))
    (is (= ["MOM" "DAD"] (:template-field-names md)))))

(deftest test-bootstrap
  (pop/bootstrap *tmp-dir* "hi")
  (let [pf (io/file *tmp-dir* "P1.prompt")]
    (is (.exists pf))))
