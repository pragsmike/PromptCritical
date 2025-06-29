(ns pcrit.pdb
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.util :as util]
            [pcrit.log :as log])
  (:import (java.io File)
           (java.time Instant)
           (java.nio.file Files CopyOption StandardCopyOption)))

;;; --- Private Helper Functions ---

(defn- ->prompt-path ^File [db-dir id]
  (io/file db-dir (str id ".prompt")))

(defn- parse-prompt-file [^File f]
  (when (.exists f)
    (let [content (slurp f)]
      (if (str/starts-with? content "---")
        (let [parts (str/split content #"\n---\n" 2)
              yaml-str (second (str/split (first parts) #"---\n" 2))
              body (get parts 1 "")]
          {:header (if (seq yaml-str)
                     (try
                       (yaml/parse-string yaml-str)
                       (catch Exception e
                         (log/warn "Failed to parse YAML front matter in" (.getPath f) "Error:" (.getMessage e))
                         {}))
                     {})
           :body body})
        {:header {} :body content}))))

(defn- write-prompt-record! [^File f record]
  (let [{:keys [header body]} record
        header-str (if (seq header)
                     (str "---\n" (yaml/generate-string header) "---\n")
                     "")]
    ;; Body is assumed to be canonical already.
    (spit f (str header-str body))))

(defonce ^:private id-counter-lock (Object.))

(defn- get-next-id!
  "Atomically reads and increments a counter file (e.g., `<db-dir>/pdb.counter`)."
  [db-dir]
  (locking id-counter-lock
    (let [counter-file (io/file db-dir "pdb.counter")]
      (try
        (let [next-id (if (.exists counter-file)
                        (Integer/parseInt (slurp counter-file))
                        1)]
          (spit counter-file (str (inc next-id)))
          (str "P" next-id))
        (catch NumberFormatException e
          (log/error "pdb.counter file is corrupt. Could not parse integer.")
          (throw (ex-info "Corrupt counter file" {:file counter-file} e)))
        (catch Exception e
          (log/error "Could not read or update prompt ID counter file.")
          (throw (ex-info "Failed to generate new prompt ID" {:file counter-file} e)))))))

(defn- execute-with-lock [db-dir id lock-fn]
  (let [prompt-file (->prompt-path db-dir id)
        lock-file (io/file (.getParentFile prompt-file) (str (.getName prompt-file) ".lock"))]
    (when-not (.exists prompt-file)
      (throw (ex-info (str "Prompt " id " not found.") {:id id :db-dir db-dir})))

    (try
      (when-not (.createNewFile lock-file)
        (throw (ex-info (str "Could not acquire lock for prompt " id ". It may be busy.")
                        {:id id :lock-file lock-file})))
      (lock-fn)
      (finally
        (.delete lock-file)))))

;;; --- Public API ---

(defn read-prompt
  "Reads the prompt with the given id from the database directory.
   Returns a prompt record map. Verifies the SHA1 hash upon reading
   and logs a warning if it doesn't match.
   Returns nil if the prompt file does not exist."
  [db-dir id]
  (let [f (->prompt-path db-dir id)]
    (when-let [record (parse-prompt-file f)]
      (let [expected-hash (get-in record [:header :sha1-hash])
            actual-hash (util/sha1-hex (:body record))] ;; Body from file is already canonical
        (when (and (seq expected-hash) (not= expected-hash actual-hash))
          (log/warn "Checksum mismatch for prompt" id ". Expected:" expected-hash ", Got:" actual-hash))
        record))))

(defn create-prompt
  "Creates a new prompt in the database."
  [db-dir prompt-text & {:keys [metadata] :or {metadata {}}}]
  (let [id (get-next-id! db-dir)
        canonical-body (util/canonicalize-text prompt-text)
        record {:header (merge {:spec-version "1"
                                :id id
                                :created-at (.toString (Instant/now))
                                :sha1-hash (util/sha1-hex canonical-body)}
                               metadata)
                :body canonical-body}]
    (write-prompt-record! (->prompt-path db-dir id) record)
    (log/info "Created new prompt" id "in" db-dir)
    record))

(defn update-metadata
  "Atomically updates the metadata for the prompt with the given id."
  [db-dir id f]
  (execute-with-lock db-dir id
    (fn []
      (let [prompt-path (.toPath (->prompt-path db-dir id))
            new-path    (.toPath (io/file db-dir (str id ".prompt.new")))
            current-record (or (read-prompt db-dir id)
                               (throw (ex-info (str "Prompt " id " disappeared after lock acquisition.") {:id id})))
            new-header (f (:header current-record))
            _ (when (not= (:id new-header) id)
                (throw (ex-info "Updating metadata must not change the prompt 'id'." {:id id})))
            _ (when (not= (:sha1-hash new-header) (get-in current-record [:header :sha1-hash]))
                (throw (ex-info "Updating metadata must not change the 'sha1-hash'." {:id id})))
            updated-record (assoc current-record :header new-header)]
        (write-prompt-record! (.toFile new-path) updated-record)
        (Files/move new-path prompt-path (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
        updated-record))))
