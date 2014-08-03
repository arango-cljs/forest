(ns foxx-cljs.core-test
  (:require [clojure.string :as str]
      #+clj [foxx-cljs.route :refer :all]
     #+cljs [foxx-cljs.route :as route
             :refer [*url-prefix* edn response]]
     #+cljs [schema.core :as s]
     #+cljs [schema.coerce :as coerce
             :refer [coercer json-coercion-matcher]]
     #+cljs [foxx-cljs.route
             :refer-macros
             [context with-uri-params GET PUT ANY foxx-route
              with-request-destructuring expand-handler]]
     #+cljs [foxx-cljs.parse
             :refer [parse-js parse-model parse-models]]
      #+clj [clojure.test :as t
             :refer [is deftest with-test run-tests testing]]
      #+cljs [cemerick.cljs.test :as t]
      #+cljs [cemerick.cljs.test
              :refer-macros
              [is deftest with-test run-tests testing test-var]]
      #+cljs [cljs.reader :refer [read-string]]))

#+cljs
(deftest context-binding-test
  (is (= (context "/foo"
           (context "/bar"
             *url-prefix*))
         "/foo/bar")))

#+cljs
(deftest edn-test
  (is (= (edn 404 {:foo "bar"})
         {:status 404
          :headers {"Content-Type" "application/edn"}
          :body "{:foo \"bar\"}"}))
  (is (= (edn {:foo "bar"})
         {:status 200
          :headers {"Content-Type" "application/edn"}
          :body "{:foo \"bar\"}"})))

#+clj
(deftest key-mappings->binding-forms-test
  (is (= (key-mappings->binding-forms '{a :x b :y} 'foo)
         '[a (clojure.core/aget foo "x") b (clojure.core/aget foo "y")])))

#+clj
(deftest keys->key-mappings-test
  (is (= (keys->key-mappings '[a b])
         '{a :a, b :b})))

#+clj
(deftest destructure-json-map-test
  (is (= (destructure-json-map
          '{m :a n :b-z p "NotAKeyword" :keys [c d] :as x} 'value)
         '[x (cljs.core/js->clj value)
           c (clojure.core/aget value "c")
           d (clojure.core/aget value "d")
           m (clojure.core/aget value "a")
           n (clojure.core/aget value "bZ")
           p (clojure.core/aget value "NotAKeyword")])))

#+clj
(deftest destructure-json-vector-test
  (is (= (destructure-json-vector '[foo bar] 'value)
         '[foo (clojure.core/aget value "foo")
           bar (clojure.core/aget value "bar")]))
  (is (= (destructure-json-vector '[foo bar :as loo] 'value)
         '[loo (cljs.core/js->clj value)
           foo (clojure.core/aget value "foo")
           bar (clojure.core/aget value "bar")])))

#+cljs
(deftest with-uri-params-test
  (let [mock-request #js {"params" #({"foo" 1 "bar" 2} %)}]
    (is (= (.params mock-request "foo") 1))
    (is (= (.params mock-request "bar") 2))
    (is (= (with-uri-params mock-request
             [foo bar]
             (+ foo bar))
           3))))

#+cljs
(defn status-setter [x v] (assoc x :status v))
#+cljs
(defn headers-setter [x k v] (assoc-in x [:headers k] v))

#+cljs
(defn make-mock-response [an-atom]
  #js {"status" #(swap! an-atom status-setter %)
       "set" #(swap! an-atom headers-setter %1 %2)})

#+cljs
(def sample-data {:status 0 :headers {}})

#+cljs
(deftest setters-test
  (is (= (status-setter sample-data 300)
         {:status 300 :headers {}}))
  (is (= (headers-setter sample-data "foo" "bar")
         {:status 0 :headers {"foo" "bar"}})))

#+cljs
(deftest response-data-test
  (let [data (atom sample-data)
        mock-response (make-mock-response data)]
    (doto mock-response
      (.status 200)
      (.set "fu" "ba"))
    (is (= @data
           {:status 200 :headers {"fu" "ba"}}))
    (response mock-response {:status 401
                             :headers {"pu" "pa"
                                       "lu" "la"}
                             :body (+ [] {})})
    (is (= @data
           {:status 401 :headers {"fu" "ba" "pu" "pa" "lu" "la"}}))
    (is (= (aget mock-response "body") "[]{}"))))

#+cljs
(deftest response-edn-test
  (let [data (atom sample-data)
        mock-response (make-mock-response data)]
    (response mock-response (edn 401 {:hello "world"}))
    (is (= @data
           {:status 401 :headers {"Content-Type" "application/edn"}}))
    (is (= (aget mock-response "body") "{:hello \"world\"}"))))

#+cljs
(defn make-mock-request-params [m]
  #js {"params" #(get m %)})

#+cljs
(deftest mock-request-tests
  (let [mock-request (make-mock-request-params {"foo" 1 "bar" 2})]
    (is (= (.params mock-request "foo") 1))))

#+clj
(deftest uri-param-bindings-test
  (is (= (get-uri-param 'a-request 'foo)
         '[foo (.params a-request "foo")]))
  (is (= (uri-param-bindings 'a-request '[foo bar])
         '[foo (.params a-request "foo")
           bar (.params a-request "bar")])))

#+cljs
(deftest with-uri-params-test
  (let [mock-request (make-mock-request-params {"foo" 1 "bar" 2})]
    (is (= (with-uri-params mock-request [foo bar] (+ foo bar))
           3))))

#+cljs
(deftest with-request-destructuring-test
  (let [mock-request #js {"parameters" #js {"foo" 3 "bar" 4}}]
    (is (= (with-request-destructuring mock-request
             {[foo bar] :parameters}
             (+ foo bar))
           7))))

#+cljs
(deftest expand-handler-test
  (let [data (atom sample-data)
        mock-request #js {"headers" #js {"foo" 3 "bar" 4}
                          "params" #(get {"loo" 1 "la" 2} %)}
        mock-response (make-mock-response data)
        handler (expand-handler [loo la] {[foo bar] :headers}
                                (edn (+ loo la foo bar)))]
    (handler mock-request mock-response)
    (is (= (aget mock-response "body") "10"))

    (is (= @data {:status 200,
                  :headers {"Content-Type" "application/edn"}}))))

#+cljs
(deftest foxx-route-test
  (let [data (atom sample-data)
        mock-request #js {"headers" #js {"foo" 4 "bar" 6}
                          "params" #(get {"loo" 0 "la" 2} %)}
        mock-response (make-mock-response data)
        mock-app #js {"get" (fn [_ handler]
                              (handler mock-request mock-response))}]
    (GET mock-app "/"
         [loo la] {[foo bar] :headers}
         (edn 404 (+ loo la foo bar)))
    (is (= (aget mock-response "body") "12"))

    (is (= @data {:status 404,
                  :headers {"Content-Type" "application/edn"}}))))

#+cljs
(deftest parsers-tests
  (is (= (js->clj #js {"foo" #js {"bar" 2}}
                  :keywordize-keys true)
         {:foo {:bar 2}}))
  ;; wrap two clauses inside `pr-str`s because javascript objects
  ;; can't be compared with `=`
  (is (= (pr-str (clj->js {:foo #{:bar}}))
         (pr-str #js {"foo" #js ["bar"]})))
  (is (= (parse-js {:text s/Str} #js {:text "foo"})
         {:text "foo"}))
  (is (= (parse-models {:text #{s/Keyword}}
                       (array #js {:attributes #js {:text (array "foo")}}
                              #js {:attributes #js {:text (array "bar")}}))
         [{:text #{:foo}} {:text #{:bar}}])))

#-clj
(is (thrown-with-msg? js/Error #"not an object"
                      (do-something)))

#+cljs
(deftest schema-tests
  (let [CommentRequest
        {:text s/Str
         :share-services #{(s/enum :twitter :facebook :google)}}

        parse-comment-request
        (coercer CommentRequest json-coercion-matcher)]
    (is (= (parse-comment-request
            {:text "This is awesome!"
             :share-services ["twitter" "facebook"]})
           {:text "This is awesome!"
            :share-services #{:twitter :facebook}}))))
