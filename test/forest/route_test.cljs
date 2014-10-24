(ns foxx-cljs.route-test
  (:require-macros
   [forest.destructuring
    :refer [with-uri-params with-request-destructuring]])
  (:require [clojure.string :as str]
            [forest.response :as route :refer [edn response]]
            [schema.core :as s]
            [schema.coerce :as coerce
             :refer [coercer json-coercion-matcher]]
            [forest.route :refer-macros
             [GET PUT ANY expand-handler]]
            [forest.coerce
             :refer [coerce-js coerce-model coerce-models]]
            [cemerick.cljs.test
             :refer-macros
             [is deftest with-test run-tests testing test-var]]
            [cljs.reader :refer [read-string]]))

(deftest edn-test
  (is (= (edn 404 {:foo "bar"})
         {:status 404
          :headers {"Content-Type" "application/edn"}
          :body "{:foo \"bar\"}"}))
  (is (= (edn {:foo "bar"})
         {:status 200
          :headers {"Content-Type" "application/edn"}
          :body "{:foo \"bar\"}"})))

(deftest with-uri-params-test
  (let [mock-request #js {"params" #({"foo" 1 "bar" 2} %)}]
    (is (= (.params mock-request "foo") 1))
    (is (= (.params mock-request "bar") 2))
    (is (= (with-uri-params mock-request
             [foo bar]
             (+ foo bar))
           3))))

(defn status-setter [x v] (assoc x :status v))

(defn headers-setter [x k v] (assoc-in x [:headers k] v))

(defn make-mock-response [an-atom]
  #js {"status" #(swap! an-atom status-setter %)
       "set"    #(swap! an-atom headers-setter %1 %2)})

(def sample-data {:status 0 :headers {}})

(deftest setters-test
  (is (= (status-setter sample-data 300)
         {:status 300 :headers {}}))
  (is (= (headers-setter sample-data "foo" "bar")
         {:status 0 :headers {"foo" "bar"}})))

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

(deftest response-edn-test
  (let [data (atom sample-data)
        mock-response (make-mock-response data)]
    (response mock-response (edn 401 {:hello "world"}))
    (is (= @data
           {:status 401 :headers {"Content-Type" "application/edn"}}))
    (is (= (aget mock-response "body") "{:hello \"world\"}"))))

(defn make-mock-request-params [m]
  #js {"params" #(get m %)})

(deftest mock-request-tests
  (let [mock-request (make-mock-request-params {"foo" 1 "bar" 2})]
    (is (= (.params mock-request "foo") 1))))

(deftest with-uri-params-test
  (let [mock-request (make-mock-request-params {"foo" 1 "bar" 2})]
    (is (= (with-uri-params mock-request [foo bar] (+ foo bar))
           3))))

(deftest with-request-destructuring-test
  (let [mock-request #js {"parameters" #js {"foo" 3 "bar" 4}}]
    (is (= (with-request-destructuring mock-request
             {[foo bar] :parameters}
             (+ foo bar))
           7))))

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

(deftest foxx-route-test
  (let [data (atom sample-data)
        mock-request #js {"headers" #js {"foo" 4 "bar" 6}
                          "params" #(get {"loo" 0 "la" 2} %)}
        mock-response (make-mock-response data)
        mock-app #js {"get" (fn [_ handler]
                              (handler mock-request mock-response))}]
    ((GET "/:loo/:la" {[foo bar] :headers}
           (edn 404 (+ loo la foo bar)))
     mock-app)
    (is (= (aget mock-response "body") "12"))

    (is (= @data {:status 404,
                  :headers {"Content-Type" "application/edn"}}))))

(deftest coercer-tests
  (is (= (js->clj #js {"foo" #js {"bar" 2}}
                  :keywordize-keys true)
         {:foo {:bar 2}}))
  ;; wrap two clauses inside `pr-str`s because javascript objects
  ;; can't be compared by using a plain `=`
  (is (= (pr-str (clj->js {:foo #{:bar}}))
         (pr-str #js {"foo" #js ["bar"]})))
  (is (= (coerce-js {:text s/Str} #js {:text "foo"})
         {:text "foo"}))
  (is (= (coerce-models {:text #{s/Keyword}}
                        (array #js {:attributes #js {:text (array "foo")}}
                               #js {:attributes #js {:text (array "bar")}}))
         [{:text #{:foo}} {:text #{:bar}}])))

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
