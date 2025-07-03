(ns pcrit.pdb.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [pcrit.log.interface :as log]
            [pcrit.pdb.util :as util]
            [pcrit.pdb.io :as pdb-io]
            [pcrit.pdb.lock :as pdb-lock]
            [pcrit.pdb.id :as pdb-id])
  (:import [java.time Instant]))

(defn- validate-header-update! [old-header new-header]
  (when-not (and (= (:id old-header) (:id new-header))
                 (= (some-> old-header :sha1-hash str/lower-case)
                    (some-> new-header :sha1-hash str/lower-case)))
    (throw (ex-info "Updater function MUST NOT remove or change :id or :sha1-hash."
                    {:id (:id old-header) :old-header old-header :new-header new-header}))))

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
  "Creates a new prompt in the database atomically.
  Accepts optional keys:
  - :metadata    A map of static metadata to merge into the header.
  - :metadata-fn A function that receives the complete initial prompt record
                  (with id, hash, canonicalized body, etc.) and returns a map of additional
                  metadata to be merged into the header."
  [db-dir prompt-text & {:keys [metadata metadata-fn] :or {metadata {} metadata-fn nil}}]
  (let [id               (pdb-id/get-next-id! db-dir)
        canonical-body   (util/canonicalize-text prompt-text)
        system-metadata  {:spec-version "1"
                          :id id
                          :created-at (.toString (Instant/now))
                          :sha1-hash (util/sha1-hex canonical-body)}
        initial-record   {:header (merge system-metadata metadata)
                          :body   canonical-body}
        dynamic-metadata (when metadata-fn (metadata-fn initial-record))
        final-record     (if dynamic-metadata
                           (update initial-record :header merge dynamic-metadata)
                           initial-record)]
    (pdb-io/write-prompt-record-atomically! (pdb-io/->prompt-path db-dir id) final-record)
    (log/info "Created new prompt " id " in " db-dir)
    final-record))

(defn update-metadata
  "Atomically updates the metadata for the prompt with the given id."
  [db-dir id f]
  (let [prompt-file (pdb-io/->prompt-path db-dir id)
        lock-file (io/file (.getParentFile prompt-file) (str (.getName prompt-file) ".lock"))]
    (when-not (.exists prompt-file)
      (throw (ex-info (str "Prompt " id " not found.") {:id id :db-dir db-dir})))

    (pdb-lock/execute-with-lock lock-file
      (fn []
        (let [current-record (or (read-prompt db-dir id)
                                 (throw (ex-info (str "Prompt " id " disappeared after lock acquisition.") {:id id})))
              new-header (f (:header current-record))
              _ (validate-header-update! (:header current-record) new-header)
              updated-record (assoc current-record :header new-header)]
          (pdb-io/write-prompt-record-atomically! prompt-file updated-record)
          updated-record)))))
