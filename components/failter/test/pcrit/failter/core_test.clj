(ns pcrit.failter.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell]
            [pcrit.failter.core :as failter]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(defn- mock-shell-fn [& args]
  (let [opts (apply hash-map (drop-while #(not (keyword? %)) args))
        dir (:dir opts)
        cmd-args (take-while #(not (keyword? %)) (rest args))]
    (when (= "report" (first cmd-args))
      (spit (io/file dir "report.csv") "prompt,score\nP1,0.95"))
    {:exit 0 :out "Mock command successful" :err ""}))

(deftest run-contest-test
  (testing "run-contest! successfully prepares, runs, and captures a contest"
    (let [exp-dir     (get-temp-dir)
          ctx         (exp/new-experiment-context exp-dir)
          inputs-dir  (io/file exp-dir "test-inputs")]
      ;; Setup the experiment and input directories
      (expdir/create-experiment-dirs! ctx)
      (.mkdirs inputs-dir)
      (spit (io/file inputs-dir "doc1.txt") "input document one")

      ;; CORRECTED: Create the dummy prompt file in the PDB so the symlink target exists.
      (spit (io/file (expdir/get-pdb-dir ctx) "P1.prompt") "This is the content of P1.")

      (let [mock-population [{:header {:id "P1"}}]]
        (with-redefs [clojure.java.shell/sh mock-shell-fn]
          (let [result (failter/run-contest! ctx {:generation-number   0
                                                  :contest-name        "test-contest"
                                                  :inputs-dir          (.getCanonicalPath inputs-dir)
                                                  :population          mock-population})]
            (is (:success result))
            ;; Verify directory structure
            (let [contest-dir      (expdir/get-contest-dir ctx 0 "test-contest")
                  spec-dir         (expdir/get-failter-spec-dir ctx 0 "test-contest")
                  final-report     (io/file contest-dir "report.csv")
                  metadata-file    (io/file contest-dir "contest-metadata.edn")]
              (is (.exists contest-dir))
              (is (.exists spec-dir))
              (is (.exists (io/file spec-dir "inputs" "doc1.txt")))
              (is (.exists (io/file spec-dir "templates" "P1.prompt")))
              (is (.exists final-report) "report.csv should be moved to the contest directory")
              (is (not (.exists (io/file spec-dir "report.csv"))) "report.csv should no longer be in the spec dir")
              (is (.exists metadata-file))))))))

  (testing "Fails when inputs directory does not exist"
    (let [ctx (exp/new-experiment-context (get-temp-dir))]
      (expdir/create-experiment-dirs! ctx)
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Inputs directory not found"
           (failter/run-contest! ctx {:generation-number 0
                                      :contest-name "bad-contest"
                                      :inputs-dir "/path/to/nonexistent/inputs"
                                      :population [{:header {:id "P1"}}]}))))))
