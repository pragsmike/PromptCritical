(ns pcrit.command.init-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(defn- exp-path [exp-dir & components]
  (apply io/file exp-dir components))

(deftest init-command-test
  (testing "init! successfully creates a new experiment skeleton"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          result (cmd/init! ctx {:force? false})]

      (is (= {:exit-code 0} result) "Command should succeed.")

      (is (.exists (exp-path exp-dir "bootstrap.edn")))
      (is (.exists (exp-path exp-dir "evolution-parameters.edn")))
      (is (.exists (exp-path exp-dir ".gitignore")))
      (is (.exists (exp-path exp-dir "seeds" "seed-object-prompt.txt")))
      (is (.exists (exp-path exp-dir "seeds" "improve-meta-prompt.txt")))

      (is (-> (exp-path exp-dir "bootstrap.edn") slurp count (> 0))
          "bootstrap.edn should not be empty.")))

  (testing "init! fails if directory contains user files and --force is not used"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)]
      ;; Pre-populate the directory
      (spit (exp-path exp-dir "a-file.txt") "some content")

      (let [result (cmd/init! ctx {:force? false})]
        (is (= {:exit-code 1} result) "Command should fail for non-empty directory."))))

  (testing "init! succeeds on a newly-created, empty directory"
    (let [exp-dir-path (str (get-temp-dir) "/new-dir")
          exp-dir (io/file exp-dir-path)
          ctx (exp/new-experiment-context exp-dir-path)]

      ;; Create a directory that is truly empty from a user perspective,
      ;; but will contain '.' and '..' for the filesystem.
      (.mkdirs exp-dir)
      (is (.isDirectory exp-dir))

      (let [result (cmd/init! ctx {:force? false})]
        (is (= {:exit-code 0} result) "Command should succeed on a fresh, empty directory without --force."))))

  (testing "init! succeeds if directory is not empty and --force is used"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          dummy-bootstrap (exp-path exp-dir "bootstrap.edn")]
      ;; Pre-populate with a file that will be overwritten
      (spit dummy-bootstrap "old-content")
      (is (= "old-content" (slurp dummy-bootstrap)))

      (let [result (cmd/init! ctx {:force? true})]
        (is (= {:exit-code 0} result) "Command should succeed with --force.")
        (is (not= "old-content" (slurp dummy-bootstrap))
            "File should have been overwritten by the template.")))))
