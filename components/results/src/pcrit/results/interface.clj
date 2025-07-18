(ns pcrit.results.interface
  (:require [pcrit.results.core :as core]))

(def parse-report
  "Reads and parses the failter-report.json for a given contest.
  This is the new source of truth for contest results. It calculates cost
  and returns a sequence of canonical result maps.

  Usage: (parse-report ctx generation-number contest-name)
  Returns a sequence of maps, e.g.,
  '({:prompt \"P1\", :score 95.5, :cost 0.0012, ...})'
  or an empty sequence if the report is not found or is corrupt."
  core/parse-report)
