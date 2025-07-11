(ns pcrit.command.evaluate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.failter.interface :as failter]
            [pcrit.pop.interface :as pop]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(defn- setup-test-env []
  (let [exp-dir (get-temp-dir)
        ctx (exp/new-experiment-context exp-dir)
        inputs-dir (io/file exp-dir "inputs")
        config-file (io/file exp-dir "evolution-parameters.edn")]
    (expdir/create-experiment-dirs! ctx)
    (.mkdirs inputs-dir)
    ;; Create gen-0 with a single prompt
    (let [p1 (pop/ingest-prompt ctx "An object prompt {{INPUT_TEXT}}")]
      (pop/create-new-generation! ctx [p1]))
    ;; Always create a valid, default config file.
    (spit config-file
          (pr-str {:evaluate {:models ["model-from-config"]
                              :judge-model "judge-from-config"}}))
    {:ctx ctx :inputs-dir inputs-dir}))

(deftest evaluate-happy-path-test
  (testing "evaluate! with all options calls failter with config values"
    (let [failter-called (atom nil)]
      (with-redefs [failter/run-contest! #(reset! failter-called %&)]
        (let [{:keys [ctx inputs-dir]} (setup-test-env)]
          (evaluate/evaluate! ctx {:generation 0
                                   :name "my-test"
                                   :inputs (.getCanonicalPath inputs-dir)})
          (is (seq @failter-called) "failter/run-contest! should have been called.")
          (let [[_ opts] @failter-called]
            (is (= ["model-from-config"] (:models opts)))
            (is (= "judge-from-config" (:judge-model opts)))))))))

(deftest evaluate-cli-override-test
  (testing "evaluate! uses CLI judge-model to override config value"
    (let [failter-called (atom nil)]
      (with-redefs [failter/run-contest! #(reset! failter-called %&)]
        (let [{:keys [ctx inputs-dir]} (setup-test-env)]
          (evaluate/evaluate! ctx {:generation 0
                                   :name "my-test"
                                   :inputs (.getCanonicalPath inputs-dir)
                                   :judge-model "judge-from-cli"})
          (is (seq @failter-called) "failter/run-contest! should have been called.")
          (let [[_ opts] @failter-called]
            (is (= "judge-from-cli" (:judge-model opts)))))))))

(deftest evaluate-defaults-test
  (testing "evaluate! uses defaults for generation and contest name"
    (let [failter-called (atom nil)]
      (with-redefs [failter/run-contest! #(reset! failter-called %&)]
        (let [{:keys [ctx inputs-dir]} (setup-test-env)]
          (evaluate/evaluate! ctx {:inputs (.getCanonicalPath inputs-dir)})
          (is (seq @failter-called) "failter/run-contest! should be called.")
          (let [[_ opts] @failter-called]
            (is (= 0 (:generation-number opts)) "Should default to latest generation (0).")
            (is (= "contest" (:contest-name opts)) "Should default to contest name 'contest'.")))))))

(deftest evaluate-validation-contest-exists-test
  (testing "Validation fails if contest already exists"
    (let [failter-called (atom nil)]
      (with-redefs [failter/run-contest! #(reset! failter-called %&)]
        (let [{:keys [ctx inputs-dir]} (setup-test-env)]
          (.mkdirs (expdir/get-contest-dir ctx 0 "existing-contest"))
          (evaluate/evaluate! ctx {:generation 0 :name "existing-contest" :inputs (.getCanonicalPath inputs-dir)})
          (is (nil? @failter-called) "failter/run-contest! should not be called."))))))

(deftest evaluate-validation-empty-population-test
  (testing "Validation fails if population is empty"
    (let [failter-called (atom nil)]
      (with-redefs [failter/run-contest! #(reset! failter-called %&)]
        (let [{:keys [ctx inputs-dir]} (setup-test-env)]
          (pop/create-new-generation! ctx []) ; Creates gen-1, which is empty
          (evaluate/evaluate! ctx {:generation 1 :inputs (.getCanonicalPath inputs-dir)})
          (is (nil? @failter-called) "failter/run-contest! should not be called for empty population."))))))

(deftest evaluate-validation-no-models-test
  (testing "Validation fails if no models are configured"
    (let [failter-called (atom nil)]
      (with-redefs [failter/run-contest! #(reset! failter-called %&)]
        (let [{:keys [ctx inputs-dir]} (setup-test-env)]
          ;; CORRECTED: Explicitly overwrite the config file for this specific test case.
          (spit (io/file (get-temp-dir) "evolution-parameters.edn")
                (pr-str {:evaluate {:judge-model "j1"}})) ; Config without :models
          (evaluate/evaluate! ctx {:inputs (.getCanonicalPath inputs-dir)})
          (is (nil? @failter-called) "failter/run-contest! should not be called."))))))
