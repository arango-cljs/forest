(ns forest.middleware.powered-by
  "Attaches an `X-Powered-By` header to outgoing responses."
  (:require-macros [forest.middleware :refer [defmiddleware-before]]))

(defmiddleware-before wrap-powered-by
  "Attaches an `X-Powered-By` header to outgoing responses."
  [& [powered-by-string]]
  (fn [req res]
    (.set res "X-Powered-By" (or powered-by-string "ArangoDB - Clojurescript - Forest"))))
