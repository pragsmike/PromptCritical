(ns pcrit.command.evaluate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.expdir.interface :as expdir]
            [pcrit.failter.interface :as failter]
            [pcrit.reports.interface :as reports]
            [pcrit.command.test-helper :as th-cmd]
            [pcrit.test-helper.interface :as th-generic]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

(def ^:private mock-failter-json-success "[]")

(deftest evaluate-happy-path-test
  (testing "evaluate! with all options calls failter and reports correctly"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          failter-called (atom nil)
          reports-called (atom nil)]
      (with-redefs [failter/run-contest! (fn [_ctx params]
                                           (reset! failter-called params)
                                           {:success true :json-report mock-failter-json-success})
                    reports/process-and-write-csv-report! (fn [json-str path]
                                                            (reset! reports-called {:json json-str :path path}))]
        (evaluate/evaluate! ctx {:generation 0
                                 :name "my-test"
                                 :inputs (.getCanonicalPath inputs-dir)})

        (is (some? @failter-called) "failter/run-contest! should have been called.")
        (is (= ["mock-model"] (:models @failter-called)))
        (is (= "mock-judge" (:judge-model @failter-called)))

        (is (some? @reports-called) "reports/process-and-write-csv-report! should have been called.")
        (is (= mock-failter-json-success (:json @reports-called)))
        (is (.endsWith (:path @reports-called) (str "my-test" java.io.File/separator "report.csv")))))))

(deftest evaluate-cli-override-test
  (testing "evaluate! uses CLI judge-model to override config value"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          failter-called (atom nil)]
      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})
                    reports/process-and-write-csv-report! (constantly nil)]
        (evaluate/evaluate! ctx {:generation 0
                                 :name "my-test"
                                 :inputs (.getCanonicalPath inputs-dir)
                                 :judge-model "judge-from-cli"})
        (is (some? @failter-called) "failter/run-contest! should have been called.")
        (is (= "judge-from-cli" (:judge-model @failter-called)))))))


(deftest evaluate-validation-contest-exists-test
  (testing "Validation fails if contest directory already exists"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          failter-called (atom nil)]
      (.mkdirs (expdir/get-contest-dir ctx 0 "existing-contest"))

      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:generation 0 :name "existing-contest" :inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called) "failter/run-contest! should not be called.")))))

(deftest evaluate-validation-empty-population-test
  (testing "Validation fails if the target population is empty"
    (let [ctx (th-cmd/setup-bootstrapped-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          failter-called (atom nil)]
      ;; Create a new, empty generation to test against
      (expdir/create-experiment-dirs! ctx)
      (spit (io/file (th-generic/get-temp-dir) "evolution-parameters.edn")
            (pr-str {:evaluate {:models ["a"]}}))
      (let [pop-dir (expdir/get-population-dir ctx 1)]
        (.mkdirs pop-dir))

      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:generation 1 :inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called) "failter/run-contest! should not be called for empty population.")))))
