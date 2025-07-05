(ns pcrit.pop.analysis-test
  (:require [clojure.test :refer (deftest testing is)]
            [pcrit.pop.analysis :as analysis]))

(deftest analyze-prompt-body-test
  (testing "Correctly identifies an object-prompt"
    (let [prompt "Summarize this document: {{INPUT_TEXT}}"
          result (analysis/analyze-prompt-body prompt)]
      (is (= :object-prompt (:prompt-type result)))
      (is (= ["INPUT_TEXT"] (:template-field-names result)))
      (is (= (count prompt) (:character-count result)))
      (is (= 4 (:word-count result)))))

  (testing "Correctly identifies a meta-prompt"
    (let [prompt "Make this better: {{OBJECT_PROMPT}}"
          result (analysis/analyze-prompt-body prompt)]
      (is (= :meta-prompt (:prompt-type result)))
      (is (= ["OBJECT_PROMPT"] (:template-field-names result)))))

  (testing "Correctly identifies a static-prompt with no template fields"
    (let [prompt "This is just a static string."
          result (analysis/analyze-prompt-body prompt)]
      (is (= :static-prompt (:prompt-type result)))
      (is (empty? (:template-field-names result)))))

  (testing "Correctly identifies a static-prompt with other, non-special fields"
    (let [prompt "Hello, {{NAME}}! Welcome to {{PLACE}}."
          result (analysis/analyze-prompt-body prompt)]
      (is (= :static-prompt (:prompt-type result)))
      (is (= ["NAME" "PLACE"] (:template-field-names result)))))

  (testing "Correctly identifies an invalid prompt with mixed special fields"
    (let [prompt "This is invalid: {{INPUT_TEXT}} and {{OBJECT_PROMPT}}."
          result (analysis/analyze-prompt-body prompt)]
      (is (= :invalid-mixed-type (:prompt-type result)))
      (is (= #{"INPUT_TEXT" "OBJECT_PROMPT"} (set (:template-field-names result))))))

  (testing "Handles empty string gracefully"
    (let [result (analysis/analyze-prompt-body "")]
      (is (= :static-prompt (:prompt-type result)))
      (is (= 0 (:character-count result)))
      (is (= 0 (:word-count result)))
      (is (empty? (:template-field-names result)))))

  (testing "Handles string with only whitespace"
    (let [result (analysis/analyze-prompt-body " \n \t ")]
      (is (= :static-prompt (:prompt-type result)))
      (is (= 5 (:character-count result)))
      (is (= 0 (:word-count result)))
      (is (empty? (:template-field-names result))))))
