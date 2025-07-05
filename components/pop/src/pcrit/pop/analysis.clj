(ns pcrit.pop.analysis
  (:require [clojure.string :as str]))

(defn- infer-prompt-type [field-names-set]
  (let [has-input?    (contains? field-names-set "INPUT_TEXT")
        has-object?   (contains? field-names-set "OBJECT_PROMPT")]
    (cond
      (and has-input? has-object?) :invalid-mixed-type
      has-input?                   :object-prompt
      has-object?                  :meta-prompt
      :else                        :static-prompt)))

(defn analyze-prompt-body
  "Analyzes a prompt's body string to compute basic metadata.
  Returns a map containing:
  :character-count, :word-count, :template-field-names, and :prompt-type"
  [prompt-body]
  (let [trimmed-body (str/trim prompt-body)
        word-count (if (str/blank? trimmed-body)
                     0
                     (count (str/split trimmed-body #"\s+")))
        field-names (vec (map second (re-seq #"\{\{(.+?)\}\}" prompt-body)))
        field-names-set (set field-names)]
    {:character-count      (count prompt-body)
     :word-count           word-count
     :template-field-names field-names
     :prompt-type          (infer-prompt-type field-names-set)}))

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
