(ns pcrit.util
  (:require [clojure.string :as str])
  (:import [java.text Normalizer Normalizer$Form]
           [java.security MessageDigest]
           [java.nio.charset StandardCharsets]))

(defn- normalize-nfc
  "Normalize a string to Unicode NFC (Canonical Composition)."
  [s]
  (Normalizer/normalize s Normalizer$Form/NFC))

(defn- canonicalize-line-endings [s]
  (let [s' (if (nil? s) "" s)
        s'' (-> s' (str/replace #"\r\n" "\n") (str/replace #"\r" "\n"))]
    (if (str/ends-with? s'' "\n")
      s''
      (str s'' "\n"))))

(defn canonicalize-text
  "Fully canonicalizes prompt text per the spec: NFC normalization and LF line endings."
  [s]
  (-> s
      (normalize-nfc)
      (canonicalize-line-endings)))

(defn sha1-hex
  "Hash a string (assumed UTF-8) with SHA-1, return hex string."
  [s]
  (let [digest (MessageDigest/getInstance "SHA-1")
        bytes (.getBytes s StandardCharsets/UTF_8)
        hash-bytes (.digest digest bytes)]
    (format "%040x" (BigInteger. 1 hash-bytes))))
