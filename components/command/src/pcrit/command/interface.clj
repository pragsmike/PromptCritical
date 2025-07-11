(ns pcrit.command.interface
  (:require [pcrit.command.core :as core]
            [pcrit.command.vary :as vary]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.command.select :as select]))

(def init! core/init!)

(def bootstrap!
  "Executes the full bootstrap process for an experiment.
  Accepts a single `ctx` map containing the `:exp-dir`."
  core/bootstrap!)

(def vary!
  "Creates a new generation of prompts by applying meta-prompts to the current population."
  vary/vary!)

(def evaluate!
  "Orchestrates the evaluation of a prompt population."
  evaluate/evaluate!)

(def select!
  "Selects survivors from a contest and creates a new generation."
  select/select!)
