(ns pcrit.pdb.util
  (:require [clojure.string :as str])
  (:import [java.text Normalizer Normalizer$Form]
           [java.security MessageDigest]
           [java.nio.charset StandardCharsets]))

(defn- normalize-nfc
  "Normalize a string to Unicode NFC (Canonical Composition)."
  [s]
  (Normalizer/normalize s Normalizer$Form/NFC))

(defn normalize-line-endings
  "Converts all line endings (CRLF, CR) in a string to a single Line Feed (LF)."
  [s]
  (if (nil? s) "" (-> s (str/replace #"\r\n" "\n") (str/replace #"\r" "\n"))))

(defn canonicalize-text
  "Fully canonicalizes text per the spec: NFC normalization, LF line endings,
  and **exactly one** trailing newline."
  [s]
  (let [s' (if (nil? s) "" s)
        normalized (-> s'
                       (normalize-nfc)
                       (normalize-line-endings)
                       (str/replace #"\n+$" ""))] ; Remove all existing trailing newlines
    (str normalized "\n"))) ; Add exactly one back

(defn sha1-hex
  "Hash a string (assumed UTF-8) with SHA-1, return hex string."
  [s]
  (let [digest (MessageDigest/getInstance "SHA-1")
        bytes (.getBytes s StandardCharsets/UTF_8)
        hash-bytes (.digest digest bytes)]
    (format "%040x" (BigInteger. 1 hash-bytes))))
