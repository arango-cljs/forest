(ns forest.middleware.edn
  (:require [forest.destructuring :refer [lookup-attribute keyword->destructurer]]))

(defn destructure-edn-parameters
  "Destructures binding forms that are bound to :edn-parameters
  in Compojure routes."
  [request bindings attribute]
  `[~bindings ~(lookup-attribute request attribute)])

(defmacro enable-destructuring-edn-parameters
  "Enable destructuring EDN parameters in requests. Must be called
  before you can use any `:edn-parameters` destructuring form."
  []
  (defmethod keyword->destructurer
    :edn-parameters [k] destructure-edn-parameters)
  nil)
