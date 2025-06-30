(ns pcrit.pdb.io
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.util :as util]
            [pcrit.log :as log])
  (:import [java.io File]))

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
          content-lf (util/normalize-line-endings content)]
      (if-not (str/starts-with? content-lf "---")
        {:header {} :body content-lf}
        (if-let [end-delim-idx (str/index-of content-lf "\n---\n" 4)] ; Start search after initial "---"
          (let [yaml-str (subs content-lf 4 end-delim-idx) ; From after "---\n" to before "\n---\n"
                body (subs content-lf (+ end-delim-idx 5))
                body (util/canonicalize-text body)] ; From after "\n---\n"

            (if (str/blank? yaml-str)
              {:header {} :body body}
              (try
                {:header (yaml/parse-string yaml-str)
                 :body body}
                (catch Exception e
                  (log/warn "Failed to parse YAML front matter in" (.getPath f) "Error:" (.getMessage e))
                  {:header {} :body body}))))
          ;; No closing delimiter found, treat as all body
          {:header {} :body content-lf})))))

(defn write-prompt-record!
  "Writes a prompt record map to the given file."
  [^File f record]
  (let [{:keys [header body]} record
        header-str (if (seq header)
                     (str "---\n" (yaml/generate-string header) "---\n")
                     "")]
    (spit f (str header-str body))))
