(ns pcrit.pdb.id
  (:require [clojure.java.io :as io]
            [pcrit.pdb.lock :as lock]
            [pcrit.pdb.io :as pdb-io]
            [pcrit.log :as log]))

(defn get-next-id!
  "Atomically reads and increments a counter file, returning the next ID as a string."
  [db-dir]
  (let [lock-file (io/file db-dir "pdb.counter.lock")]
    (lock/execute-with-lock lock-file
      (fn []
        (let [counter-file (io/file db-dir "pdb.counter")]
          (try
            (let [next-id (if (.exists counter-file)
                            (inc (Integer/parseInt (slurp counter-file)))
                            1)]
              (pdb-io/atomic-write-file! counter-file (str next-id))
              ;; For maximum durability, fsync the counter file even after an atomic move.
              (pdb-io/fsync! counter-file)
              (str "P" next-id))
            (catch NumberFormatException e
              (log/error "pdb.counter file is corrupt. Could not parse integer.")
              (throw (ex-info "Corrupt counter file" {:file counter-file} e)))))))))
