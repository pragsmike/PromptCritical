(ns pcrit.pdb.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [pcrit.util :as util]
            [pcrit.log :as log]
            [pcrit.pdb.io :as pdb-io]
            [pcrit.pdb.lock :as pdb-lock]
            [pcrit.pdb.id :as pdb-id])
  (:import [java.time Instant]
           [java.nio.file Files CopyOption StandardCopyOption AtomicMoveNotSupportedException]))

(defn read-prompt
  "Reads the prompt with the given id from the database directory."
  [db-dir id]
  (let [f (pdb-io/->prompt-path db-dir id)]
    (when-let [record (pdb-io/parse-prompt-file f)]
      (let [expected-hash (some-> (get-in record [:header :sha1-hash]) str/lower-case)
            actual-hash (util/sha1-hex (:body record))]
        (when (and (seq expected-hash) (not= expected-hash actual-hash))
          (log/warn "Checksum mismatch for prompt" id ". Expected:" expected-hash ", Got:" actual-hash))
        record))))

(defn create-prompt
  "Creates a new prompt in the database."
  [db-dir prompt-text & {:keys [metadata] :or {metadata {}}}]
  (let [id (pdb-id/get-next-id! db-dir)
        canonical-body (util/canonicalize-text prompt-text)
        record {:header (merge {:spec-version "1"
                                :id id
                                :created-at (.toString (Instant/now))
                                :sha1-hash (util/sha1-hex canonical-body)}
                               metadata)
                :body canonical-body}]
    (pdb-io/write-prompt-record! (pdb-io/->prompt-path db-dir id) record)
    (log/info "Created new prompt " id " in " db-dir)
    record))

(defn update-metadata
  "Atomically updates the metadata for the prompt with the given id."
  [db-dir id f]
  (let [prompt-file (pdb-io/->prompt-path db-dir id)
        lock-file (io/file (.getParentFile prompt-file) (str (.getName prompt-file) ".lock"))]
    (when-not (.exists prompt-file)
      (throw (ex-info (str "Prompt " id " not found.") {:id id :db-dir db-dir})))

    (pdb-lock/execute-with-lock lock-file
      (fn []
        (let [new-path (.toPath (io/file db-dir (str id ".prompt.new")))
              ;; IMPORTANT: Read inside the lock to get the most current state.
              current-record (or (read-prompt db-dir id)
                                 (throw (ex-info (str "Prompt " id " disappeared after lock acquisition.") {:id id})))
              new-header (f (:header current-record))
              _ (when-not (and (= (get new-header :id) id)
                               (= (get new-header :sha1-hash) (get-in current-record [:header :sha1-hash])))
                  (throw (ex-info "Updater function MUST NOT remove or change :id or :sha1-hash."
                                  {:id id :old-header (:header current-record) :new-header new-header})))
              updated-record (assoc current-record :header new-header)]
          (pdb-io/write-prompt-record! (.toFile new-path) updated-record)
          (try
            (Files/move new-path (.toPath prompt-file) (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
            (catch AtomicMoveNotSupportedException _
              (log/warn "ATOMIC_MOVE not supported on this filesystem. Falling back to non-atomic move for prompt" id)
              (Files/move new-path (.toPath prompt-file) (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))))
          updated-record)))))
