(ns pcrit.llm.interface
  (:require [pcrit.llm.core :as core]
            [pcrit.llm.templater :as templater]))

(def pre-flight-checks core/pre-flight-checks)

(def call-model
  "Calls an LLM with a model name and prompt string."
  core/call-model)

(def call-model-template
  "Renders a template with vars and calls an LLM.
  Accepts an optional 4th argument, `llmfunc`, for testing."
  templater/call-model-template)

(def parse-llm-response
  "Parses the raw JSON string body from an LLM response."
  core/parse-llm-response)
