(ns arango.repository
  (:require [arango.console :refer [info warn error]]
            [arango.core :refer [Foxx]]
            [schema.core :as s :refer [validate]]
            [forest.coerce
             :refer [coerce-js coerce-model coerce-models]]))

(def ^{:no-doc true :private true}
  db (.-db (js/require "org/arangodb")))

(def ^{:no-doc true :private true}
  Model (.extend (.-Model Foxx) #js {}))

(defn- ^:no-doc ->Repository
  [indexes]
  (.extend (.-Repository Foxx) indexes))

;; Using a weird syntax to force docstrings to appear in
;; generated codox documentation

(defprotocol ^{:doc "ArangoDB collection"}
  ArangoCollection
  ;; Function to add new data
  (^{:doc "Saves an `entry` into the database. This `entry` can be any Clojure data
  structure. Will set the `:_id` and `:_rev` on the entry. Returns the entry."}
   add [this entry])

  ;; Functions to find entries
  (^{:doc "Returns the entry for the given `:_id`."}
   by-id [this id])
  (^{:doc "Returns a vector of entries for the given `example`."}
   by-example [this example])
  (^{:doc "Returns the first entry that matches the given `example`."}
   first-example [this example])

  (^{:doc "Returns a vector of entries that matches the given `example`.
  You can provide both a `skip` and a `limit` value.

  Parameters:

    - `options` (optional):
      + `skip` (optional): skips the first given number of entries.
      + `limit` (optional): only returns at most the given number of entries."}
   all [this] [this options])

  ;; Functions to remove entries
  (^{:doc  "Removes the entry from the repository."}
   remove-entry [this entry])
  (^{:doc "Removes the entry with the given `:_id`. Expects an `:_id` of an existing
  entry."}
   remove-by-id [this id])
  (^{:doc "Finds all entries that fit this `example` and removes them."}
   remove-by-example [this example])

  ;; Replacing entries in the repository
  (^{:doc "Finds the entry in the database by its `:_id` and replace it with this version.
  Expects an `entry`. Sets the `:_rev` of the entry. Returns `nil`."}
   replace-entry [this entry])
  (^{:doc "Finds the entry in the database by the given `:_id` and replaces it
  with the given `entry`. Sets the `:_id` and `:_rev` of the entry. Returns `nil`."}
   replace-by-id [this id entry])
  (^{:doc "Finds the entry in the database by the given `example` and replaces it
  with the given `entry`. Sets the `:_id` and `:_rev` of the entry. Returns `nil`."}
   replace-by-example [this example entry])

  ;; Updating entries in the repository
  (^{:doc "Finds an entry by `:_id` and update it with the keys/values
  in the provided data. Returns `nil`."}
   update-by-id [this id data])
  (^{:doc "Finds an entry by `example` and update it with the key/values
  in the provided `data`. Returns `nil`."}
   update-by-example [this example data])

  ;; Counting entries in the repository
  (^{:doc "Returns the number of entries in this collection."}
   count-all [this])

  ;; Index-specific repository methods
  (^{:doc "Returns all entries in the repository such that the `key` is
  greater than or equal to `left` and strictly less than `right`.

  For range queries it is required that a *skiplist* index is present
  for the queried `key`. If no *skiplist* index is present on the `key`, the
  method will not be available.

  Parameters:

    - `key`: key to query.
    - `left`: lower bound of the value range (inclusive).
    - `right`: upper bound of the value range (exclusive)."}
   key-range [this key left right])

  (^{:doc "Finds entries near the coordinate (latitude,
  longitude). The returned vector is sorted by distance with the
  nearest entry coming first.

  For geo queries it is required that a *geo* index is present in the
  repository.  If no *geo* index is present, the methods will not be
  available.

  Parameters:

    - `lat`: latitude of the coordinate.
    - `lng`: longitude of the coordinate.
    - `options` (optional):
      + `:geo` (optional): name of the specific geo index to use.
      + `:distance` (optional): If set to a truthy value, the returned models will
        have an additional key containing the distance between the given
        coordinate and the entry. If the value is a string, that value will be
        used as the key name, otherwise the name defaults to `:distance`.
      + `:limit` (optional): number of entries to return. Defaults to `100`."}
   near [this lat lng] [this lat lng options])

  (^{:doc "Finds entries within the distance radius from the coordinate
  (latitude, longitude). The returned vector is sorted by distance
  with the nearest entry coming first.

  For geo queries it is required that a *geo* index is present in the
  repository.  If no *geo* index is present, the methods will not be
  available.

  Parameters:

    - `lat`: latitude of the coordinate.
    - `lng`: longitude of the coordinate.
    - `radius`: maximum distance from the coordinate.
    - `options` (optional):
      + `:geo` (optional): name of the specific *geo* index to use.
      + `:distance` (optional): If set to a truthy value, the returned entries
     will have an additional key containing the distance between the given coordinate
     and the entry. If the value is a keyword, that value will be used as the
     key name, otherwise the name defaults to `:distance`.
      + `:limit` (optional): number of entries to return. Defaults to `100`."}
   within [this lat lng radius] [this lat lng radius options])

  (^{:doc "Returns all entries whose `key` value matches the search query `text`.

  In order to use the `fulltext` method, a *fulltext* index must be defined
  on the repository. If multiple *fulltext* indexes are defined on the repository
  for the key, the most capable one will be selected. If no *fulltext* index is
  present, the method will not be available.

  Parameter:

   - `key`: entry key to perform a search on.
   - `text`: text query to match the key's value against.
   - `limit` (optional): number of entries to return. Defaults to *all*."}
   fulltext [this key text] [this key text limit]))

(defrecord ^{:doc "A repository is a gateway to the database. It gets
  data from the database, updates it or adds brand-new data. Conversion
  between Javascript and Clojure datastructure is done automatically."}
    FoxxRepository [repo schema]
    ArangoCollection
    (add [this entry]
      (->> entry
           (validate (:schema this))
           clj->js
           Model.
           (.save (:repo this))
           (coerce-model (:schema this))))
    (by-id [this id]
      (->> id
           (.byId (:repo this)) ;; what about entry not found exception?
           (coerce-model (:schema this))))
    (by-example [this example]
      (->> example
           clj->js
           (.byExample (:repo this))
           (coerce-models (:schema this))))
    (first-example [this example]
      (->> example
           clj->js
           (.firstExample (:repo this))
           (coerce-model (:schema this))))
    (all [this]
      (->> (.all (:repo this))
           (coerce-models (:schema this))))
    (all [this options]
      (->> (.all (:repo this) (clj->js options))
           (coerce-models (:schema this))))
    (remove-entry [this entry]
      (.removeById (:repo this) (:_id entry)))
    (remove-by-id [this id]
      (.removeById (:repo this) id))
    (remove-by-example [this example]
      (->> (clj->js example)
           (.removeByExample (:repo this))))
    (replace-entry [this entry]
      (->> entry
           (validate (:schema this))
           clj->js
           Model.
           (.replace (:repo this)))
      nil)
    (replace-by-id [this id entry]
      (->> entry
           (validate (:schema this))
           clj->js
           Model.
           (.replaceById (:repo this) id))
      nil)
    (replace-by-example [this example entry]
      (->> entry
           (validate (:schema this))
           clj->js
           Model.
           (.replaceByExample (:repo this) (clj->js example))
           ;; Strange behavior:
           ;; conversion from js-obj to Clojure produces
           ;; an unexpected extra key whose value is
           ;; affected documents' count.
           ;; (.stringify js/JSON)
           ;; (.parse js/JSON)
           ;; (coerce-model (:schema this))
           )
      nil)
    (update-by-id [this id data]
      (->> (clj->js data)
           (.updateById (:repo this) id)))
    (update-by-example [this example data]
      (->> (clj->js data)
           (.updateByExample (:repo this) (clj->js example))))
    (count-all [this]
      (.count (:repo this)))

    (key-range [this key left right]
      (->> (.range (:repo this) key left right)
           (coerce-models (:schema this))))

    (near [this lat lng]
      (->> (.range (:repo this) lat lng)
           (coerce-models (:schema this))))
    (near [this lat lng options]
      (->> (clj->js options)
           (.range (:repo this) lat lng)
           (coerce-models (:schema this))))

    (within [this lat lng radius]
      (->> (.within (:repo this) lat lng)
           (coerce-models (:schema this))))
    (within [this lat lng radius options]
      (->> (clj->js options)
           (.within (:repo this) lat lng)
           (coerce-models (:schema this))))

    (fulltext [this key text]
      (->> (.fulltext (:repo this) (name key) text)
           (coerce-models (:schema this))))
    (fulltext [this key text limit]
      (->> (.fulltext (:repo this) (name key) text #js {:limit limit})
           (coerce-models (:schema this)))))

(defn app-repository
  "Creates a new instance of Repo inside current application context.

  Repository can take care of ensuring the existence of collection indexes
  for you. If you define indexes for a repository, instances of the repository
  will have access to additional index-specific methods like `range` or `fulltext`.

  Parameters:

    - `repo`: name of the repo, as string
    - `schema`: the *schema* against which entries will be validated and coerced.
    - `indexes` (optional): a vector of indexes.
      + For example: `[{:type :fulltext, :fields [:text], :minLength 3}]`"
  ([repo schema] (app-repository repo schema nil))
  ([repo schema indexes]
     (let [RepoType (->Repository (when indexes
                                    #js {:indexes (clj->js indexes)}))
           repo-coll (.collection js/applicationContext repo)
           schema (assoc schema
                    ;; keys generated by arangodb are marked as optional
                    (s/optional-key :_id)  s/Str
                    (s/optional-key :_rev) s/Str
                    (s/optional-key :_oldRev) s/Str
                    (s/optional-key :_key) s/Str)]
       (if repo-coll
         (FoxxRepository. (RepoType. repo-coll) schema)
         (error "Collection % does not exist!" repo)))))

(defn create-collection!
  "Creates a new collection with given name for current application context.
  Returns `true` if created successfully. Otherwise returns `nil`."
  [coll-name]
  (let [collection (.collectionName js/applicationContext coll-name)]
    (if (._collection db collection)
      (error "Collection %s already exists." coll-name)
      (do (._create db collection)
          true))))

(defn drop-collection!
  "Deletes a new collection with given name for current application context.
  Returns `true` if deleted successfully. Otherwise returns `nil`."
  [coll-name]
  (let [collection (.collectionName js/applicationContext coll-name)]
    (if (._collection db collection)
      (do (._drop db collection)
          true)
      (error "Collection %s does not exist." coll-name))))

(defn truncate-collection!
  "Truncates a collection with given name for current application context,
  removing all documents but keeping all its indexes.
  Returns `true` if deleted successfully. Otherwise returns `nil`."
  [coll-name]
  (let [collection (.collectionName js/applicationContext coll-name)]
    (if (._collection db collection)
      (do (._truncate db collection)
          true)
      (error "Collection %s does not exist." coll-name))))
