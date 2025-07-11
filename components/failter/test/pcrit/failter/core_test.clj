(ns pcrit.failter.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [pcrit.failter.core :as failter]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(deftest run-contest-test
  (testing "run-contest! orchestrates expdir and shell commands correctly"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          ;; Atoms to capture calls to mocked functions
          prepare-called (atom nil)
          capture-called (atom nil)
          shell-commands (atom [])
          ;; Mock implementations
          mock-prepare-dir (fn [ctx params]
                             (reset! prepare-called {:ctx ctx :params params})
                             ;; Return a realistic file path for the spec dir
                             (expdir/get-failter-spec-dir ctx (:generation-number params) (:contest-name params)))
          mock-capture-report (fn [ctx gen-num contest-name]
                                (reset! capture-called {:gen gen-num :name contest-name})
                                ;; Return a realistic file path for the report
                                (io/file (expdir/get-contest-dir ctx gen-num contest-name) "report.csv"))
          mock-shell-fn (fn [& args]
                          (swap! shell-commands conj (vec args))
                          {:exit 0 :out (str "Mocked " (first args)) :err ""})
          contest-params {:generation-number 0
                          :contest-name      "test-contest"
                          :inputs-dir        "/fake/inputs"
                          :population        [{:header {:id "P1"}}]
                          :models            ["model-a"]
                          :judge-model       "test-judge"}]

      (with-redefs [expdir/prepare-contest-directory! mock-prepare-dir
                    expdir/capture-contest-report!    mock-capture-report
                    clojure.java.shell/sh             mock-shell-fn]

        (let [result (failter/run-contest! ctx contest-params)]

          ;; 1. Verify the result of the top-level function call
          (is (:success result))
          (is (str/ends-with? (:report-path result) "report.csv"))

          ;; 2. Verify that the expdir functions were called correctly
          (is (some? @prepare-called) "prepare-contest-directory! should have been called.")
          (is (= contest-params (:params @prepare-called)))

          (is (some? @capture-called) "capture-contest-report! should have been called.")
          (is (= {:gen 0 :name "test-contest"} @capture-called))

          ;; 3. Verify that the failter shell commands were executed in order
          (is (= 3 (count @shell-commands)))
          (let [[cmd1 cmd2 cmd3] @shell-commands
                spec-path (.getCanonicalPath (expdir/get-failter-spec-dir ctx 0 "test-contest"))]
            (is (= ["failter" "experiment" spec-path] cmd1))
            (is (= ["failter" "evaluate" spec-path "--judge-model" "test-judge"] cmd2))
            (is (= ["failter" "report" spec-path] cmd3))))))))
