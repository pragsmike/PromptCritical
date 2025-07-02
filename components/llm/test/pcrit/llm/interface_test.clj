(ns pcrit.llm.interface-test
  (:require [clojure.test :as test :refer :all]
            [pcrit.llm.templater :as templater :refer (expand call-model-template)]))


(deftest templater-test
  (is (= "1 is not 2" (expand "{{a}} is not {{b}}" {:a 1 :b 2}))))

(deftest call-template-test
  (let [s (atom nil)
        m (atom nil)]
    (call-model-template "mistral" "{{a}} is not {{b}}" {:a 1 :b 2}
                         (fn [mn sn] (reset! m mn) (reset! s sn)))
    (is (= @m "mistral"))
    (is (= @s "1 is not 2"))
    ))
