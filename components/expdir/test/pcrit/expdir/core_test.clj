(ns pcrit.expdir.core-test
  (:require [clojure.test :refer (use-fixtures deftest is testing)]
            [clojure.java.io :as io]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.core :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]
            )
  (:import [java.nio.file Files]))

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

      (is (.exists link-file) "Link should be created.")
      (is (Files/isSymbolicLink (.toPath link-file)) "Created file should be a symbolic link.")

      (let [link-target-path (Files/readSymbolicLink (.toPath link-file))
            link-dir-path (.toPath (.getParentFile link-file))
            resolved-target-path (.toAbsolutePath (.resolve link-dir-path link-target-path))
            canonical-target-path (.toPath (.getCanonicalFile target-file))]

        (is (not (.isAbsolute link-target-path))
            "The symbolic link target path should be relative.")

        (is (= (.normalize canonical-target-path) (.normalize resolved-target-path))
            "The relative link should resolve to the correct target file.")))))

(deftest link-prompt-test
  (testing "Correctly creates a relative symbolic link to a prompt file"
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

        (let [link-target-path (Files/readSymbolicLink (.toPath link-file))
              link-dir-path (.toPath (.getParentFile link-file))
              resolved-target-path (.toAbsolutePath (.resolve link-dir-path link-target-path))
              canonical-target-path (.toPath (.getCanonicalFile prompt-file))]

          (is (not (.isAbsolute link-target-path))
              "The symbolic link target path should be relative.")

          (is (= (.normalize canonical-target-path) (.normalize resolved-target-path))
              "The relative link should resolve to the correct target file."))))))

;; --- Generation Tests ---

(deftest generation-path-getters-test
  (testing "Generation-specific path getters return correct File objects"
    (let [ctx (get-test-ctx)]
      (testing "get-generation-dir"
        (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-000"))
               (.getCanonicalPath (expdir/get-generation-dir ctx 0))))
        (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-012"))
               (.getCanonicalPath (expdir/get-generation-dir ctx 12)))))

      (testing "get-population-dir"
        (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-001" "population"))
               (.getCanonicalPath (expdir/get-population-dir ctx 1)))))

      (testing "get-contests-dir"
        (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-002" "contests"))
               (.getCanonicalPath (expdir/get-contests-dir ctx 2)))))

      (testing "get-contest-dir"
        (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-003" "contests" "my-contest"))
               (.getCanonicalPath (expdir/get-contest-dir ctx 3 "my-contest")))))

      (testing "get-failter-spec-dir"
        (is (= (.getCanonicalPath (io/file (get-temp-dir) "generations" "gen-004" "contests" "another-run" "failter-spec"))
               (.getCanonicalPath (expdir/get-failter-spec-dir ctx 4 "another-run"))))))))


(deftest find-latest-generation-number-test
  (let [ctx (get-test-ctx)]
    (testing "Returns nil when generations directory does not exist"
      (is (nil? (expdir/find-latest-generation-number ctx))))

    (testing "Returns nil when generations directory is empty"
      (.mkdirs (expdir/get-generations-dir ctx))
      (is (nil? (expdir/find-latest-generation-number ctx))))

    (testing "Finds the highest number among valid generation directories"
      (let [gens-dir (expdir/get-generations-dir ctx)]
        (.mkdirs (io/file gens-dir "gen-000"))
        (.mkdirs (io/file gens-dir "gen-002"))
        (.mkdirs (io/file gens-dir "gen-001"))
        (is (= 2 (expdir/find-latest-generation-number ctx)))))

    (testing "Ignores files and directories that do not match the pattern"
      (let [gens-dir (expdir/get-generations-dir ctx)]
        (.mkdirs (io/file gens-dir "gen-005"))
        (spit (io/file gens-dir "gen-006.tmp") "temp")
        (spit (io/file gens-dir "notes.txt") "notes")
        (.mkdirs (io/file gens-dir "gen-abc"))
        (is (= 5 (expdir/find-latest-generation-number ctx)))))))
