(ns pcrit.pdb.io
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.util :as util]
            [pcrit.log :as log])
  (:import [java.io File]))

(def ^:private front-matter-regex #"(?s)^---\n(.*?)\n---\n(.*)$")

(defn- split-front-matter
  "Splits raw file content into a map of {:yaml-str, :body-raw}.
  If no front matter is found, :yaml-str will be nil."
  [raw-content]
  (if-let [match (re-find front-matter-regex raw-content)]
    (let [[_ yaml-str body-raw] match]
      {:yaml-str yaml-str :body-raw body-raw})
    {:yaml-str nil :body-raw raw-content}))

(defn- parse-header
  "Parses a YAML string into a Clojure map. Handles nil/blank strings
  and logs a warning on parsing failure, returning an empty map."
  [yaml-str ^File source-file]
  (if (str/blank? yaml-str)
    {}
    (try
      (yaml/parse-string yaml-str)
      (catch Exception e
        (log/warn "Failed to parse YAML front matter in" (.getPath source-file) "Error:" (.getMessage e))
        {}))))

(defn ->prompt-path
  "Returns a File object pointing to the prompt file."
  ^File [db-dir id]
  (io/file db-dir (str id ".prompt")))

(defn parse-prompt-file
  "Reads and parses a prompt file from disk.
  Returns a prompt record map or nil if the file doesn't exist."
  [^File f]
  (when (.exists f)
    (let [content (slurp f)
          ;; Line endings must be normalized before regex splitting.
          content-lf (util/normalize-line-endings content)
          {:keys [yaml-str body-raw]} (split-front-matter content-lf)
          header (parse-header yaml-str f)
          body (util/canonicalize-text body-raw)]
      {:header header :body body})))

(defn write-prompt-record!
  "Writes a prompt record map to the given file."
  [^File f record]
  (let [{:keys [header body]} record
        header-str (if (seq header)
                     (str "---\n" (yaml/generate-string header) "---\n")
                     "")]
    (spit f (str header-str body))))
