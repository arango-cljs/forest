(ns arango.crypto
  "Crypto utilities for Foxx applications"
  (:require [arango.core :refer [internal]]))

(def ^{:arglists ([key message algorithm])
       :doc "Keyed-hash message authentication codes (HMAC) is a
  mechanism for message authentication using cryptographic hash
  algorithms. Allowed lgorithms: \"sha1\", \"sha224\", \"sha256\",
  \"sha384\" or \"sha512\""}
  hmac (.-hmac internal))

(def ^{:arglists ([salt password iterations key-length])
       :doc "PBKDF2 is a password-based key derivation function. In
       many applications of cryptography, user security is ultimately
       dependent on a password, and because a password usually can't
       be used directly as a cryptographic key, some processing is
       required.

  A salt provides a large set of keys for any given password, and an
  iteration count increases the cost of producing keys from a
  password, thereby also increasing the difficulty of attack."}
  pbkdf2 (.-pbkdf2 internal))

(def ^{:arglists ([value])
       :doc "MD5 is a widely used hash function. It's been used in a
       variety of security applications and is also commonly used to
       check the integrity of files. Though, MD5 is not collision
       resistant, and it isn't suitable for applications like SSL
       certificates or digital signatures that rely on this
       property."}
  md5 (.-md5 internal))

(def ^{:arglists ([value])
       :doc "The SHA hash functions were designed by the National
       Security Agency (NSA). SHA-1 is the most established of the
       existing SHA hash functions, and it's used in a variety of
       security applications and protocols. Though, SHA-1's collision
       resistance has been weakening as new attacks are discovered or
       improved."}
  sha1 (.-sha1 internal))

(def ^{:arglists ([value])
       :doc "SHA-256 is one of the four variants in the SHA-2 set. It
       isn't as widely used as SHA-1, though it appears to provide
       much better security."}
  sha256 (.-sha256 internal))

(def ^{:arglists ([value])
       :doc "SHA-512 is largely identical to SHA-256 but operates on
       64-bit words rather than 32."}
  sha512 (.-sha512 internal))

(def ^{:arglists ([value])
       :doc "SHA-224 a largely identical but truncated version of
  SHA-256"}
  sha224 (.-sha224 internal))

(def ^{:arglists ([value])
       :doc "SHA-384 a largely identical but truncated version of
  SHA-512"}
  sha384 (.-sha384 internal))

(def ^{:arglists ([value])
       :doc "Generates a string of a given length containing numbers."}
  random-numbers (.-genRandomNumbers internal))

(def ^{:arglists ([value])
       :doc "Generates a string of a given length containing numbers
  and characters."}
  random-alpha-numbers (.-genRandomAlphaNumbers internal))

(def ^{:arglists ([value])
       :doc "Generates a string containing numbers and characters (length 8)."}
  random-salt (.-genRandomSalt internal))

;; stolen from https://github.com/weavejester/crypto-equality
(defn eq?
  "Test whether two sequences of characters or bytes are equal in a way that
protects against timing attacks. Note that this does not prevent an attacker
from discovering the *length* of the data being compared."
  [a b]
  (let [a (map int a), b (map int b)]
    (if (and a b (= (count a) (count b)))
      (zero? (reduce bit-or (map bit-xor a b)))
      false)))
