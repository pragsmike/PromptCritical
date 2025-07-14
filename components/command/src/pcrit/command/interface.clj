(ns pcrit.command.interface
  (:require [pcrit.command.core :as core]
            [pcrit.command.init :as init]
            [pcrit.command.stats :as stats]
            [pcrit.command.vary :as vary]
            [pcrit.command.evaluate :as evaluate]
            [pcrit.command.select :as select]
            [pcrit.command.evolve :as evolve]))

(def setup!
  "Performs application-wide setup, like logging and pre-flight checks."
  core/setup!)

(def init!
  "Creates a new, minimal experiment skeleton directory."
  init/init!)

(def stats!
  "Calculates and displays statistics for a given contest or generation."
  stats/stats!)

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

(def evolve!
  "Automates the `vary -> evaluate -> select` loop for N generations."
  evolve/evolve!)
