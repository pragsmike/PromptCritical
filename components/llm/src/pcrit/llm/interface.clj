(ns pcrit.llm.interface
  (:require [pcrit.llm.core :as core]))

(def pre-flight-checks core/pre-flight-checks)

(def call-model
  "Calls an LLM with a model name and prompt string.
  Accepts optional kwargs:
  :timeout - request timeout in ms
  :post-fn - a function to use for the HTTP POST (for testing)"
  core/call-model)

(def parse-llm-response
  "Parses the raw JSON string body from an LLM response."
  core/parse-llm-response)
