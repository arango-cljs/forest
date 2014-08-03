(ns foxx-cljs.parse
  (:require [goog.object :as gobject]
            [schema.core :as s :refer [validate]]
            [schema.coerce :as coerce
             :refer [coercer json-coercion-matcher]]))

(defn parse-js
  "Parses a javascript object.
  Returns Clojure data structure in the shape of the given schema."
  [schema js-obj]
  (let [parse (coercer schema json-coercion-matcher)]
    (-> js-obj
        (js->clj :keywordize-keys true)
        ;; remove unwanted key added by foxx
        (dissoc :_PRINT)
        parse)))

(defn parse-model
  "Parses a model returned by one of foxx repository's methods.
  Returns Clojure data structure in the shape of the given schema."
  [schema model]
  (parse-js schema (.-attributes model)))

(defn parse-models
  "Parses an array of models returned by one of foxx repository's methods.
  Returns Clojure data structure in the shape of the given schema."
  [schema models]
  (->> (vec models)
       (map #(parse-model schema %))))
