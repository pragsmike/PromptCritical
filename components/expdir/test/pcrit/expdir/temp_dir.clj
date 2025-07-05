(ns pcrit.expdir.temp-dir
  (:require [clojure.java.io :as io])
  (:import [java.nio.file Files Path]
           [java.nio.file.attribute FileAttribute]))

;; Tests can refer to the directory via this dynamic var.
(def ^:dynamic *tmp-dir* nil)

(defn- delete-recursively
  "Remove `file-or-dir` and everything beneath it, silencing any
  'file-not-found' errors that might appear during cleanup."
  [file-or-dir]
  (when (.exists file-or-dir)
    (doseq [f (reverse (file-seq file-or-dir))] ; children first
      (io/delete-file f true))))               ; `true` → silence errors

(defn with-temp-dir
  "A clojure.test :each fixture that binds *tmp-dir* and cleans up afterwards."
  [test-fn]
  (let [^Path tmp-path (Files/createTempDirectory "poly-test-" (make-array FileAttribute 0))
        tmp-dir        (.toFile tmp-path)]
    (try
      (binding [*tmp-dir* (.getAbsolutePath tmp-dir)]
        (test-fn))                             ; ← run the test
      (finally
        (delete-recursively tmp-dir)))))       ; ← always runs

