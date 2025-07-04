(ns pcrit.pop.util
  (:import [java.nio.file Files Paths]
           [java.nio.file.attribute FileAttribute]))

(defn create-link [link-path target-path]
  (let [link   (Paths/get link-path (make-array String 0))
        target (Paths/get target-path (make-array String 0))]
    (Files/createSymbolicLink link target (make-array FileAttribute 0))))

;; --- Usage ---
;; This will create a symlink named "my-link" in the /tmp directory
;; that points to the file "/etc/hosts".
;; (create-link "/tmp/my-link" "/etc/hosts")
