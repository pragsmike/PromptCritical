(ns pcrit.test-helper.interface
  (:require [pcrit.test-helper.core :as core]
            [pcrit.test-helper.temp-dir :as tmpdir]))

(def with-quiet-logging
  "A clojure.test :once fixture that sets the log level to :warn for a
  test run, restoring the application default (:info) afterward."
  core/with-quiet-logging)

(def with-temp-dir
  "A clojure.test :each fixture that creates a new temp directory
  for each test run, deleting it afterward."
  tmpdir/with-temp-dir)

(defn get-temp-dir [] tmpdir/*tmp-dir*)
