(ns pcrit.pdb.lock
  (:require [clojure.java.io :as io]
            [pcrit.log.interface :as log]
            [pcrit.config.interface :as config])
  (:import [java.io File]))

(defn execute-with-lock
  "Self-healing, retrying lock protocol for a given lock file.
  Takes a lock-file and a thunk `f`. Executes `f` while holding the lock."
  [^File lock-file f]
  (let [{:keys [max-retries stale-lock-threshold-ms retry-ms retry-jitter-ms]} (:locking config/config)]
    (when (.exists lock-file)
      (let [lock-age (- (System/currentTimeMillis) (.lastModified lock-file))]
        (if (> lock-age stale-lock-threshold-ms)
          (do
            (log/warn "Deleting stale lock file " (.getName lock-file) " (age:" lock-age "ms)")
            (io/delete-file lock-file))
          (log/info "Lock file " (.getName lock-file) " is too new to be stale. Waiting."))))

    (loop [retries-left max-retries]
      (cond
        (zero? retries-left)
        (throw (ex-info (str "Could not acquire lock, timed out after " max-retries " retries.")
                        {:lock-file lock-file}))

        (.createNewFile lock-file)
        (try
          (f)
          (finally
            (io/delete-file lock-file true)))

        :else
        (do
          (Thread/sleep (+ retry-ms (rand-int retry-jitter-ms)))
          (recur (dec retries-left)))))))
