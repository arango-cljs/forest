(ns forest.destructuring
  "Destructuring `request` JSON object using Clojure syntax."
  (:require [clojure.string :as string]
            [clojure.tools.macro :as macro]))

(defn get-uri-param
  "Receives a `request` object and a URI parameter name `param` as
  *symbols*, returns a binding form which is a vector of two elements:

   - a symbol (from *param*)
   - a form to access that parameter from the `request` object."
  [request param]
  [(symbol (name param)) `(.params ~request ~(name param))])

(defn uri-param-bindings
  "Helper function for `uri-param-bindings` macro to generate a
  destructuring form."
  [request params]
  (vec (mapcat #(get-uri-param request %) params)))

(defmacro with-uri-params
  "Wraps a `body` inside a `let` form that binds symbols (of those
  `params`) to their values found in `request`."
  [request params body]
  `(let ~(uri-param-bindings request params)
     ~body))

(def ^{:arglists '([attribute])}
  as-is-attribute?
  "The `request` object provided by Foxx has several
  attributes.  Many of them are plain strings or numbers. By binding
  them to symbols (via Compojure destructuring syntax) you get access
  to their values *as-is*.

  Such attributes are:

    - `compatibility`: an integer specifying the compatibility version
      sent by the client (in request header x-arango-version). If the
      client does not send this header, ArangoDB will set this to the
      minimum compatible version number. The value is 10000 major +
      100 minor (e.g. 10400 for ArangoDB version 1.4).

    - `database`: the name of the current database (e.g. `_system`)

    - `protocol`: `http` or `https`

    - `path`: request URI path, with potential database name stripped
      off.

    - `url`: request URI path + query string, with potential database
      name stripped off

    - `request-type`: the request method (e.g. `GET`, `POST`, `PUT`, ...)

    - `request-body`: the complete body of the request as a string."
  #{:compatibility
    :database
    :protocol
    :path
    :url
    :request-type
    :request-body})

(def ^{:arglists '([attribute])}
  json-attribute?
  "Other attributes of `request` object holds a JSON object as their
  values. For best performance, access forms for binding values will
  be generated according to destructuring forms. JSON objects will be
  they will be converted to Clojure *maps* only when needed.

  They are:

   - `headers`: a map of the request headers as key/value pairs

   - `user`: the name of the current ArangoDB user. This will be
     populated only if `wrap-authentication` middleware is turned on,
     and will be `nil` otherwise.

   - `server`: a map with `:address` (containing *server host name* or
     *IP address*) and `:port`.

   - `cookies`: a map with the request cookies as key/value pairs

   - `parameters`: a map with all parameters set in the URL as key/value pairs

   - `url-parameters`: a map with all named parameters defined for the
  route as key/value pairs."
  #{:headers
    :cookies
    :parameters
    :server
    :url-parameters
    :current-session
    :user})

(defmulti keyword->destructurer
  "Assigns a function to destructure a specific attribute (often
  populated into `request` object by middlewares). See `wrap-edn`
  middleware for an use case."
  identity)

(defmethod keyword->destructurer
  :default
  [attribute]
  (->> (name attribute)
       (repeat 3)
       (apply format (str "Don't know how to generate destructuring form for: %s"
                    "In case %s is populated into `request` object by a "
                    "middleware, did you forget to call "
                    "`(enable-destructuring-%s)` before all destructuring"
                    " forms?"))
       Exception.
       throw))

(defn camel-case
  "Converts kebab-case to camelCase"
  [s]
  (string/replace s #"-(\w)" (comp string/upper-case second)))

(defn lookup-attribute
  "Generates a form to look up value of a given `attribute` in
  a javascript object `js-obj`. `Attribute` can be *keyword* or *string*.
  Keywords will be *camelized* beforehand."
  [js-obj attribute]
  `(aget ~js-obj ~(if (keyword? attribute)
                    (camel-case (name attribute))
                    attribute)))

(defn destructure-as-is
  "Generates destructuring form for a symbol *binding-sym* whose value
  should be hold in `request` object as a plain string."
  [request binding-sym attribute]
  `[~binding-sym ~(lookup-attribute request attribute)])

(defn extract-uri-params
  "Receives a string representing an *Compojure URI pattern* `s`,
  extracts URI parameters (those starting with a `:`) found in `s` as
  a list of symbols." [s]
  (map #(-> % second symbol) (re-seq #":([a-z0-9-_]+)" s)))

(defn complete-map-binding-forms
  "Generates complete (both the left and right hand side) binding
  forms for map binding forms. Only accepts map bindings. Vector
  bindings need to be converted to their equivalant map binding form
  using `keys->map-binding-forms`"
  [map-bindings value]
  (->> (for [[sym mapped-key] map-bindings
             :when (and (symbol? sym)
                        (or (keyword? mapped-key)
                            (string? mapped-key)))]
         `[~sym ~(lookup-attribute value mapped-key)])
       (apply concat)))

(defn keys->map-binding-forms
  "Expands a shorthand vector of key symbols into its equivalent map
  binding form."
  [ks]
  (into {} (map #(vector % (keyword %)) ks)))

(defn keys->binding-forms
  "Expands a shorthand vector of key symbols into its complete binding
  form."
  [ks value]
  (complete-map-binding-forms (keys->map-binding-forms ks) value))

(defn destructure-json-map
  "If the whole JSON object is used (by an `:as` in map binding form),
  converts it to a Clojure map before binding to the specified symbol"
  [m value]
  `[~@(when-let [as (:as m)]
        [as `(cljs.core/js->clj ~value)]) ;; js->clj
    ~@(keys->binding-forms (:keys m) value)
    ~@(complete-map-binding-forms (dissoc m :as :keys) value)])

(defn destructure-json-vector
  "Expands a destructuring form of *vector* type to equivalant map form
  and passes to `destructure-json-map`"
  [v value]
  (if (every? symbol? v)
    (destructure-json-map {:keys v} value)
    (let [total (count v)
          [ks [should-be-:as as]] (split-at (- total 2) v)]
      (if (and (every? symbol? ks) (= :as should-be-:as) (symbol? as))
        (destructure-json-map {:keys ks :as as} value)))))

(defn destructure-json-symbol
  "Expands a destructuring form of *symbol* type to equivalant map form
  and passes to `destructure-json-map`"
  [sym value]
  (destructure-json-map {:as sym} value))

(defn destructure-json
  "Given a Compojure binding form (that will become the left-hand
  side), expands to a complete binding form by calling appropriate
  destructurer function to fill in the right-hand side."
  [request bindings attribute]
  (cond
   (symbol? bindings)
   (destructure-json-symbol bindings (lookup-attribute request attribute))
   (vector? bindings)
   (destructure-json-vector bindings (lookup-attribute request attribute))
   (map? bindings)
   (destructure-json-map bindings (lookup-attribute request attribute))))

(defn destructure-request*
  "Produces a pair of a symbol & its corresponding access form."
  [request [bindings attribute]]
  (cond
   (as-is-attribute? attribute)
   (destructure-as-is request bindings attribute)

   (json-attribute? attribute)
   (destructure-json request bindings attribute)

   :else
   (let [destructurer (keyword->destructurer attribute)]
     (destructurer request bindings attribute))))

(defn destructure-request
  "Helper function for `with-request-destructuring`. Produces a list
  of pairs of *symbols* & their corresponding *access forms*."
  [request request-destructuring]
  (mapcat #(destructure-request* request %)
          (seq request-destructuring)))

(defmacro with-request-destructuring
  "Wraps response `body` form inside a `let` binding form according to
  `request-destructuring`. Will bind *symbols* to their *values* found in
  `request`."
  [request request-destructuring & body]
  `(let [~@(destructure-request request request-destructuring)]
     ~@body))
