(ns foxx-cljs.route
  (:require [clojure.string :as str])
  #+cljs
  (:require-macros [foxx-cljs.route
                    :refer [context with-uri-params
                            with-request-destructuring
                            foxx-route expand-handler]]))

#+cljs
(def ^:dynamic *url-prefix* "")

#+cljs
(defn any [app uri handler]
  (.get app uri handler)
  (.post app uri handler)
  (.put app uri handler)
  (.del app uri handler)
  (.head app uri handler)
  (.patch app uri handler))

#+clj
(defn context-binding [prefix]
  `[*url-prefix* (str *url-prefix* ~prefix)])

#+clj
(defmacro context [prefix & body]
  `(binding ~(context-binding prefix)
     ~@body))

#+cljs
(defn response [response content]
  (assert (map? content))
  (let [{:keys [status headers body]} content]
       (.status response status)
       (doseq [[k v] headers]
         (.set response k v))
       (aset response "body" body)))

#+cljs
(defn edn
  ([body] (edn 200 body))
  ([status body]
   {:status status
    :headers {"Content-Type" "application/edn"}
    :body (pr-str body)}))

#+cljs
(defn json*
  ([body] (json* 200 body))
  ([status body]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (.stringify js/JSON body)}))

#+cljs
(defn json
  ([body] (json* 200 (cljs->js body)))
  ([status body]
   (json* 200 (cljs->js body))))
;; --------

#+clj
(defn get-uri-param [request param]
  [(symbol (name param))
   `(.params ~request ~(name param))])

#+clj
(defn uri-param-bindings [request params]
  (vec (mapcat #(get-uri-param request %) params)))

#+clj
(defmacro with-uri-params [request params body]
  `(let ~(uri-param-bindings request params)
     ~body))

#+clj
(def string-attr?
  #{:compatibility
    :database
    :protocol
    :server
    :path
    :url
    :request-type
    :request-body})

#+clj
(def json-attr?
  #{:headers
    :cookies
    :parameters
    :url-parameters
    :current-session
    :user})

#+clj
(defn camel-case
  "Converts kebab-case to camelCase"
  [s]
  (str/replace s #"-(\w)" (comp str/upper-case second)))

#+clj
(defn lookup-attribute [js-obj attribute]
  `(aget ~js-obj ~(if (keyword? attribute)
                    (camel-case (name attribute))
                    attribute)))

#+clj
(defn destructure-string [request binding-sym attribute]
  `[~binding-sym ~(lookup-attribute request attribute)])

#+clj
(defn key-mappings->binding-forms [key-mappings value]
  (->> (for [[sym mapped-key] key-mappings
             :when (and (symbol? sym)
                        (or (keyword? mapped-key)
                            (string? mapped-key)))]
         `[~sym ~(lookup-attribute value mapped-key)])
       (apply concat)))

#+clj
(defn keys->key-mappings [ks]
  (into {} (map #(vector % (keyword %)) ks)))

#+clj
(defn keys->binding-forms [ks value]
  (key-mappings->binding-forms (keys->key-mappings ks) value))

#+clj
(defn destructure-json-map [m value]
  `[~@(when-let [as (:as m)]
        [as `(cljs.core/js->clj ~value)]) ;; js->clj
    ~@(keys->binding-forms (:keys m) value)
    ~@(key-mappings->binding-forms (dissoc m :as :keys) value)])

#+clj
(defn destructure-json-vector [v value]
  (if (every? symbol? v)
    (destructure-json-map {:keys v} value)
    (let [total (count v)
          [ks [should-be-:as as]] (split-at (- total 2) v)]
      (if (and (every? symbol? ks) (= :as should-be-:as) (symbol? as))
        (destructure-json-map {:keys ks :as as} value)))))

#+clj
(defn destructure-json-symbol [sym value]
  (destructure-json-map {:as sym} value))

#+clj
(defn destructure-json [request bindings attribute]
  (cond
   (symbol? bindings)
   (destructure-json-symbol bindings (lookup-attribute request attribute))
   (vector? bindings)
   (destructure-json-vector bindings (lookup-attribute request attribute))
   (map? bindings)
   (destructure-json-map bindings (lookup-attribute request attribute))))

#+clj
(defn destructure-edn [request bindings attribute]
  `[~bindings ~(lookup-attribute request attribute)])

#+clj
(defn destructure-request*
  [request [bindings attribute]]
  (cond
   (string-attr? attribute)
   (destructure-string request bindings attribute)

   (json-attr? attribute)
   (destructure-json request bindings attribute)

   (= :edn-parameters attribute)
   (destructure-edn request bindings attribute)

   :else
   (throw (Exception. "Invalid destructuring form."))))

#+clj
(defn destructure-request [request request-destructuring]
  (mapcat #(destructure-request* request %)
          (seq request-destructuring)))

#+clj
(defmacro with-request-destructuring
  [request request-destructuring & body]
  `(let [~@(destructure-request request request-destructuring)]
     ~@body))

#+clj
(defmacro expand-handler [uri-params request-destructuring & content]
  `(fn [request# response#]
     (with-uri-params request# ~uri-params
       (with-request-destructuring request# ~request-destructuring
         ~@(butlast content)
         (response response# ~(last content))))))

#+clj
(defmacro foxx-route
  [method app uri uri-params request-destructuring & content]
  (let [action (case method
                 :GET    '.get
                 :POST   '.post
                 :PUT    '.put
                 :DELETE '.del
                 :HEAD   '.head
                 :PATCH  '.patch
                 :ANY    `any)]
    `(~action ~app ~uri
              (expand-handler ~uri-params
                              ~request-destructuring
                              ~@content))))

#+clj
(defmacro GET [app uri uri-params request-destructuring & content]
  `(foxx-route :GET ~app ~uri ~uri-params ~request-destructuring ~@content))
#+clj
(defmacro POST [app uri uri-params request-destructuring & content]
  `(foxx-route :POST ~app ~uri ~uri-params ~request-destructuring ~@content))
#+clj
(defmacro PUT [app uri uri-params request-destructuring & content]
  `(foxx-route :PUT ~app ~uri ~uri-params ~request-destructuring ~@content))
#+clj
(defmacro DELETE [app uri uri-params request-destructuring & content]
  `(foxx-route :DELETE ~app ~uri ~uri-params ~request-destructuring ~@content))
#+clj
(defmacro HEAD [app uri uri-params request-destructuring & content]
  `(foxx-route :HEAD ~app ~uri ~uri-params ~request-destructuring ~@content))
#+clj
(defmacro PATCH [app uri uri-params request-destructuring & content]
  `(foxx-route :PATCH ~app ~uri ~uri-params ~request-destructuring ~@content))
#+clj
(defmacro ANY [app uri uri-params request-destructuring & content]
  `(foxx-route :ANY ~app ~uri ~uri-params ~request-destructuring ~@content))

#+clj
(defmacro defmiddleware [sym params & body]
  `(defn ~sym [routes# ~@params]
     (fn [app#]
       (routes# app#)
       (->> (fn ~@body)
            (.before app#)))))
