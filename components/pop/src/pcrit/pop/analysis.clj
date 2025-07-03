(ns pcrit.pop.analysis
(:require [clojure.string :as str]))

(defn analyze-prompt-body
  "Analyzes a prompt's body string to compute basic metadata.
  Accepts a single string (the prompt body) and returns a map containing:
  :character-count: The total character count of the string.
  :word-count: A best-effort count of words, splitting by whitespace.
  :template-field-names: A vector of strings, where each string is the
  name of a '{{FIELD_NAME}}' placeholder found in the prompt."
  [prompt-body]
  (let [trimmed-body (str/trim prompt-body)
        word-count (if (str/blank? trimmed-body)
                     0
                     (count (str/split trimmed-body #"\s+")))
        ;; Regex explained:
        ;; {{ -> Match the literal opening '{{'
        ;; (.+?) -> Lazily capture one or more of any character (the field name)
        ;; }} -> Match the literal closing '}}'
        field-names (vec (map second (re-seq #"\{\{(.+?)\}\}" prompt-body)))]
    {:character-count (count prompt-body)
     :word-count word-count
     :template-field-names field-names}))

(comment
  ;; Example Usage:
  (analyze-prompt-body "Hello {{NAME}}, how are you today? This is a test for {{PLACE}}.")
  ;; Expected Output:
  ;; {:character-count 62,
  ;; :word-count 12,
  ;; :template-field-names ["NAME" "PLACE"]}
  (analyze-prompt-body "A prompt with no fields.")
  ;; Expected Output:
  ;; {:character-count 24,
  ;; :word-count 5,
  ;; :template-field-names []}
  (analyze-prompt-body " \n \t ")
  ;; Expected Output:
  ;; {:character-count 6,
  ;; :word-count 0,
  ;; :template-field-names []}
)
