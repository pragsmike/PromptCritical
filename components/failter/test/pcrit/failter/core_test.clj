(ns pcrit.failter.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clj-yaml.core :as yaml]
            [pcrit.failter.core :as failter]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(def ^:private sample-json-output "{\"prompt_id\": \"P1.prompt\", \"score\": 99}")

(deftest run-contest-happy-path-test
  (testing "run-contest! orchestrates spec file creation and shell command correctly"
    (let [exp-dir (get-temp-dir)
          inputs-dir (doto (io/file exp-dir "my-inputs") .mkdirs)
          ctx (exp/new-experiment-context exp-dir)
          shell-command (atom nil)
          mock-shell-fn (fn [& args]
                          (reset! shell-command (vec args))
                          {:exit 0 :out sample-json-output :err "Log message"})
          contest-params {:generation-number 0
                          :contest-name      "test-contest"
                          :inputs-dir        (.getCanonicalPath inputs-dir)
                          :population        [{:header {:id "P1"}} {:header {:id "P2"}}]
                          :models            ["model-a"]
                          :judge-model       "test-judge"}]
      (expdir/create-experiment-dirs! ctx)
      (.mkdirs (expdir/get-pdb-dir ctx))

      (with-redefs [clojure.java.shell/sh mock-shell-fn]
        (let [result (failter/run-contest! ctx contest-params)]

          ;; 1. Verify the result of the top-level function call
          (is (:success result))
          (is (= sample-json-output (:json-report result)))

          ;; 2. Verify that the spec.yml file was created and is valid
          (let [spec-file (io/file (expdir/get-contest-dir ctx 0 "test-contest") "spec.yml")]
            (is (.exists spec-file) "spec.yml should have been created.")
            (let [spec-data (yaml/parse-string (slurp spec-file))]
              (is (= 2 (:version spec-data)))
              (is (= ["P1.prompt" "P2.prompt"] (:templates spec-data)))
              (is (= "test-judge" (:judge_model spec-data)))
              (is (.endsWith (:artifacts_dir spec-data) "failter-artifacts/")))

            ;; 3. Verify that the correct failter shell command was executed
            (is (some? @shell-command))
            (is (= ["failter" "run" "--spec" (.getCanonicalPath spec-file)] @shell-command))))))))

(deftest run-contest-failure-test
  (testing "run-contest! returns a failure map when shell command fails"
    (let [exp-dir (get-temp-dir)
          inputs-dir (doto (io/file exp-dir "my-inputs") .mkdirs)
          ctx (exp/new-experiment-context exp-dir)
          mock-shell-fn (fn [& _args] {:exit 1 :out "" :err "Failter process error"})
          contest-params {:generation-number 0
                          :contest-name      "test-contest"
                          :inputs-dir        (.getCanonicalPath inputs-dir)
                          :population        [] :models [] :judge-model ""}]
      (expdir/create-experiment-dirs! ctx)

      (with-redefs [clojure.java.shell/sh mock-shell-fn]
        (let [result (failter/run-contest! ctx contest-params)]
          (is (false? (:success result)))
          (is (= "Failter process error" (:error result))))))))
