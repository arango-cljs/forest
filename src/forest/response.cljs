(ns forest.response
  "Passes content to response object with some useful formats.")

(defn response
  "Sets headers, status and body for the given response object `res` according to those
  defined in `content`. Use it inside a request handler."
  [res content]
  (assert (map? content))
  (let [{:keys [status headers body]} content]
    (.status res status)
    (doseq [[k v] headers]
      (.set res k v))
    (aset res "body" body)))

(defn edn
  "Receives a `status` (optional) and a Clojure data structure `body`, returns a
  *map* that can be passed to `response`"
  ([body] (edn 200 body))
  ([status body]
     {:status status
      :headers {"Content-Type" "application/edn"}
      :body (pr-str body)}))

(defn json*
  "Receives a `status` (optional) and a JSON *string* `body`, returns a
  *map* that can be passed to `response`"
  ([body] (json* 200 body))
  ([status body]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (.stringify js/JSON body)}))

(defn json'
  "Receives a `status` (optional) and a javascript data structure
  `body`. Will stringify `body` and return a *map* that can be passed to
  `response`"
  ([body] (json* 200 (.stringify js/JSON body)))
  ([status body]
     (json* 200 (.stringify js/JSON body))))

(defn json
  "Receives a `status` (optional) and a Clojure data structure
  `body`. Will coerce `body` to a JSON *string* and return a *map*
  that can be passed to `response`"
  ([body] (json* 200 (clj->js body)))
  ([status body]
     (json* 200 (clj->js body))))

(defn download
  "Sets HTTP headers to force web browsers to download the string
  `content` as file instead of displaying it inline.

  You could even set the `filename` differently than the requested
  URI."
  ([content]
     {:status 200
      :headers {"Content-Disposition" "attachment" }
      :body content})
  ([filename content]
     {:status 200
      :headers {"Content-Disposition" (str "attachment; filename= \"" filename "\"" )}
      :body content}))
