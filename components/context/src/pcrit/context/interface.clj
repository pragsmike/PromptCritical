(ns pcrit.context.interface
  (:require [pcrit.context.core :as core]))

(def new-context
  "Constructor for the AppContext.
  Usage: (new-context \"/path/to/experiment\")"
  core/new-context)
