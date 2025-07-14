(ns pcrit.command.evaluate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.experiment.interface :as exp]
            [pcrit.expdir.interface :as expdir]
            [pcrit.failter.interface :as failter]
            [pcrit.pop.interface :as pop]
            [pcrit.reports.interface :as reports]
            [pcrit.test-helper.interface :refer [with-temp-dir get-temp-dir with-quiet-logging]]))

(use-fixtures :each with-temp-dir with-quiet-logging)

(def ^:private mock-failter-json-success "[]")

(defn- setup-basic-experiment
  "Sets up a minimal, valid experiment with gen-0 and one prompt."
  [ctx]
  (expdir/create-experiment-dirs! ctx)
  (let [p1 (pop/ingest-prompt ctx "An object prompt {{INPUT_TEXT}}")]
    (pop/create-new-generation! ctx [p1])))

(deftest evaluate-happy-path-test
  (testing "evaluate! with all options calls failter and reports correctly"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          failter-called (atom nil)
          reports-called (atom nil)]
      (setup-basic-experiment ctx)
      (spit (io/file exp-dir "evolution-parameters.edn")
            (pr-str {:evaluate {:models ["model-from-config"]
                                :judge-model "judge-from-config"}}))

      (with-redefs [failter/run-contest! (fn [_ctx params]
                                           (reset! failter-called params)
                                           {:success true :json-report mock-failter-json-success})
                    reports/process-and-write-csv-report! (fn [json-str path]
                                                            (reset! reports-called {:json json-str :path path}))]
        (evaluate/evaluate! ctx {:generation 0
                                 :name "my-test"
                                 :inputs (.getCanonicalPath inputs-dir)})

        (is (some? @failter-called) "failter/run-contest! should have been called.")
        (is (= ["model-from-config"] (:models @failter-called)))
        (is (= "judge-from-config" (:judge-model @failter-called)))

        (is (some? @reports-called) "reports/process-and-write-csv-report! should have been called.")
        (is (= mock-failter-json-success (:json @reports-called)))
        (is (.endsWith (:path @reports-called) (str "my-test" java.io.File/separator "report.csv")))))))

(deftest evaluate-cli-override-test
  (testing "evaluate! uses CLI judge-model to override config value"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          failter-called (atom nil)]
      (setup-basic-experiment ctx)
      (spit (io/file exp-dir "evolution-parameters.edn")
            (pr-str {:evaluate {:models ["a-model"] :judge-model "judge-from-config"}}))

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
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          failter-called (atom nil)]
      (setup-basic-experiment ctx)
      (spit (io/file exp-dir "evolution-parameters.edn") (pr-str {:evaluate {:models ["a"]}}))
      (.mkdirs (expdir/get-contest-dir ctx 0 "existing-contest"))

      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:generation 0 :name "existing-contest" :inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called) "failter/run-contest! should not be called.")))))

(deftest evaluate-validation-empty-population-test
  (testing "Validation fails if the target population is empty"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          failter-called (atom nil)]
      (expdir/create-experiment-dirs! ctx)
      (pop/create-new-generation! ctx []) ; Creates gen-0, which is empty
      (spit (io/file exp-dir "evolution-parameters.edn") (pr-str {:evaluate {:models ["a"]}}))

      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:generation 0 :inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called) "failter/run-contest! should not be called for empty population.")))))

(deftest evaluate-validation-no-models-test
  (testing "Validation fails if no models are configured"
    (let [exp-dir (get-temp-dir)
          ctx (exp/new-experiment-context exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          failter-called (atom nil)]
      (setup-basic-experiment ctx)
      (spit (io/file exp-dir "evolution-parameters.edn")
            (pr-str {:evaluate {:judge-model "j1"}}))

      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called) "failter/run-contest! should not be called.")))))
