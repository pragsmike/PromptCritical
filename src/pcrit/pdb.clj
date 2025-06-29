(ns pcrit.pdb
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml]
            [pcrit.util :as util]
            [pcrit.log :as log])
  (:import [java.io File]
           [java.time Instant]
           [java.nio.file Files CopyOption StandardCopyOption AtomicMoveNotSupportedException]))

;;; --- Constants and Configuration ---

(def ^:private lock-retry-ms 50)
(def ^:private lock-retry-jitter-ms 25)
(def ^:private lock-max-retries 100) ; ~5-7.5 seconds total
(def ^:private stale-lock-threshold-ms (* 10 60 1000)) ; 10 minutes

;;; --- Private Helper Functions ---

(defn- ->prompt-path ^File [db-dir id]
  (io/file db-dir (str id ".prompt")))

(defn- canonicalize-line-endings [s]
  (if (nil? s) "" (-> s (str/replace #"\r\n" "\n") (str/replace #"\r" "\n"))))

(defn- parse-prompt-file [^File f]
  (when (.exists f)
    (let [content (slurp f)
          content-lf (canonicalize-line-endings content)]
      (if-not (str/starts-with? content-lf "---")
        {:header {} :body content-lf}
        (if-let [end-delim-idx (str/index-of content-lf "\n---\n" 4)] ; Start search after initial "---"
          (let [yaml-str (subs content-lf 4 end-delim-idx) ; From after "---\n" to before "\n---\n"
                body (subs content-lf (+ end-delim-idx 5))
                body (util/canonicalize-text body)
                ] ; From after "\n---\n"

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

(defn- write-prompt-record! [^File f record]
  (let [{:keys [header body]} record
        header-str (if (seq header)
                     (str "---\n" (yaml/generate-string header) "---\n")
                     "")]
    (spit f (str header-str body))))

(defn- execute-with-lock
  "Self-healing, retrying lock protocol for a given lock file."
  [^File lock-file f]
  (when (.exists lock-file)
    (let [lock-age (- (System/currentTimeMillis) (.lastModified lock-file))]
      (if (> lock-age stale-lock-threshold-ms)
        (do
          (log/warn "Deleting stale lock file" (.getName lock-file) "(age:" lock-age "ms)")
          (io/delete-file lock-file))
        (log/info "Lock file" (.getName lock-file) "is too new to be stale. Waiting."))))

  (loop [retries-left lock-max-retries]
    (cond
      (zero? retries-left)
      (throw (ex-info (str "Could not acquire lock, timed out after " lock-max-retries " retries.")
                      {:lock-file lock-file}))

      (.createNewFile lock-file)
      (try
        (f)
        (finally
          (io/delete-file lock-file true)))

      :else
      (do
        (Thread/sleep (+ lock-retry-ms (rand-int lock-retry-jitter-ms)))
        (recur (dec retries-left))))))

(defn- get-next-id!
  "Atomically reads and increments a counter file using a lock."
  [db-dir]
  (let [lock-file (io/file db-dir "pdb.counter.lock")]
    (execute-with-lock lock-file
                       (fn []
                         (let [counter-file (io/file db-dir "pdb.counter")]
                           (try
                             (let [next-id (if (.exists counter-file)
                                             (inc (Integer/parseInt (slurp counter-file)))
                                             1)]
                               (spit counter-file (str next-id))
                               (str "P" next-id))
                             (catch NumberFormatException e
                               (log/error "pdb.counter file is corrupt. Could not parse integer.")
                               (throw (ex-info "Corrupt counter file" {:file counter-file} e)))))))))

;;; --- Public API ---

(defn read-prompt
  "Reads the prompt with the given id from the database directory."
  [db-dir id]
  (let [f (->prompt-path db-dir id)]
    (when-let [record (parse-prompt-file f)]
      (let [expected-hash (some-> (get-in record [:header :sha1-hash]) str/lower-case)
            actual-hash (util/sha1-hex (:body record))]
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
  (let [prompt-file (->prompt-path db-dir id)
        lock-file (io/file (.getParentFile prompt-file) (str (.getName prompt-file) ".lock"))]
    (when-not (.exists prompt-file)
      (throw (ex-info (str "Prompt " id " not found.") {:id id :db-dir db-dir})))

    (execute-with-lock lock-file
                       (fn []
                         (let [new-path (.toPath (io/file db-dir (str id ".prompt.new")))
                               current-record (or (read-prompt db-dir id)
                                                  (throw (ex-info (str "Prompt " id " disappeared after lock acquisition.") {:id id})))
                               new-header (f (:header current-record))
                               _ (when-not (and (= (get new-header :id) id)
                                                (= (get new-header :sha1-hash) (get-in current-record [:header :sha1-hash])))
                                   (throw (ex-info "Updater function MUST NOT remove or change :id or :sha1-hash."
                                                   {:id id :old-header (:header current-record) :new-header new-header})))
                               updated-record (assoc current-record :header new-header)]
                           (write-prompt-record! (.toFile new-path) updated-record)
                           (try
                             (Files/move new-path (.toPath prompt-file) (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
                             (catch AtomicMoveNotSupportedException _
                               (log/warn "ATOMIC_MOVE not supported on this filesystem. Falling back to non-atomic move for prompt" id)
                               (Files/move new-path (.toPath prompt-file) (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING]))))
                           updated-record)))))

(comment
  (def db-dir "/tmp")
  (def p1 (create-prompt "/tmp" "Original body."))
  (def id (get-in p1 [:header :id]))
  (def original-hash (get-in p1 [:header :sha1-hash]))
  (def updated-prompt (update-metadata "/tmp" id #(assoc % :status "reviewed")))
  (def f (->prompt-path db-dir id))
  (def record (parse-prompt-file f))
  (def content (slurp f))
  (def content-lf (canonicalize-line-endings content))
  (def end-delim-idx (str/index-of content-lf "\n---\n" 4))
  (def yaml-str (subs content-lf 4 end-delim-idx))
  (def body (subs content-lf (+ end-delim-idx 5)))

  (def crlf-body "This body\r\nhas CRLF\r\nendings.")
  (def crlf-header (str "---\r\n" "id: P99\r\n" "---\r\n"))
  (def crlf-content (str crlf-header crlf-body))
  (def prompt-file (io/file db-dir "P99.prompt"))
  (spit prompt-file crlf-content)
  (def read-p (read-prompt db-dir "P99"))
  (def ct (util/canonicalize-text crlf-body))
  (:body read-p)

;
  )
