(ns forest.crypto.password.pbkdf2
  "Password encryption using pbkdf2"
  (:require [arango.util :refer [encode]]
            [arango.crypto :refer [random-alpha-numbers pbkdf2 eq?]]))

(defn verify
  "Verifies a given password with encrypted data returned by the
  `encrypt` function."
  [{:keys [hash salt key-length hash-method work-units baseline work-key]
    :or {work-units 60
         work-key 388
         key-length 66
         hash ""
         salt ""}} password]
  (if-not (= "pbkdf2" hash-method)
    (throw (js/Error. (str "Unsupported hash method: " hash-method)))
    (let [iterations (* (+ baseline work-key) work-units)
          valid-hash (pbkdf2 salt password iterations key-length)]
      (eq? hash (encode valid-hash "hex" "base64")))))

(defn encrypt
  "Encrypts a password string using the PBKDF2 algorithm. The default
  key length to generate random salt is 66. Default baseline is
  1,000. Default number of work units is 60 and default work key is
  388.

  Returns a map containing the following information: hash, salt,
  key-length, hash-method and work-units."
  [password & {:keys [key-length baseline work-units work-key]
               :or {key-length 66
                    baseline 1000
                    work-units 60
                    work-key 388}}]
  (let [salt       (random-alpha-numbers key-length)
        iterations (* (+ baseline work-key) work-units)
        hash       (pbkdf2 salt password iterations key-length)]
    {:hash (encode hash "hex" "base64")
     :salt salt
     :key-length key-length
     :hash-method "pbkdf2"
     :work-units work-units}))
