(ns pcrit.command.core-test
  (:require [clojure.test :refer (deftest testing is use-fixtures)]
            [clojure.java.io :as io]
            [pcrit.command.core :as cmd]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :as th-generic]
            [pcrit.command.test-helper :as th-cmd])
  (:import [java.nio.file Files]))

(use-fixtures :each th-generic/with-quiet-logging th-generic/with-temp-dir)

(deftest bootstrap-test
  (testing "bootstrap process creates dirs, ingests prompts, creates links, and creates gen-0"
    (let [exp-dir (th-generic/get-temp-dir)
          ;; Use the new helper to set up preconditions
          ctx (th-cmd/setup-bootstrappable-exp! exp-dir)]

      ;; Run the command being tested
      (cmd/bootstrap! ctx)

      (let [pdb-dir (expdir/get-pdb-dir ctx)
            links-dir (expdir/get-link-dir ctx)
            gen0-pop-dir (expdir/get-population-dir ctx 0)]
        ;; Verify pdb ingestion
        (is (.exists (io/file pdb-dir "P1.prompt")))
        (is (.exists (io/file pdb-dir "P2.prompt")))

        ;; Verify links (now dynamically created for all manifest entries)
        (is (.exists (io/file links-dir "seed")))
        (is (.exists (io/file links-dir "refine")))

        ;; Verify gen-0 creation
        (is (.isDirectory gen0-pop-dir) "Generation 0 population directory should be created.")

        ;; Verify gen-0 population
        (let [pop-files (.listFiles gen0-pop-dir)]
          (is (= 1 (count pop-files)) "gen-0 population should have one member.")
          (let [pop-member (first pop-files)]
            (is (= "P1.prompt" (.getName pop-member)) "gen-0 population should contain the seed object-prompt.")
            (is (Files/isSymbolicLink (.toPath pop-member)) "Population member should be a symlink.")))))))


(deftest bootstrap-link-relativity-test
  (testing "bootstrap! command creates relative symbolic links"
    (let [exp-dir (th-generic/get-temp-dir)
          ;; Use the new helper to set up preconditions
          ctx (th-cmd/setup-bootstrappable-exp! exp-dir)]

      ;; Execute the command
      (cmd/bootstrap! ctx)

      (let [links-dir (expdir/get-link-dir ctx)
            seed-link-file (io/file links-dir "seed")]

        (is (Files/isSymbolicLink (.toPath seed-link-file)) "File 'seed' should be a symbolic link.")

        (let [link-target-path (Files/readSymbolicLink (.toPath seed-link-file))]
          (is (not (.isAbsolute link-target-path))
              "The symbolic link created by bootstrap! should be relative."))))))
