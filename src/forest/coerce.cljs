(ns forest.coerce
  "Utils to coerce data between javascript an Clojure utilizing Schema library."
  (:require [schema.core :as s :refer [validate]]
            [schema.coerce :as coerce
             :refer [coercer json-coercion-matcher]]))

(defn coerce-js
  "Coerces a javascript object.  Returns Clojure data structure in the
  shape of the given `schema`."
  [schema js-obj]
  (let [coerce (coercer schema json-coercion-matcher)]
    (-> js-obj
        (js->clj :keywordize-keys true)
        ;; remove unwanted key added by foxx
        (dissoc :_PRINT)
        coerce)))

(defn coerce-model
  "Coerces a `model` returned by one of foxx repository's methods.
  Returns Clojure data structure in the shape of the given `schema`."
  [schema model]
  (coerce-js schema (.-attributes model)))

(defn coerce-models
  "Coerces an array of models returned by one of foxx repository's methods.
  Returns Clojure data structure in the shape of the given schema."
  [schema models]
  (->> (vec models)
       (map #(coerce-model schema %))))
