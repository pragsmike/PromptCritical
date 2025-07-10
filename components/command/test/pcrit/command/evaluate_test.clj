(ns pcrit.command.evaluate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.failter.interface :as failter]
            [pcrit.pop.interface :as pop]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir]]))

(use-fixtures :each with-temp-dir)

(defn- setup-test-env []
  (let [ctx (exp/new-experiment-context (get-temp-dir))
        inputs-dir (io/file (get-temp-dir) "inputs")]
    (expdir/create-experiment-dirs! ctx)
    (.mkdirs inputs-dir)
    (let [p1 (pop/ingest-prompt ctx "An object prompt {{INPUT_TEXT}}")]
      (pop/create-new-generation! ctx [p1]))
    {:ctx ctx :inputs-dir inputs-dir}))

(deftest evaluate-happy-path-test
  (testing "evaluate! with all options calls failter"
    (let [failter-called (atom nil)]
      (with-redefs [failter/run-contest! #(reset! failter-called %&)]
        (let [{:keys [ctx inputs-dir]} (setup-test-env)]
          (evaluate/evaluate! ctx {:generation 0 :name "my-test" :inputs (.getCanonicalPath inputs-dir)})
          (is (seq @failter-called) "failter/run-contest! should have been called.")
          (let [[_ opts] @failter-called]
            (is (= 0 (:generation-number opts)))
            (is (= "my-test" (:contest-name opts)))
            (is (= 1 (count (:population opts))))))))))

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

;; CORRECTED: Each validation check is now in its own deftest for isolation.
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
          ;; Creates gen-1, which is empty. Gen-0 is unaffected.
          (pop/create-new-generation! ctx [])
          (evaluate/evaluate! ctx {:generation 1 :inputs (.getCanonicalPath inputs-dir)})
          (is (nil? @failter-called) "failter/run-contest! should not be called for empty population."))))))
