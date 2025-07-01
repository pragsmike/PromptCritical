(ns pcrit.llm.interface
  (:require [pcrit.llm.core :as core]))

(def pre-flight-checks core/pre-flight-checks)
(def call-model core/call-model)

