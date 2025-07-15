(ns pcrit.command.evaluate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.expdir.interface :as expdir]
            [pcrit.failter.interface :as failter]
            [pcrit.reports.interface :as reports]
            [pcrit.command.test-helper :as th-cmd]
            [pcrit.test-helper.interface :as th-generic]
            [taoensso.telemere :as tel]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

(def ^:private mock-failter-parsed-json [])

(deftest evaluate-happy-path-test
  (testing "evaluate! with all options calls failter and reports correctly"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          _ (spit (io/file inputs-dir "doc1.txt") "content")
          failter-called (atom nil)
          reports-called (atom nil)]
      (with-redefs [failter/run-contest! (fn [_ctx params]
                                           (reset! failter-called params)
                                           {:success true :parsed-json mock-failter-parsed-json})
                    reports/process-and-write-csv-report! (fn [parsed-data path]
                                                            (reset! reports-called {:data parsed-data :path path}))]
        (evaluate/evaluate! ctx {:generation 0
                                 :name "my-test"
                                 :inputs (.getCanonicalPath inputs-dir)})

        (is (some? @failter-called))
        (is (= ["mock-model"] (:models @failter-called)))
        (is (= "mock-judge" (:judge-model @failter-called)))

        (is (some? @reports-called))
        (is (= mock-failter-parsed-json (:data @reports-called)))
        (is (.endsWith (:path @reports-called) (str "my-test" java.io.File/separator "report.csv")))))))


(deftest evaluate-validation-tests
  (testing "Validation fails if contest directory already exists"
    (let [ctx (th-cmd/setup-configured-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          _ (spit (io/file inputs-dir "doc1.txt") "content")
          failter-called (atom nil)]
      (.mkdirs (expdir/get-contest-dir ctx 0 "existing-contest"))
      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:generation 0 :name "existing-contest" :inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called)))))

  (testing "Validation fails if the target population is empty"
    (let [ctx (th-cmd/setup-bootstrapped-exp! (th-generic/get-temp-dir))
          inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
          _ (spit (io/file inputs-dir "doc1.txt") "content")
          failter-called (atom nil)]
      (spit (io/file (th-generic/get-temp-dir) "evolution-parameters.edn")
            (pr-str {:evaluate {:models ["a"] :judge-model "b"}}))
      (.mkdirs (expdir/get-population-dir ctx 1))
      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:generation 1 :inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called)))))

  (testing "Validation fails if no judge-model is provided"
    (let [exp-dir (th-generic/get-temp-dir)
          ctx (th-cmd/setup-bootstrapped-exp! exp-dir)
          inputs-dir (doto (io/file exp-dir "inputs") .mkdirs)
          _ (spit (io/file inputs-dir "doc1.txt") "content")
          failter-called (atom nil)]
      (spit (io/file exp-dir "evolution-parameters.edn")
            (pr-str {:evaluate {:models ["a-model"]}}))
      (with-redefs [failter/run-contest! (fn [_ p] (reset! failter-called p) {:success true})]
        (evaluate/evaluate! ctx {:generation 0 :name "a-contest" :inputs (.getCanonicalPath inputs-dir)})
        (is (nil? @failter-called))))))


(deftest check-and-warn-test
  (let [check-and-warn! (ns-resolve 'pcrit.command.evaluate 'check-and-warn!)]
    (testing "Warns about empty inputs directory"
      (let [inputs-dir (doto (io/file (th-generic/get-temp-dir) "empty-inputs") .mkdirs)
            captured (atom [])]
        (tel/with-handler :capture
          (fn [signal] (swap! captured conj signal))
          {}
          (check-and-warn! (.getCanonicalPath inputs-dir) ["some-model"]))
        (let [warn-log (first (filter #(= :warn (:level %)) @captured))]
          (is (some? warn-log))
          (is (.startsWith (:msg_ warn-log) "Inputs directory is empty")))))

    (testing "Warns about unknown model name"
      (let [inputs-dir (doto (io/file (th-generic/get-temp-dir) "inputs") .mkdirs)
            _ (spit (io/file inputs-dir "doc.txt") "content")
            captured (atom [])]
        (tel/with-handler :capture
          (fn [signal] (swap! captured conj signal))
          {}
          (check-and-warn! (.getCanonicalPath inputs-dir) ["not-a-real-model"]))
        (let [warn-log (first (filter #(= :warn (:level %)) @captured))]
          (is (some? warn-log))
          (is (.contains (:msg_ warn-log) "not a known model")))))))
