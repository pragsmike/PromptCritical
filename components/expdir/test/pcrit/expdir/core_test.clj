(ns pcrit.expdir.core-test
  (:require [clojure.test :refer (use-fixtures deftest is testing)]
            [clojure.java.io :as io]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.core :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]])
  (:import [java.io File]
           [java.nio.file Files Path]))

(use-fixtures :each with-temp-dir)

(defn- get-test-ctx []
  (exp/new-experiment-context (get-temp-dir)))

(deftest path-getter-functions-test
  (testing "Getter functions return correct File objects for subdirectories"
    (let [ctx (get-test-ctx)]
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "pdb"))
             (.getCanonicalPath (expdir/get-pdb-dir ctx))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations"))
             (.getCanonicalPath (expdir/get-generations-dir ctx))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "links"))
             (.getCanonicalPath (expdir/get-link-dir ctx))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "seeds"))
             (.getCanonicalPath (expdir/get-seeds-dir ctx))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "bootstrap.edn"))
             (.getCanonicalPath (expdir/bootstrap-spec-file ctx)))))))

(deftest create-experiment-dirs-test
  (testing "Creates all standard directories idempotently"
    (let [ctx (get-test-ctx)
          pdb-dir (expdir/get-pdb-dir ctx)
          gens-dir (expdir/get-generations-dir ctx)
          links-dir (expdir/get-link-dir ctx)
          seeds-dir (expdir/get-seeds-dir ctx)]

      (is (not (.exists pdb-dir)) "Precondition: PDB dir should not exist.")

      ;; First call
      (expdir/create-experiment-dirs! ctx)

      (is (.isDirectory pdb-dir))
      (is (.isDirectory links-dir))
      (is (.isDirectory seeds-dir))
      ;; CORRECTED: Assert that the generations dir is NOT created.
      (is (not (.exists gens-dir)) "Generations dir should NOT be created by this fn.")

      ;; Second call (should not throw an error)
      (expdir/create-experiment-dirs! ctx)
      (is (.isDirectory pdb-dir) "Directory should still exist after second call."))))

(deftest pdb-file-of-prompt-record-test
  (testing "Constructs the correct path to a prompt file in the PDB"
    (let [ctx (get-test-ctx)
          mock-record {:header {:id "P42"}}
          expected-path (io/file (expdir/get-pdb-dir ctx) "P42.prompt")
          actual-file (expdir/pdb-file-of-prompt-record ctx mock-record)]
      (is (= (.getCanonicalPath expected-path) (.getCanonicalPath actual-file)))))

  (testing "Throws an exception if record is missing the prompt ID"
    (let [ctx (get-test-ctx)
          bad-record {:header {:other-key "value"}}]
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Cannot determine prompt path"
            (expdir/pdb-file-of-prompt-record ctx bad-record))))))

(defn- is-relative-symlink?
  "Test utility to check if a file is a relative symbolic link."
  [^File f]
  (and (Files/isSymbolicLink (.toPath f))
       (not (.isAbsolute ^Path (Files/readSymbolicLink (.toPath f))))))

(deftest create-relative-symlink-test
  (testing "Generic symlink function creates a correct relative link"
    (let [root-dir (io/file (get-temp-dir))
          target-dir (io/file root-dir "targets")
          link-dir (io/file root-dir "links")
          target-file (io/file target-dir "target.txt")
          link-file (io/file link-dir "link.txt")]

      (.mkdirs target-dir)
      (.mkdirs link-dir)
      (spit target-file "hello world")

      (expdir/create-relative-symlink! link-file target-file)

      (is (is-relative-symlink? link-file) "Created file should be a relative symbolic link."))))


;; --- Generation and Contest Tests ---

(deftest generation-path-getters-test
  (testing "Generation-specific path getters return correct File objects"
    (let [ctx (get-test-ctx)]
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-012"))
             (.getCanonicalPath (expdir/get-generation-dir ctx 12))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-001" "population"))
             (.getCanonicalPath (expdir/get-population-dir ctx 1))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-003" "contests"))
             (.getCanonicalPath (expdir/get-contests-dir ctx 3))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-004" "contests" "my-contest"))
             (.getCanonicalPath (expdir/get-contest-dir ctx 4 "my-contest")))))))

(deftest failter-artifacts-dir-test
  (testing "get-failter-artifacts-dir returns the correct path"
    (let [ctx (get-test-ctx)
          expected-path (io/file (get-temp-dir) "generations" "gen-002" "contests" "a-contest" "failter-artifacts")]
      (is (= (.getCanonicalPath expected-path)
             (.getCanonicalPath (expdir/get-failter-artifacts-dir ctx 2 "a-contest")))))))
