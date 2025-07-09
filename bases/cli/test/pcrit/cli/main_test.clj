(ns pcrit.cli.main-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.cli.main :as main]
            [pcrit.command.interface :as cmd]
            [pcrit.experiment.interface :as exp]
            [pcrit.pop.interface :as pop]
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
    (is (str/includes? output "Usage: pcrit"))))

(deftest cli-no-command-test
  (let [{:keys [exit-code output]} (run-cli [])]
    (is (= 1 exit-code))
    (is (str/includes? output "Usage: pcrit"))))

(deftest cli-unknown-command-test
  (let [{:keys [exit-code output]} (run-cli ["foobar"])]
    (is (= 1 exit-code))
    (is (str/includes? output "Unknown command: foobar"))))

(deftest cli-invalid-option-test
  (let [{:keys [exit-code output]} (run-cli ["--invalid"])]
    (is (= 1 exit-code))
    ;; CORRECTED: Expect the pr-str formatted output with double quotes.
    (is (str/includes? output "Unknown option: \"--invalid\""))
    (is (str/includes? output "errors occurred"))))

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
    ;; SETUP: Call underlying commands directly to create a valid state for `vary`.
    (make-temp-exp-dir! exp-dir)
    (cmd/bootstrap! ctx)
    (let [seed-prompt (pop/read-linked-prompt ctx "seed")]
      (pop/create-new-generation! ctx [seed-prompt])) ; Create gen-0

    ;; TEST: Now test the CLI dispatch with a mocked `vary!` command.
    (with-redefs [cmd/vary! (fn [_ctx] (reset! vary-called true))]
      (let [{:keys [exit-code]} (run-cli ["vary" exp-dir])]
        (is (nil? exit-code))
        (is (true? @vary-called) "vary! should have been called.")))))
