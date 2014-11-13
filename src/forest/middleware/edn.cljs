(ns forest.middleware.edn
  (:require [cljs.reader :refer [read-string]]
            [arango.console :refer [info]])
  (:require-macros [forest.middleware :refer [defmiddleware-before]]))

(defn edn-request?
  "Checks if `req` is an EDN request by looking at its content-type
  header."
  [req]
  (when-let [content-type (-> (aget req "headers")
                              (aget "content-type"))]
    (seq (re-find #"^application/(vnd.+)?edn" content-type))))

(defn read-string-safely
  "Reads a string in an EDN request's body and tries to convert to
  Clojure data structure"
  [s]
  (when (and s (string? s) (seq s))
    (try (read-string s)
         (catch :default e
           (info "Invalid edn body" s)))))

(defmiddleware-before wrap-edn
  "If a request is of EDN type, reads its body as Clojure data
  structure and populates it into request's attribute
  `:edn-parameters`. Remember that you need to call
  the `(enable-destructuring-edn-parameters)` macro before any
  Compojure destructuring forms that involve that attribute."
  []
  (fn [req res]
    (when (edn-request? req)
      (->> (.rawBody req)
           read-string-safely
           (aset req "ednParameters")))))
