(ns pcrit.expdir.core-test
  (:require [clojure.test :refer (use-fixtures deftest is testing)]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
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
      (is (.isDirectory gens-dir))
      (is (.isDirectory links-dir))
      (is (.isDirectory seeds-dir))

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


;; --- Generation Tests ---

(deftest generation-path-getters-test
  (testing "Generation-specific path getters return correct File objects"
    (let [ctx (get-test-ctx)]
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-012"))
             (.getCanonicalPath (expdir/get-generation-dir ctx 12))))
      (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-001" "population"))
             (.getCanonicalPath (expdir/get-population-dir ctx 1)))))))

;; --- Contest Management Tests ---

(deftest prepare-contest-directory-test
  (testing "Correctly prepares the full contest directory structure"
    (let [ctx (get-test-ctx)
          exp-dir (get-temp-dir)
          inputs-dir (io/file exp-dir "my-inputs")]
      (expdir/create-experiment-dirs! ctx)
      (.mkdirs inputs-dir)
      (spit (io/file inputs-dir "doc1.txt") "doc1")
      (spit (io/file (expdir/get-pdb-dir ctx) "P1.prompt") "prompt1")

      (let [params {:generation-number 0
                    :contest-name "my-contest"
                    :inputs-dir (.getCanonicalPath inputs-dir)
                    :population [{:header {:id "P1"}}]
                    :models ["m1" "m2"]
                    :judge-model "judgy"}]
        (expdir/prepare-contest-directory! ctx params)

        (let [spec-dir (expdir/get-failter-spec-dir ctx 0 "my-contest")
              input-link (io/file spec-dir "inputs" "doc1.txt")
              template-link (io/file spec-dir "templates" "P1.prompt")
              models-file (io/file spec-dir "model-names.txt")
              meta-file (io/file (expdir/get-contest-dir ctx 0 "my-contest") "contest-metadata.edn")]
          (is (.isDirectory (io/file spec-dir "inputs")))
          (is (.isDirectory (io/file spec-dir "templates")))
          (is (is-relative-symlink? input-link) "Input link must be relative.")
          (is (is-relative-symlink? template-link) "Template link must be relative.")
          (is (= "m1\nm2" (slurp models-file)))
          (is (.exists meta-file))
          (let [meta-data (edn/read-string (slurp meta-file))]
            (is (= "my-contest" (:contest-name meta-data)))
            (is (= ["P1"] (:participants meta-data)))))))))

(deftest capture-contest-report-test
  (let [ctx (get-test-ctx)]
    (testing "Moves report from spec dir to contest dir"
      (let [spec-dir (expdir/get-failter-spec-dir ctx 0 "capture-test")
            source-report (io/file spec-dir "report.csv")
            dest-dir (expdir/get-contest-dir ctx 0 "capture-test")]
        (.mkdirs spec-dir)
        (spit source-report "prompt,score\nP1,100")

        (expdir/capture-contest-report! ctx 0 "capture-test")

        (is (not (.exists source-report)) "Source report should be gone.")
        (is (.exists (io/file dest-dir "report.csv")) "Destination report should exist.")))

    (testing "Returns nil if source report does not exist"
      (let [result (expdir/capture-contest-report! ctx 1 "no-report-test")]
        (is (nil? result))))))
