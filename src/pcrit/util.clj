(ns pcrit.util
  (:import [java.text Normalizer Normalizer$Form]
           [java.security MessageDigest]
           [java.nio.charset StandardCharsets]))

(defn normalize-nfc
  "Normalize a string to Unicode NFC (Canonical Composition)."
  [s]
  (Normalizer/normalize s Normalizer$Form/NFC))

(defn sha1-hex
  "Hash a string (assumed normalized) as UTF-8 with SHA-1, return hex string."
  [s]
  (let [digest (MessageDigest/getInstance "SHA-1")
        bytes (.getBytes s StandardCharsets/UTF_8)
        hash-bytes (.digest digest bytes)]
    (format "%040x" (BigInteger. 1 hash-bytes))))

(defn canonical-sha1
  "Normalize input string to NFC and hash as UTF-8 with SHA-1."
  [s]
  (sha1-hex (normalize-nfc s)))
