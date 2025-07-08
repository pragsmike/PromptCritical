(ns pcrit.command.interface
  (:require [pcrit.command.core :as core]
            [pcrit.command.vary :as vary]))

(def init! core/init!)

(def bootstrap!
  "Executes the full bootstrap process for an experiment.
  Accepts a single `ctx` map containing the `:exp-dir`."
  core/bootstrap!)

(def vary!
  "Creates a new generation of prompts by applying meta-prompts to the current population."
  vary/vary!)
