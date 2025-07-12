(ns pcrit.config.interface
  (:require [pcrit.config.core :as core]))

(def defaults
  "A map of application-wide default values."
  core/defaults)

(def price-table
  "A map of model names to their cost per 1000 tokens for input and output."
  core/price-table)

(def config
  "A centralized map for all application configuration."
  core/config)

(def load-evolution-params
  "Reads evolution-parameters.edn from the experiment directory specified in the ctx.
  Returns a map of parameters, or an empty map if the file is not found or corrupt."
  core/load-evolution-params)
