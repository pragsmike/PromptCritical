(ns pcrit.command.vary-test
  (:require [clojure.test :refer :all]
            [pcrit.command.vary :as vary]
            [pcrit.expdir.interface :as expdir]
            [pcrit.llm.interface :as llm]
            [pcrit.pop.interface :as pop]
            ;; Import helpers
            [pcrit.command.test-helper :as th-cmd]
            [pcrit.test-helper.interface :as th-generic]))

(use-fixtures :each th-generic/with-temp-dir th-generic/with-quiet-logging)

(deftest vary-command-test
  (let [;; Use the new shared helper to create a bootstrapped experiment
        ctx (th-cmd/setup-bootstrapped-exp! (th-generic/get-temp-dir))
        mock-llm-response {:content "New offspring content"
                           :generation-metadata {:provider          :mock-provider
                                                 :model             "mock-provider/mock-model"
                                                 :token-in          120
                                                 :token-out         30
                                                 :cost-usd-snapshot 0.0025
                                                 :duration-ms       750}}
        mock-sender-fn (fn [_model-name _prompt-body] mock-llm-response)
        mock-template-caller (fn [model-name template vars] (llm/call-model-template model-name template vars mock-sender-fn))]

    (testing "Pre-condition: gen-0 exists and has 1 member"
      (is (= 0 (expdir/find-latest-generation-number ctx)))
      (is (= 1 (count (pop/load-population ctx 0)))))

    (testing "vary! adds offspring with full generation metadata to its header"
      (let [{:keys [cost offspring-created]} (vary/vary! ctx {:call-template-fn mock-template-caller})]

        (is (= 1 offspring-created))
        (is (< (Math/abs (- cost 0.0025)) 1e-9))

        (let [varied-pop (pop/load-population ctx 0)
              pop-headers (map :header varied-pop)
              offspring-header (first (filter :parents pop-headers))]

          (is (some? offspring-header) "An offspring with parent metadata should exist.")
          (is (= ["P1"] (:parents offspring-header)) "Offspring should list P1 as its parent.")

          (is (= "mock-provider" (:provider offspring-header)))
          (is (= "mock-provider/mock-model" (:model offspring-header)))
          (is (= 120 (:token-in offspring-header)))
          (is (= 30 (:token-out offspring-header)))
          (is (= 0.0025 (:cost-usd-snapshot offspring-header)))
          (is (= 750 (:duration-ms offspring-header)))

          (is (some? (get-in offspring-header [:generator :model])) "Generator block should still exist."))))))
