(ns foxx-cljs.app
  (:require [clojure.string :as str]
            [foxx-cljs.route :as route
             :refer [edn response]]
            [foxx-cljs.arango
             :refer [start log print]]
            [schema.core :as s]
            [foxx-cljs.repository
             :refer [create-collection!
                     app-repository add all count-all
                     by-id by-example first-example
                     remove-entry remove-by-id remove-by-example
                     replace-entry replace-by-id replace-by-example
                     update-by-id update-by-example
                     key-range near within fulltext]]
            [foxx-cljs.middleware
             :refer [wrap-format wrap-edn wrap-authentication]]
            [foxx-cljs.route :refer-macros [context GET POST PUT ANY]]
            [cljs.reader :refer [read-string]]))

(defn my-auth-app [app]
  ;; should show some authentication information when logged in
  (GET app "/hello" []
       {user :user
        {:keys [identifier _key data]} :current-session}
       (edn [user identifier _key data])))

(-> my-auth-app
    wrap-edn
    (wrap-authentication "/auth")
    start)

;; Playing with auth-app
;; 1. Register
;; curl -H "Content-Type: application/edn" -X PUT 'http://localhost:8529/dev/hello/auth' -d '{:username "foo" :password "bar"}' -b cookie.txt -c cookie.txt
;; 2. Login
;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/hello/auth' -d '{:username "foo" :password "bar"}' -b cookie.txt -c cookie.txt
;; 3. See your identifier is recognized
;; curl http://localhost:8529/dev/hello/hello -b cookie.txt -c cookie.txt
;; 4. Change the password
;; curl -H "Content-Type: application/edn" -X PATCH 'http://localhost:8529/dev/hello/auth' -d '{:password "newbar"}' -b cookie.txt -c cookie.txt
;; 5. Log out
;; curl -X DELETE 'http://localhost:8529/dev/hello/auth' -c cookie.txt -b cookie.txt
;; 6. Login using old password
;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/hello/auth' -d '{:username "foo" :password "bar"}' -b cookie.txt -c cookie.txt
;; 7. Login using new password
;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/hello/auth' -d '{:username "foo" :password "newbar"}' -b cookie.txt -c cookie.txt
