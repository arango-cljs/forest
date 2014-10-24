(ns forest.destructuring-test
  (:require [clojure.string :as string]
            [forest.destructuring :refer :all]
            [clojure.test :refer [is deftest testing run-tests]]))

(deftest uri-param-bindings-test
  (is (= (get-uri-param 'a-request 'foo)
         '[foo (.params a-request "foo")]))
  (is (= (uri-param-bindings 'a-request '[foo bar])
         '[foo (.params a-request "foo")
           bar (.params a-request "bar")])))

(deftest complete-map-binding-forms-test
  (is (= (complete-map-binding-forms '{a :x b :y} 'foo)
         '[a (clojure.core/aget foo "x")
           b (clojure.core/aget foo "y")])))

(deftest keys->map-binding-forms-test
  (is (= (keys->map-binding-forms '[a b])
         '{a :a, b :b})))

(deftest destructure-json-map-test
  (is (= (destructure-json-map
          '{m :a n :b-z p "NotAKeyword" :keys [c d] :as x} 'value)
         '[x (cljs.core/js->clj value)
           c (clojure.core/aget value "c")
           d (clojure.core/aget value "d")
           m (clojure.core/aget value "a")
           n (clojure.core/aget value "bZ")
           p (clojure.core/aget value "NotAKeyword")])))

(deftest destructure-json-vector-test
  (is (= (destructure-json-vector '[foo bar] 'value)
         '[foo (clojure.core/aget value "foo")
           bar (clojure.core/aget value "bar")]))
  (is (= (destructure-json-vector '[foo bar :as loo] 'value)
         '[loo (cljs.core/js->clj value)
           foo (clojure.core/aget value "foo")
           bar (clojure.core/aget value "bar")])))

(run-tests)
