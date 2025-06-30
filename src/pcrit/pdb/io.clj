(ns pcrit.pdb.io
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.util :as util]
            [pcrit.log :as log])
  (:import [java.io File]
           [java.nio.file Files CopyOption StandardCopyOption AtomicMoveNotSupportedException OpenOption StandardOpenOption]
           [java.nio.channels FileChannel]))

(def ^:private front-matter-regex #"(?s)^---\n(.*?)\n?---\n(.*)$")

(defn- fsync! [^File f]
  (with-open [channel (FileChannel/open (.toPath f) (into-array OpenOption [StandardOpenOption/WRITE]))]
    (.force channel true)))

(defn atomic-write-file!
  "Writes content to a destination file atomically.
  Writes to a temp file, then performs an atomic move. Falls back to a
  non-atomic move with fsync if ATOMIC_MOVE is not supported."
  [^File dest-file content]
  (let [temp-file (io/file (.getParentFile dest-file) (str "." (.getName dest-file) ".new"))]
    (spit temp-file content)
    (try
      (Files/move (.toPath temp-file) (.toPath dest-file) (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
      (catch AtomicMoveNotSupportedException _
        (log/warn "ATOMIC_MOVE not supported on this filesystem. Falling back to non-atomic move for " (.getName dest-file))
        (Files/move (.toPath temp-file) (.toPath dest-file) (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))
        (fsync! dest-file))
      (catch Exception e
        (log/error "Failed to move temp file" (.getPath temp-file) "to" (.getPath dest-file))
        (io/delete-file temp-file)
        (throw e)))))

(defn- prompt-record->string [record]
  (let [{:keys [header body]} record
        header-str (if (seq header)
                     (str "---\n"
                          (yaml/generate-string header :dumper-options {:flow-style :block})
                          "---\n")
                     "")
        canonical-header (util/canonicalize-text header-str)]
    (str canonical-header body)))

(defn write-prompt-record-atomically!
  "Converts a prompt record to its string representation and writes it atomically to the given file."
  [^File f record]
  (let [content (prompt-record->string record)]
    (atomic-write-file! f content)))

(defn- split-front-matter
  [raw-content]
  (if-let [match (re-find front-matter-regex raw-content)]
    (let [[_ yaml-str body-raw] match]
      {:yaml-str yaml-str :body-raw body-raw})
    {:yaml-str nil :body-raw raw-content}))

(defn- parse-header
  [yaml-str ^File source-file]
  (if (str/blank? yaml-str)
    {}
    (try
      (yaml/parse-string yaml-str)
      (catch Exception e
        (log/warn "Failed to parse YAML front matter in" (.getPath source-file) "Error:" (.getMessage e))
        {}))))

(defn ->prompt-path
  ^File [db-dir id]
  (io/file db-dir (str id ".prompt")))

(defn parse-prompt-file
  [^File f]
  (when (.exists f)
    (let [content (slurp f)
          content-lf (util/normalize-line-endings content)
          {:keys [yaml-str body-raw]} (split-front-matter content-lf)
          header (parse-header yaml-str f)
          body (util/canonicalize-text body-raw)]
      {:header header :body body})))
