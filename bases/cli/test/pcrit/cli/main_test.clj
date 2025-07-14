(ns pcrit.cli.main-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pcrit.cli.main :as main]
            [pcrit.command.interface :as cmd]
            ;; Import both generic and command-specific helpers
            [pcrit.test-helper.interface :as th-generic]
            [pcrit.command.test-helper :as th-cmd]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

;; --- Test Helper Functions ---

(defn- run-cli [args]
  (let [exit-code (atom nil)
        output (atom nil)
        mock-exit-fn (fn [code msg] (reset! exit-code code) (reset! output msg))
        mock-out-fn (fn [msg] (reset! output msg) msg)
        injected-fns {:exit-fn mock-exit-fn :out-fn mock-out-fn}]
    (main/process-cli-args args injected-fns)
    {:exit-code @exit-code :output @output}))


;; --- CLI Tests ---

(deftest cli-help-test
  (let [{:keys [exit-code output]} (run-cli ["--help"])]
    (is (= 0 exit-code))
    (is (str/includes? output "Usage: pcrit <command>"))))

(deftest cli-unknown-command-test
  (let [{:keys [exit-code output]} (run-cli ["foobar"])]
    (is (= 1 exit-code))
    (is (str/includes? output "Unknown command: foobar"))))

(deftest cli-bootstrap-dispatch-test
  (let [exp-dir (th-generic/get-temp-dir)]
    ;; Use the new shared helper to create the necessary files
    (th-cmd/setup-bootstrappable-exp! exp-dir)
    (let [{:keys [exit-code]} (run-cli ["bootstrap" exp-dir])]
      (is (nil? exit-code) "Bootstrap should run without error.")
      (is (.exists (io/file exp-dir "pdb" "P1.prompt")))
      (is (.exists (io/file exp-dir "links" "seed"))))))

(deftest cli-vary-dispatch-test
  (let [exp-dir (th-generic/get-temp-dir)
        ;; Use the new shared helper to create a fully bootstrapped experiment
        ctx (th-cmd/setup-bootstrapped-exp! exp-dir)
        vary-called (atom false)]
    (with-redefs [cmd/vary! (fn [_ctx] (reset! vary-called true))]
      (let [{:keys [exit-code]} (run-cli ["vary" exp-dir])]
        (is (nil? exit-code))
        (is (true? @vary-called) "vary! should have been called.")))))

(deftest cli-evaluate-dispatch-test
  (testing "evaluate command with --judge-model flag"
    (let [exp-dir (th-generic/get-temp-dir)
          ;; Use the new shared helper to create a configured experiment
          ctx (th-cmd/setup-configured-exp! exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          evaluate-called (atom nil)]
      (with-redefs [cmd/evaluate! (fn [_ctx options] (reset! evaluate-called options))]
        (run-cli ["evaluate" exp-dir
                  "--inputs" (.getCanonicalPath inputs-dir)
                  "--judge-model" "cli-judge-model"]))

      (is (some? @evaluate-called) "cmd/evaluate! should have been called.")
      (is (= "cli-judge-model" (:judge-model @evaluate-called))))))

(deftest cli-evolve-dispatch-test
  (testing "evolve command correctly parses options and calls the command fn"
    (let [exp-dir (th-generic/get-temp-dir)
          ;; Use the new shared helper to create a configured experiment
          ctx (th-cmd/setup-configured-exp! exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          evolve-called (atom nil)]
      (with-redefs [cmd/evolve! (fn [_ctx options] (reset! evolve-called options))]
        (run-cli ["evolve" exp-dir
                  "--inputs" (.getCanonicalPath inputs-dir)
                  "--generations" "5"
                  "--max-cost" "10.50"]))

      (is (some? @evolve-called) "cmd/evolve! should have been called.")
      (let [opts @evolve-called]
        (is (= 5 (:generations opts)))
        (is (= 10.5 (:max-cost opts)))
        (is (= (.getCanonicalPath inputs-dir) (:inputs opts)))))))
