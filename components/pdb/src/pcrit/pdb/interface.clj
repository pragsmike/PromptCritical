(ns pcrit.pdb.interface 
  (:require [pcrit.pdb.core :as core]))

(def create-prompt core/create-prompt)
(def read-prompt core/read-prompt)
(def update-metadata core/update-metadata)
