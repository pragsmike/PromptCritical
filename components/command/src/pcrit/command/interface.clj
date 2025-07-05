(ns pcrit.command.interface
  (:require [pcrit.command.core :as core]))

(def init! core/init!)

(def bootstrap!
  "Executes the full bootstrap process for an experiment."
  core/bootstrap!)
