(ns pcrit.test-helper.interface
  (:require [pcrit.test-helper.core :as core]))

(def with-quiet-logging
  "A clojure.test :once fixture that sets the log level to :warn for a
  test run, restoring the application default (:info) afterward."
  core/with-quiet-logging)
