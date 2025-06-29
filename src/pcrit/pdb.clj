(ns pcrit.pdb
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.util :as util]
            [pcrit.log :as log])
  (:import (java.io File)
           (java.time Instant)
           (java.nio.file Files Path CopyOption StandardCopyOption)))

;;; --- Private Helper Functions ---

(defn- ->prompt-path ^File [db-dir id]
  (io/file db-dir (str id ".prompt")))

(defn- canonicalize-body [s]
  (let [s' (if (nil? s) "" s)
        s'' (-> s' (str/replace #"\r\n" "\n") (str/replace #"\r" "\n"))]
    (if (str/ends-with? s'' "\n")
      s''
      (str s'' "\n"))))

(defn- parse-prompt-file [^File f]
  (when (.exists f)
    (with-open [rdr (io/reader f)]
      (let [lines (doall (line-seq rdr))
            [yaml-lines body-lines]
            (if (and (seq lines) (str/starts-with? (first lines) "---"))
              (let [parts (split-with #(not= "---" %) (rest lines))]
                [(first parts) (rest (second parts))])
              [nil lines])

            header (if (seq yaml-lines)
                     (try
                       (yaml/parse-string (str/join "\n" yaml-lines))
                       (catch Exception e
                         (log/warn "Failed to parse YAML front matter in" (.getPath f) "Error:" (.getMessage e))
                         {}))
                     {})
            body (str/join "\n" body-lines)]
        {:header header
         :body body}))))

(defn- write-prompt-record! [^File f record]
  (let [{:keys [header body]} record
        canonical-body (canonicalize-body body)
        header-str (if (seq header)
                     (str "---\n" (yaml/generate-string header) "---\n")
                     "")]
    (spit f (str header-str canonical-body))))

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
            actual-hash (util/canonical-sha1 (:body record))]
        (when (and (seq expected-hash) (not= expected-hash actual-hash))
          (log/warn "Checksum mismatch for prompt" id ". Expected:" expected-hash ", Got:" actual-hash))
        record))))

(defn create-prompt
  "Creates a new prompt in the database."
  [db-dir prompt-text & {:keys [metadata] :or {metadata {}}}]
  (let [id (get-next-id! db-dir)
        record {:header (merge {:spec-version "1"
                                :id id
                                :created-at (.toString (Instant/now))
                                :sha1-hash (util/canonical-sha1 prompt-text)}
                               metadata)
                :body prompt-text}
        f (->prompt-path db-dir id)]
    (write-prompt-record! f record)
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
                         #_(Files/move new-path prompt-path StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING)
                         (Files/move new-path prompt-path (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
                         updated-record))))
