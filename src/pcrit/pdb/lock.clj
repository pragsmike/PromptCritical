(ns pcrit.pdb.lock
  (:require [clojure.java.io :as io]
            [pcrit.log :as log])
  (:import [java.io File]))

(def ^:private lock-retry-ms 50)
(def ^:private lock-retry-jitter-ms 25)
(def ^:private lock-max-retries 100) ; ~5-7.5 seconds total
(def ^:private stale-lock-threshold-ms (* 10 60 1000)) ; 10 minutes

(defn execute-with-lock
  "Self-healing, retrying lock protocol for a given lock file.
  Takes a lock-file and a thunk `f`. Executes `f` while holding the lock."
  [^File lock-file f]
  (when (.exists lock-file)
    (let [lock-age (- (System/currentTimeMillis) (.lastModified lock-file))]
      (if (> lock-age stale-lock-threshold-ms)
        (do
          (log/warn "Deleting stale lock file" (.getName lock-file) "(age:" lock-age "ms)")
          (io/delete-file lock-file))
        (log/info "Lock file" (.getName lock-file) "is too new to be stale. Waiting."))))

  (loop [retries-left lock-max-retries]
    (cond
      (zero? retries-left)
      (throw (ex-info (str "Could not acquire lock, timed out after " lock-max-retries " retries.")
                      {:lock-file lock-file}))

      (.createNewFile lock-file)
      (try
        (f)
        (finally
          (io/delete-file lock-file true)))

      :else
      (do
        (Thread/sleep (+ lock-retry-ms (rand-int lock-retry-jitter-ms)))
        (recur (dec retries-left))))))
