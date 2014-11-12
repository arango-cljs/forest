(ns arango.http-client
  "HTTP client inside ArangoDB."
  (:require [arango.core :refer [internal]]))

(def ^{:arglists ([url body params])
       :doc "Makes a HTTP request to a given `url` with a request
  body. `body` can be `nil` or a string. `Params` is a JSON object
  containing the following keys/values:

    - `method`: can be one of these strings: GET, POST, PUT, DELETE, MOVE,
  PATCH, HEAD.

    - `headers`: a JSON object represents pairs of request headers."}

  request (.-download internal))
