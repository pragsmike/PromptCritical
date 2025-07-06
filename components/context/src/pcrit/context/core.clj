(ns pcrit.context.core)

(defrecord AppContext
  [;; The absolute path to the root of the experiment directory.
   exp-dir])

(defn new-context
  "Creates a new AppContext record."
  [exp-dir]
  {:pre [(string? exp-dir) (not (clojure.string/blank? exp-dir))]}
  (->AppContext exp-dir))
