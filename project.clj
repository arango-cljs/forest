(defproject arango-cljs/forest "0.2.0"
  ;; When version changes, remember to change :output-dir and :src-dir-uri in :codox too
  :description "Forest - a cozy home for (ArangoDB) Foxx applications.
  Write scalable, database-ready APIs and apps in Clojurescript with ease."
  :url "http://github.com/arango-cljs/forest"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [prismatic/schema "0.3.2"]]
  :jar-exclusions [#"sample-app"]
  :profiles {:dev {:dependencies [[com.cemerick/clojurescript.test "0.3.1"]
                                  [codox-md "0.2.0" :exclusions [org.clojure/clojure]]]
                   :plugins [[codox "0.8.10"]
                             [lein-cljsbuild "1.0.3"]
                             [com.cemerick/clojurescript.test "0.3.1"]]
                   :codox {:language :clojurescript
                           :include [forest.response forest.middleware forest.middleware.edn forest.middleware.powered-by
                                     forest.route.destructuring forest.route forest.coerce
                                     forest.repository arango.fs arango.console arango.core arango.http-client]
                           :output-dir "doc/0.2.0/"
                           :src-dir-uri "http://github.com/arango-cljs/forest/blob/0.2.0/"
                           :src-linenum-anchor-prefix "L"
                           :defaults {:doc/format :markdown
                                      :doc "FIXME: write docs"}}
                   :cljsbuild {:test-commands {"node" ["node" :node-runner "target/testable.js"]}
                               :builds [{:id "dev"
                                         :source-paths ["src" "sample-app/src"]
                                         :compiler {:output-to "sample-app/app.js"
                                                    :optimizations :simple}}
                                        {:id "test"
                                         :source-paths ["src" "test"]
                                         ;; Running `cljsbuild <once|auto>` will trigger this test.
                                         :notify-command ["node" :node-runner "target/testable.js"]
                                         :compiler {:output-to "target/testable.js"
                                                    :optimizations :simple}}]}}}
  )
