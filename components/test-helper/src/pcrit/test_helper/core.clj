(ns pcrit.test-helper.core
  (:require [taoensso.telemere :as tel]))

(defn with-quiet-logging
  "A clojure.test :once fixture that sets the log level to :warn for the
  duration of a test namespace, restoring the application default (:info)
  afterward. This keeps test output clean on success."
  [f]
  (try
    (tel/set-min-level! :warn)
    (f)
    (finally
      (tel/set-min-level! :info))))
