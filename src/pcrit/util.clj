(ns pcrit.util
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

(defn- ensure-trailing-newline
  "Ensures the given string ends with exactly one newline."
  [s]
  (if (str/ends-with? s "\n")
    s
    (str s "\n")))

(defn canonicalize-text
  "Fully canonicalizes prompt text per the spec: NFC normalization, LF line endings, and a guaranteed trailing newline."
  [s]
  (-> s
      (normalize-nfc)
      (normalize-line-endings)
      (ensure-trailing-newline)))

(defn sha1-hex
  "Hash a string (assumed UTF-8) with SHA-1, return hex string."
  [s]
  (let [digest (MessageDigest/getInstance "SHA-1")
        bytes (.getBytes s StandardCharsets/UTF_8)
        hash-bytes (.digest digest bytes)]
    (format "%040x" (BigInteger. 1 hash-bytes))))
