(ns forest.route
  (:require [clojure.tools.macro :as macro]
            [forest.route.destructuring
             :refer [extract-uri-params with-uri-params
                     with-request-destructuring]]))

(defmacro expand-handler
  "Helper function for `compile-route`.

  Wraps response `body` form inside several binding forms by utilizing
  `with-uir-destructuring` and `with-request-destructuring`"
  [uri-params request-destructuring & content]
  `(fn [request# response#]
     (with-uri-params request# ~uri-params
       (with-request-destructuring request# ~request-destructuring
         (binding [*request*  request#
                   *response* response#]
           ~@(butlast content)
           (forest.response/response response# ~(last content)))))))

(defmacro compile-route
  "Compiles a route in the form (method path bindings & body) into a function.
  Used to create custom route macros."
  [method uri request-destructuring & content]
  (let [action (case method
                 :GET    '.get
                 :POST   '.post
                 :PUT    '.put
                 :DELETE '.del
                 :HEAD   '.head
                 :PATCH  '.patch
                 :ANY    `any)]
    `(fn [app#] (~action app# ~uri
                         (expand-handler ~(extract-uri-params uri)
                                         ~request-destructuring
                                         ~@content)))))

(defmacro GET
  "Generate a GET route."
  [uri request-destructuring & content]
  `(compile-route :GET ~uri ~request-destructuring ~@content))

(defmacro POST
  "Generate a POST route."
  [uri request-destructuring & content]
  `(compile-route :POST ~uri ~request-destructuring ~@content))

(defmacro PUT
  "Generate a PUT route."
  [uri request-destructuring & content]
  `(compile-route :PUT ~uri ~request-destructuring ~@content))

(defmacro DELETE
  "Generate a DELETE route."
  [uri request-destructuring & content]
  `(compile-route :DELETE ~uri ~request-destructuring ~@content))

(defmacro HEAD
  "Generate a HEAD route."
  [uri request-destructuring & content]
  `(compile-route :HEAD ~uri ~request-destructuring ~@content))

(defmacro PATCH
  "Generate a PATCH route."
  [uri request-destructuring & content]
  `(compile-route :PATCH ~uri ~request-destructuring ~@content))

(defmacro ANY
  "Generate a route that matches any method."
  [uri request-destructuring & content]
  `(compile-route :ANY ~uri ~request-destructuring ~@content))

(defmacro defroutes
  "Define a Foxx handler function from a sequence of routes. The name
  may optionally be followed by a doc-string and metadata map."
  [name & route-handlers]
  (let [[name route-handlers] (macro/name-with-attributes name route-handlers)]
    `(def ~name (routes ~@route-handlers))))
