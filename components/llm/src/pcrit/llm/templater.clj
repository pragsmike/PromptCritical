(ns pcrit.llm.templater
  (:require [selmer.parser :as selmer]
            [pcrit.llm.core :refer (call-model)]))

(defn expand [template vars-map]
  (selmer/render template vars-map)
  )


(defn call-model-template
  ([model-name prompt-template-string vars-map]
   (call-model-template model-name prompt-template-string vars-map call-model))
  ([model-name prompt-template-string vars-map llmfunc]

   (llmfunc model-name (expand prompt-template-string vars-map)) ))
