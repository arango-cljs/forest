(defproject arango-cljs "0.1.0-SNAPSHOT"
  :description "Utitlities to write ArangoDB/Foxx app in Clojurescript"
  :url "http://github.com/arango-cljs/arango-cljs"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2268"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [prismatic/schema "0.2.5"]
                 [org.clojure/core.match "0.2.1"]]
  :source-paths ["src-cljs" "src/cljs" "src/clj" "target/classes"]
  :test-paths ["target/test-classes"]
  :jar-exclusions [#"\.cljx" #"sample-app"]
  :codox {:writer codox-md.writer/write-docs}
  :plugins [[codox "0.6.4"]]
  :hooks [cljx.hooks]
  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-2227"]
                                  [com.cemerick/clojurescript.test "0.3.1"]
                                  [codox-md "0.2.0" :exclusions [org.clojure/clojure]]]
                   :plugins [[com.keminglabs/cljx "0.4.0"]
                             [lein-cljsbuild "1.0.3"]
                             [com.cemerick/clojurescript.test "0.3.1"]
                             [com.cemerick/austin "0.1.5-SNAPSHOT"]]
                   :cljx {:builds [{:source-paths ["src/cljx"]
                                    :output-path "target/classes"
                                    :rules :clj}

                                   {:source-paths ["src/cljx"]
                                    :output-path "target/classes"
                                    :rules :cljs}

                                   {:source-paths ["test/cljx"]
                                    :output-path "target/test-classes"
                                    :rules :clj}

                                   {:source-paths ["test/cljx"]
                                    :output-path "target/test-classes"
                                    :rules :cljs}]}

                   :aliases {"cleantest" ["do" "clean," "cljx" "once," "test," "cljsbuild" "test"]
                             "deploy" ["do" "clean," "cljx" "once," "push"]}

                   :cljsbuild {:test-commands {"phantom" ["phantomjs" :runner "target/testable.js"]
                                               "node" ["node" :node-runner "target/testable.js"]}
                               :builds [{:id "dev"
                                         :source-paths ["src-cljs" "src/cljs" "target/classes" "sample-app/dev"]
                                         :compiler {:output-to "sample-app/app.js"
                                                    :optimizations :simple}}
                                        {:id "test"
                                         :source-paths ["src/cljs" "target/classes" "target/test-classes"]
                                         ;; Running `cljsbuild <once|auto>` will trigger this test.
                                         :notify-command ["phantomjs" :cljs.test/runner
                                                          "target/testable.js"]
                                         :compiler {:output-to "target/testable.js"
                                                    :optimizations :simple}}]}}}
  )
