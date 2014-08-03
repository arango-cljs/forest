(ns foxx-cljs.arango
  (:refer-clojure :exclude [print])
  (:require [foxx-cljs.route :refer [*url-prefix*]]))

(def ^:private logger (js/require "console"))

(defn log [& data] (.log logger (pr-str data)))

(def ^:private internal (js/require "internal"))

(defn print [& data] (.print internal (pr-str data)))

(def foxx-auth (js/require "org/arangodb/foxx/authentication"))
(def arango-is (js/require "org/arangodb/is"))
(def errors (.-errors internal))
(def Sessions (.-Sessions foxx-auth))
(def Users (.-Users foxx-auth))
(def CookieAuthentication (.-CookieAuthentication foxx-auth))
(def Authentication (.-Authentication foxx-auth))

(def Foxx (js/require "org/arangodb/foxx"))

(def FormatMiddleware (.-FormatMiddleware Foxx))

(def Controller (.-Controller Foxx))

(def Model (.extend (.-Model Foxx) #js {}))

(defn Repository [indexes]
  (.extend (.-Repository Foxx) indexes))

(defn start [routes]
  (let [initial-app (->> #js {:urlPrefix *url-prefix*}
                         (Controller. js/applicationContext))]
    ;; assiociate routes to app
    (routes initial-app)))
