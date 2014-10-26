(ns forest.middleware.format
  (:require-macros [forest.middleware :refer [defmiddleware-before]])
  (:require [arango.core :refer [Foxx]]))

(def FormatMiddleware (.-FormatMiddleware Foxx))

(defmiddleware-before wrap-format
  "This Middleware gives you Rails-like format handling via the
  extension of the URL or the accept header.

  Say you request an URL like `/people.json`:

  The FormatMiddleware will set the format of the request to JSON and
  then delete the .json from the request. You can therefore write
  handlers that do not take an extension into consideration and
  instead handle the format via a simple string. To determine the
  format of the request it checks the URL and then the accept
  header. If one of them gives a format or both give the same, the
  format is set. If the formats are not the same, an error is raised."
  [routes & extensions]
  (->> extensions
       (apply array)
       (FormatMiddleware.)))
