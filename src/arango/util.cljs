(ns arango.util
  "Utilities for Arango")

(defn encode
  "Converts strings between encoding systems."
  [s input-encoding output-encoding]
  (-> (js/Buffer. s input-encoding)
      (.toString output-encoding)))

(defn base64-encode
  "Encodes an UTF-8 string to base64."
  [s]
  (-> (js/Buffer. s "utf-8")
      (.toString "base64")))

(defn base64-decode
  "Decodes a base64-encoded string to UTF-8."
  [s]
  (-> (js/Buffer. s "base64")
      (.toString "utf-8")))
