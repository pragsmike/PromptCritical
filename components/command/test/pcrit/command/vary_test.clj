(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcrit.command.vary :as vary]
            [pcrit.expdir.interface :as expdir]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.interface :as pop]
            [pcrit.command.test-helper :as th-cmd]
            [pcrit.test-helper.interface :as th-generic]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

(def ^:private mock-llm-response
  {:content "New offspring content"
   :generation-metadata {:provider          :mock-provider
                         :model             "mock-provider/mock-model"
                         :token-in          120
                         :token-out         30
                         :cost-usd-snapshot 0.0025
                         :duration-ms       750}})

(def ^:private mock-sender-fn
  (fn [_model-name _prompt-body] mock-llm-response))

(def ^:private mock-template-caller
  (fn [model-name template vars] (llm/call-model-template model-name template vars mock-sender-fn)))

(deftest vary-command-default-strategy-test
  (testing "vary! with no config uses the default :refine strategy successfully"
    (let [ctx (th-cmd/setup-bootstrapped-exp! (th-generic/get-temp-dir))]
      (is (= 0 (expdir/find-latest-generation-number ctx)) "Precondition: gen-0 should exist.")
      (is (= 1 (count (pop/load-population ctx 0))) "Precondition: gen-0 should have one member.")

      (let [{:keys [offspring-created]} (vary/vary! ctx {:call-template-fn mock-template-caller})]
        (is (= 1 offspring-created))

        (let [varied-pop (pop/load-population ctx 0)
              pop-headers (map :header varied-pop)
              offspring-header (first (filter :parents pop-headers))]

          (is (some? offspring-header) "An offspring with parent metadata should exist.")
          (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")
          ;; CORRECTED ASSERTION: The test now checks for the correct default model.
          (is (= "openai/gpt-4o-mini" (get-in offspring-header [:generator :model]))))))))

(deftest vary-command-configured-model-test
  (testing "vary! uses the model specified in evolution-parameters.edn"
    (let [exp-dir (th-generic/get-temp-dir)
          ctx (th-cmd/setup-bootstrapped-exp! exp-dir)
          config-file (io/file exp-dir "evolution-parameters.edn")]
      (spit config-file (pr-str {:vary {:model "my-configured-model"}}))

      (vary/vary! ctx {:call-template-fn mock-template-caller})

      (let [offspring-header (->> (pop/load-population ctx 0)
                                  (map :header)
                                  (filter :parents)
                                  first)]
        (is (= "my-configured-model" (get-in offspring-header [:generator :model])))))))


(deftest vary-command-with-unknown-strategy-test
  (testing "vary! with an unknown strategy in config logs a warning and creates no offspring"
    (let [exp-dir (th-generic/get-temp-dir)
          ctx (th-cmd/setup-bootstrapped-exp! exp-dir)
          ;; Add a config file specifying an unknown strategy
          config-file (io/file exp-dir "evolution-parameters.edn")]
      (spit config-file (pr-str {:vary {:strategy :hallucinate}}))

      (let [{:keys [cost offspring-created]} (vary/vary! ctx {:call-template-fn mock-template-caller})]
        (is (zero? offspring-created) "No offspring should be created with an unknown strategy.")
        (is (zero? cost) "Cost should be zero when no offspring are created.")))))
