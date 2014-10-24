(ns arango.core)

(def ^{:no-doc true :private true}
  internal (js/require "internal"))

(def ^{:doc "is true if being in development mode, otherwise false."}
  development-mode? (.-developmentMode internal))

(def ^{:doc "A string representing current platform."}
  platform (.-platform internal))

(def ^{:doc "A string representing ArangoDB version."}
  version (.-version internal))

(def Foxx (js/require "org/arangodb/foxx"))

(def Controller (.-Controller Foxx))

(defn start [routes & {:keys [url-prefix]}]
  (let [initial-app (->> (when url-prefix #js {:urlPrefix url-prefix})
                         (Controller. js/applicationContext))]
    ;; assiociate routes to app
    (routes initial-app)))
