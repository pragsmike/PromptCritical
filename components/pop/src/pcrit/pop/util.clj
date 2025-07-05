(ns pcrit.pop.util
  (:import [java.nio.file Files Paths]
           [java.nio.file.attribute FileAttribute]))

(defn ->path [f] (if (string? f)
                   f
                   (.getPath f)))

(defn create-link [link-path target-path]
  (let [link   (Paths/get (->path link-path) (make-array String 0))
        target (Paths/get (->path target-path) (make-array String 0))]
    (Files/createSymbolicLink target link (make-array FileAttribute 0))))

;; --- Usage ---
;; This will create a symlink named "my-link" in the /tmp directory
;; that points to the file "/etc/hosts".
;; (create-link "/tmp/my-link" "/etc/hosts")
