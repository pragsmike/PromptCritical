(ns pcrit.expdir.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.context.interface :as context]
            [pcrit.expdir.core :as expdir]
            [pcrit.expdir.temp-dir :refer [with-temp-dir *tmp-dir*]])
  (:import [java.nio.file Files]))

(use-fixtures :each with-temp-dir)

(defn- get-test-ctx []
  (context/new-context *tmp-dir*))

(deftest path-getter-functions-test
  (testing "Getter functions return correct File objects for subdirectories"
    (let [ctx (get-test-ctx)]
      (is (= (.getCanonicalPath (io/file *tmp-dir* "pdb"))
             (.getCanonicalPath (expdir/get-pdb-dir ctx))))
      (is (= (.getCanonicalPath (io/file *tmp-dir* "generations"))
             (.getCanonicalPath (expdir/get-generations-dir ctx))))
      (is (= (.getCanonicalPath (io/file *tmp-dir* "links"))
             (.getCanonicalPath (expdir/get-link-dir ctx))))
      (is (= (.getCanonicalPath (io/file *tmp-dir* "seeds"))
             (.getCanonicalPath (expdir/get-seeds-dir ctx))))
      (is (= (.getCanonicalPath (io/file *tmp-dir* "bootstrap.edn"))
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

(deftest link-prompt-test
  (testing "Correctly creates a symbolic link to a prompt file"
    (let [ctx (get-test-ctx)]
      ;; Setup: Create directories and a dummy prompt file to link to
      (expdir/create-experiment-dirs! ctx)
      (let [links-dir (expdir/get-link-dir ctx)
            mock-record {:header {:id "P101"}}
            prompt-file (expdir/pdb-file-of-prompt-record ctx mock-record)
            link-file (io/file links-dir "test-link")]

        (spit prompt-file "This is the prompt content.")
        (is (.exists prompt-file))
        (is (not (.exists link-file)) "Precondition: Link should not exist.")

        ;; Execute the function
        (expdir/link-prompt! ctx mock-record "test-link")

        (is (.exists link-file) "Link should be created.")
        (is (Files/isSymbolicLink (.toPath link-file)) "Created file should be a symbolic link.")

        (let [target-path (Files/readSymbolicLink (.toPath link-file))
              expected-target-path (.toPath prompt-file)]
          (is (= expected-target-path target-path) "Link should point to the correct prompt file."))))))
