(ns pcrit.log.interface
  (:require [pcrit.log.core :as core]))

(defmacro info [& args]  `(core/info ~@args))
(defmacro warn [& args]  `(core/warn ~@args))
(defmacro error [& args]  `(core/error ~@args))
(defmacro debug [& args]  `(core/debug ~@args))

(defn setup-logging! [] (core/setup-logging!))

