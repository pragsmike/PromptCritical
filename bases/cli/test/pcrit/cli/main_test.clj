(ns pcrit.cli.main-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.cli.main :as main]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir make-temp-exp-dir!]]))

(use-fixtures :each with-temp-dir)

(defn- run-cli [args]
  (let [exit-code (atom nil)
        output (atom nil)
        mock-exit-fn (fn [code msg] (reset! exit-code code) (reset! output msg))
        mock-out-fn (fn [msg] (reset! output msg) msg)
        injected-fns {:exit-fn mock-exit-fn :out-fn mock-out-fn}]
    (main/process-cli-args args injected-fns)
    {:exit-code @exit-code :output @output}))

(deftest cli-help-test
  (let [{:keys [exit-code output]} (run-cli ["--help"])]
    (is (= 0 exit-code))
    (is (str/includes? output "Usage: pcrit <command>"))))

(deftest cli-unknown-command-test
  (let [{:keys [exit-code output]} (run-cli ["foobar"])]
    (is (= 1 exit-code))
    (is (str/includes? output "Unknown command: foobar"))))

(deftest cli-bootstrap-dispatch-test
  (let [exp-dir (get-temp-dir)]
    (make-temp-exp-dir! exp-dir)
    (let [{:keys [exit-code]} (run-cli ["bootstrap" exp-dir])]
      (is (nil? exit-code))
      (is (.exists (io/file exp-dir "pdb" "P1.prompt")))
      (is (.exists (io/file exp-dir "links" "seed"))))))

(deftest cli-vary-dispatch-test
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)
        vary-called (atom false)]
    (make-temp-exp-dir! exp-dir)
    (cmd/bootstrap! ctx)
    (with-redefs [cmd/vary! (fn [_ctx] (reset! vary-called true))]
      (let [{:keys [exit-code]} (run-cli ["vary" exp-dir])]
        (is (nil? exit-code))
        (is (true? @vary-called) "vary! should have been called.")))))

;; ADDED: Test for the evaluate command dispatch and its new flag.
(deftest cli-evaluate-dispatch-test
  (testing "evaluate command with --judge-model flag"
    (let [exp-dir (get-temp-dir)
          inputs-dir (io/file exp-dir "inputs")
          evaluate-called (atom nil)]
      (.mkdirs inputs-dir)

      ;; Mock the command that will be called by the CLI handler
      (with-redefs [cmd/evaluate! (fn [_ctx options] (reset! evaluate-called options))]
        (run-cli ["evaluate" exp-dir
                  "--inputs" (.getCanonicalPath inputs-dir)
                  "--judge-model" "cli-judge-model"]))

      (is (some? @evaluate-called) "cmd/evaluate! should have been called.")
      (is (= "cli-judge-model" (:judge-model @evaluate-called))))))
