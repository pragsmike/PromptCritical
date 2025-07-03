(ns pcrit.pop.core-test
  (:require [clojure.test :refer (deftest is use-fixtures)]
            [clojure.java.io :as io]
            [pcrit.pop.core :as pop]
            [prcit.pop.temp-dir :refer [with-temp-dir *tmp-dir*]]))

(use-fixtures :each with-temp-dir)


(deftest test-bootstrap
  (pop/bootstrap *tmp-dir* "hi")
  (let [pf (io/file *tmp-dir* "P1.prompt")]
    (is (.exists pf))))
