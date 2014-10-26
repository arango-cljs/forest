(ns forest.route
  "Functions and macros to help building Compojure-like request
  handlers."
  (:require [forest.response :refer [response]]))

(def ^:dynamic *app* #js {})
(def ^:dynamic *request* #js {})
(def ^:dynamic *response* #js {})

(defn routing
  "Apply a list of routes to a Foxx app."
  [app & handlers]
  (binding [*app* app]
    (doseq [f handlers]
      (f app))))

(defn routes
  "Create a Foxx handler by combining several handlers into one."
  [& handlers]
  #(apply routing % handlers))

(defn any
  "Shorthand for requests that match any HTTP method."
  [app uri handler]
  (.get app uri handler)
  (.post app uri handler)
  (.put app uri handler)
  (.del app uri handler)
  (.head app uri handler)
  (.patch app uri handler))
