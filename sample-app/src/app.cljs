(ns sample.app
  (:require [forest.response :refer [edn]]
            [arango.fs :refer [spit slurp]]
            [arango.console :refer [info]]
            [arango.core :refer [start]]
            [schema.core :as s]
            [forest.repository
             :refer [create-collection!
                     app-repository add all count-all
                     by-id by-example first-example
                     remove-entry remove-by-id remove-by-example
                     replace-entry replace-by-id replace-by-example
                     update-by-id update-by-example
                     key-range near within fulltext]]
            [forest.middleware.edn :refer [wrap-edn]
             :refer-macros [enable-destructuring-edn-parameters]]
            [forest.route :refer [*request* *response* *app*]
             :refer-macros [defroutes GET POST PUT ANY]]))

;; you can create a collection here in development mode
;; but in production, you need a separated setup.js file to do it
(create-collection! "texts")

;; A respository with a fulltext index
(def repo
  (app-repository "texts" {:text s/Str}
                  [{:type :fulltext
                    :fields [:text]
                    :minLength 3}]))

(enable-destructuring-edn-parameters)

(defroutes repo-app
  ;; curl 'http://localhost:8529/dev/hi/hi-repo?text=bla'
  (GET "/repo" {}
       (edn {:ok (all repo)}))
  (GET "/search/:key/:text" {}
       (edn {:results (fulltext repo key text)}))
  (GET "/repo/:id" {}
       (edn {:ok (by-id repo id)}))
  (POST "/by-example" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/repo-example' -d '{:text "ya"}'
        {example :edn-parameters}
        (edn (by-example repo example)))
  (POST "/first-example" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/first-example' -d '{:text "ya"}'
        {example :edn-parameters}
        (edn (first-example repo example)))
  (POST "/replace" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/replace' -d '{:text "ya" :_id ""}'
        {entry :edn-parameters}
        (edn {:fine (replace-entry repo entry)}))
  (POST "/replace/:id" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/replace' -d '{:text "ya" :_id ""}'
        {entry :edn-parameters}
        (edn {:fine (replace-by-id repo id entry)}))
  (POST "/replace-example" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/replace' -d '{:text "ya" :_id ""}'
        {[example entry] :edn-parameters}
        (edn {:fine (replace-by-example repo example entry)}))
  (POST "/update/:id" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/replace' -d '{:text "ya" :_id ""}'
        {data :edn-parameters}
        (edn {:fine (update-by-id repo id data)}))
  (POST "/update" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/replace' -d '{:text "ya" :_id ""}'
        {[example data] :edn-parameters}
        (edn {:fine (update-by-example repo example data)}))
  (POST "/count" ;; curl -H "Content-Type: application/edn" -X POST 'http://localhost:8529/dev/{APP-MOUNT-POINT}/replace' -d '{:text "ya" :_id ""}'
        {[example data] :edn-parameters}
        (edn {:fine (count-all repo)})))

(-> repo-app wrap-edn start)

(defroutes dump-routes
  (GET "/dump-request" {}
       (edn (js->clj *request*)))
  (GET "/dump-response" {}
       (edn (js->clj *response*)))
  (GET "/dump-app" {}
       (edn (js->clj *app*)))
  (GET "/write/:content" {}
       (spit "/tmp/foo" content)
       (edn "ok"))
  (POST "/upload"
        {headers :headers
         request-body :request-body}
        (edn [headers request-body]))
  (GET  "/format"
        {path :path headers :headers}
        (edn [path headers]))
  (GET "/hello/:name" {}
       (edn {:name name
             :version "basic url param"}))
  (GET "/destructuring/:name" {}
       (edn 200 {:name name
                 :version "Hello destructuring..."}))
  (GET "/other-destructuring/:name"
       {prot :protocol tipe :request-type}
       (edn {:name name
             :protocol prot
             :type tipe
             :version "tipe"}))
  (GET "/hi-destructuring"
       ;; /hi-destructuring?a=1&b=2&c=3&d=4
       {prot :protocol
        {m :a n :b :keys [c d] :as x} :parameters}
       (edn {:version "expand-handler macro"
             :all [m n c d x]}))
  (GET "/hello-destructuring"
       ;; /hello-destructuring?a=1&b=2&c=3&d=4
       {[a b c d] :parameters}
       (edn {:version "des vector macro."
             :all [a b c d]}))
  (GET "/aloha-destructuring"
       ;; /aloha-destructuring?a=1&b=2&c=3&d=4
       {[a b c d :as x] :parameters}
       (edn {:version "destructruring vector with :as"
             :all [a b c d x]}))
  (PUT "/echo"
       {file :edn-parameters}
       (edn (:file file))))

(-> dump-routes wrap-edn start)
