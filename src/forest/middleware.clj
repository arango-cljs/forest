(ns forest.middleware
  "Macros that help defining a Forest middlewares. A middleware is a
  function that receives an app followed by an optional list of
  arguments."
  (:require [clojure.tools.macro :as macro]))

(defmacro defmiddleware
  "Defines a generic middleware that will take an app and do something
  to change it."

  {:arglists '([name doc-string? attr-map? middleware-args & body])}
  [name & body]
  (let [[name [middleware-args & body]] (macro/name-with-attributes name body)]
    `(def ~name
       ;; middleware function
       (fn [routes# ~@middleware-args]
         (fn [app#]
           (routes# app#)
           (binding [forest.route/*app* app#]
             ~@body)
           app#)))))

(defmacro defmiddleware-before
  "Defines a Forest `before` middleware.

  `main-function` is a function that receives two arguments `request`
  and `response` and will be executed before the route's appropriate
  request handler.

  If `main-function` retuns `false`, the request handler will be
  terminated."

  {:arglists '([name doc-string? attr-map? middleware-args main-function])}
  [name & body]
  (let [[name [middleware-args main-function]] (macro/name-with-attributes name body)]
    `(def ~name
       ;; middleware function
       (fn [routes# ~@middleware-args]
         (fn [app#]
           (routes# app#)
           (binding [forest.route/*app* app#]
             (.before app# ~main-function))
           app#)))))

(defmacro defmiddleware-after
  "Like `defmiddleware` but `main-function` will be executed after
  the request handler."

  {:arglists '([name doc-string? attr-map? middleware-args main-function])}
  [name & body]
  (let [[name [middleware-args main-function]] (macro/name-with-attributes name body)]
    `(def ~name
       ;; middleware function
       (fn [routes# ~@middleware-args]
         (fn [app#]
           (routes# app#)
           (binding [forest.route/*app* app#]
             (.after app# ~main-function))
           app#)))))

(defmacro defmiddleware-around
  "Like `defmiddleware` but `main-function` will be executed in place
  of the request handler. Unlike in `defmiddleware`, this
  `main-function` will be passed four arguments: `request`,
  `response`, `opts` and `next`. `next` is a function that takes no
  arguments and will trigger the route's request handler when being
  called."

  {:arglists '([name doc-string? attr-map? middleware-args main-function])}
  [name & body]
  (let [[name [middleware-args main-function]] (macro/name-with-attributes name body)]
    `(def ~name
       ;; middleware function
       (fn [routes# ~@middleware-args]
         (fn [app#]
           (routes# app#)
           (binding [forest.route/*app* app#]
             (.around app# ~main-function))
           app#)))))
