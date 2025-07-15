(ns pcrit.failter.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [pcrit.failter.core :as failter]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]
            [pcrit.log.interface :as log]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(def ^:private sample-json-output "[{\"prompt_id\": \"P1.prompt\", \"score\": 99}]")
(def ^:private sample-parsed-json [{:prompt_id "P1.prompt", :score 99}])

(deftest run-contest-happy-path-test
  (testing "run-contest! orchestrates spec file, shell command, and JSON file parsing correctly"
    (let [exp-dir (get-temp-dir)
          inputs-dir (doto (io/file exp-dir "my-inputs") .mkdirs)
          ctx (exp/new-experiment-context exp-dir)
          shell-command (atom nil)
          mock-shell-fn (fn [& args]
                          (reset! shell-command (vec args))
                          (let [contest-dir (expdir/get-contest-dir ctx 0 "test-contest")
                                report-file (io/file contest-dir "failter-report.json")]
                            (spit report-file sample-json-output))
                          {:exit 0 :out "failter log message" :err ""})
          contest-params {:generation-number 0
                          :contest-name      "test-contest"
                          :inputs-dir        (.getCanonicalPath inputs-dir)
                          :population        [{:header {:id "P1"}} {:header {:id "P2"}}]
                          :models            ["model-a"]
                          :judge-model       "test-judge"}]
      (expdir/create-experiment-dirs! ctx)
      (.mkdirs (expdir/get-pdb-dir ctx))

      (with-redefs [clojure.java.shell/sh mock-shell-fn]
        (let [result (failter/run-contest! ctx contest-params)
              contest-dir (expdir/get-contest-dir ctx 0 "test-contest")]

          (is (:success result))
          (is (= sample-parsed-json (:parsed-json result)))

          (is (.exists (io/file contest-dir "failter.stdout.log")))
          (is (= "failter log message" (slurp (io/file contest-dir "failter.stdout.log"))))

          (is (some? @shell-command))
          (is (= ["failter" "run" "--spec" (str contest-dir "/spec.yml")] @shell-command)))))))

(deftest run-contest-shell-failure-test
  (testing "run-contest! returns a failure map and logs stderr when shell command fails"
    (let [exp-dir (get-temp-dir)
          inputs-dir (doto (io/file exp-dir "my-inputs") .mkdirs)
          ctx (exp/new-experiment-context exp-dir)
          mock-shell-fn (fn [& _args] {:exit 1 :out "" :err "Failter process error"})
          contest-params {:generation-number 0
                          :contest-name      "test-contest"
                          :inputs-dir        (.getCanonicalPath inputs-dir)
                          :population        [] :models [] :judge-model ""}]
      (expdir/create-experiment-dirs! ctx)

      (with-redefs [clojure.java.shell/sh mock-shell-fn
                    log/error (fn [& args])] ; Suppress log output for this test
        (let [result (failter/run-contest! ctx contest-params)]
          (is (false? (:success result)))
          (is (= "Failter process error" (:error result)))
          ;; NEW: Check that stderr log file is still created on failure
          (let [stderr-log (io/file (expdir/get-contest-dir ctx 0 "test-contest") "failter.stderr.log")]
            (is (.exists stderr-log))
            (is (= "Failter process error" (slurp stderr-log)))))))))

(deftest run-contest-missing-report-file-test
  (testing "run-contest! returns a failure map when failter does not create the report file"
    (let [exp-dir (get-temp-dir)
          inputs-dir (doto (io/file exp-dir "my-inputs") .mkdirs)
          ctx (exp/new-experiment-context exp-dir)
          mock-shell-fn (fn [& _args] {:exit 0 :out "failter log" :err ""})
          contest-params {:generation-number 0
                          :contest-name      "test-contest"
                          :inputs-dir        (.getCanonicalPath inputs-dir)
                          :population        [] :models [] :judge-model ""}]
      (expdir/create-experiment-dirs! ctx)

      (with-redefs [clojure.java.shell/sh mock-shell-fn]
        (let [result (failter/run-contest! ctx contest-params)]
          (is (false? (:success result)))
          (is (= "Failter report file not found." (:error result))))))))
