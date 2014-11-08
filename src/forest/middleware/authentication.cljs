(ns forest.middleware.authentication
  (:require-macros [forest.middleware :refer [defmiddleware defmiddleware-before]])
  (:require [arango.console :refer [info]]
            [forest.route :refer [*app* *request* *response*]
             :refer-macros [defroutes GET POST PUT DELETE PATCH]]
            [arango.repository :refer [create-collection!]]
            [forest.middleware.edn :refer-macros [enable-destructuring-edn-parameters]]
            [forest.response :refer [edn]]
            [arango.core :refer [Foxx internal]]))

(def foxx-auth (js/require "org/arangodb/foxx/authentication"))

(def errors (.-errors internal))

(def no-error (.-ERROR_NO_ERROR errors))

(def Sessions (.-Sessions foxx-auth))
(def Users (.-Users foxx-auth))
(def CookieAuthentication (.-CookieAuthentication foxx-auth))
(def Authentication (.-Authentication foxx-auth))

(defmiddleware wrap-cookies
  "Activates cookies for the given Foxx application `app`.
  Populates `app` with a `cookies` attribute holding the
  `cookieAuthentication` instance.

  Accepts the following optional keyword arguments:

   - `:cookie-lifetime`: An integer. Lifetime of cookies in seconds. Default: `360000`
   - `:cookie-name`: A string used as the name of the cookie session. Default: `forest-app`"
  [& {:keys [cookie-lifetime cookie-name]}]
  (when-not (.-cookies *app*)
    (info "Cookies not found, initializing...")
    (let [cookieAuth (->> #{:cookieLifetime (or cookie-lifetime 360000)
                            :cookieName     (or cookie-name "forest-app")}
                          (CookieAuthentication. js/applicationContext))]
      (aset *app* "cookies" cookieAuth))))

(defmiddleware wrap-sessions
  "Activates sessions for the given Foxx application `app`.
  Populates `app` with a `sessions` attribute holding the `sessions`
  instance.

  Accepts the following optional keyword arguments:

    - `:session-lifetime`: An integer. Lifetime of sessions in seconds. Default: `600`"
  [& {:keys [session-lifetime]}]
  (when-not (.-sessions *app*)
    (info "Sessions not found, initializing...")
    (create-collection! "sessions")
    (let [sessions (Sessions. js/applicationContext
                              #js {:sessionLifetime (or session-lifetime 600)})]
      (aset *app* "sessions" sessions))))

(defmiddleware wrap-users
  "Activates sessions for the given Foxx application `app`.
  Populates `app` with a `users` attribute holding the `users`
  instance."
  []
  (when-not (.-users *app*)
    (info "Users not found, initializing...")
    (create-collection! "users")
    (let [users (Users. js/applicationContext)]
      (aset *app* "users" users))))

(defn populate-session!
  "Populates a session into current `*request*`'s attribute named
  `currentSession` "
  [session]
  (aset *request* "currentSession" session))

(defn unpopulate-session!
  "Removes the session brought into `*request*` by `populate-session!`"
  []
  (aset *request* "currentSession" nil))

(defn populate-user!
  "Populates an user into current `*request*`'s attribute named
  `user` "
  [user]
  (aset *request* "user" user))

(defn unpopulate-user!
  "Removes the user brought into `*request*` by `populate-user!`"
  []
  (aset *request* "user" nil))

(defn authenticate
  "Authenticates `*request*`. Returns the session if succeeeded, else
  returns `nil`."
  []
  (let [auth-result (.authenticate (.-auth *app*) *request*)]
    (when-not (= (.-errorNum auth-result) no-error)
      (.-session auth-result))))

(defn update-session!
  "Updates the current session in *request* if found."
  []
  (when-let [session (.-currentSession *request*)]
    (.update session)))

(defn find-user
  "If an user with such `user-id` is found in database, returns
  it. Else returns `nil`."
  [user-id]
  (.get (.-users *app*) user-id))

(defn session->user
  "Receives a `session`, finds an *user* associated with it."
  [session]
  (find-user (.-identifier session)))

(defn current-session
  "If there is a session populated in current `*request*`, returns
  it. Else returns `nil`."
  []
  (-> *request* .-currentSession))

(defn current-user
  "If there is a user populated in current `*request*`, returns
  it. Else returns `nil`."
  []
  (-> *request* .-user))

(defn current-user-id
  "Returns `user-id` of the user associated with current
  `*request*`. If no such user is found, returns `nil`."
  []
  (.-identifier (current-session)))

(defn current-session-id
  "Returns `_key` of the session associated with current
  `*request*`. If no such session is found, returns `nil`."
  []
  (.-_key (current-session)))

(defn user-exists?
  "Checks if an user with that *username* in database."
  [username]
  (.exists (.-users *app*) username))

(defn add-user
  "Add an user with given properties to database."
  [username password active? & [data]]
  (.add (.-users *app*) username password active? (clj->js data)))

(defn set-password
  "Sets password for a given username."
  [username password]
  (.setPassword (.-users *app*) username password))

(defn begin-authenticated-session
  "Begins an authenticated session for a given user"
  ([username]
     (.beginSession (.-auth *app*) *request* *response* username))
  ([username data]
     (.beginSession (.-auth *app*) *request* *response* username (clj->js data))))

(defn end-authenticated-session
  "Begins an authenticated session."
  []
  (.endSession (.-auth *app*) *request* *response* (current-session-id)))

(defn validate
  "Checks if a pair of username/password is valid."
  [username password]
  (.isValid (.-users *app*) username password))

(defmiddleware-before wrap-authentication-request
  "Adds session data to incoming requests."
  []
  (fn [req res]
    (binding [*request* req]
      (update-session!)
      (if-let [session (authenticate)]
        (do (populate-session! session)
            (populate-user! (session->user session)))
        (do (unpopulate-session!)
            (unpopulate-user!))))))

(defmiddleware wrap-authentication*
  "Activates authentication for the given Foxx application `app`.
  Populates `app` with a `auth` attribute holding the
  `Authentication` instance.

  Requires `wrap-cookies` and `wrap-sessions` to function."
  []
  (let [sessions (.-sessions *app*)
        cookies (.-cookies *app*)]
    (if-not (.-auth *app*)
      (->> (Authentication. js/applicationContext sessions cookies)
           (aset *app* "auth")))))

(defmiddleware-before wrap-populate-session-and-user
  "Populates *session* and *user* to incoming requests."
  []
  (fn [req res]
    (binding [*request* req]
      (update-session!)
      (if-let [session (authenticate)]
        (do (populate-session! session)
            (populate-user! (session->user session)))
        (do (unpopulate-session!)
            (unpopulate-user!))))))

(defmiddleware wrap-authentication
  []
  (-> *app*
      (wrap-cookies)   ;; configurations!!!
      (wrap-sessions)  ;; configurations!!!
      (wrap-users)     ;; configurations!!!
      (wrap-authentication-request)
      (wrap-authentication*)
      (wrap-populate-session-and-user)))

(enable-destructuring-edn-parameters)

(defroutes basic-authentication
  (GET "/auth" {}
       (if (current-session)
         (edn {:user (current-user-id) :key (current-session-id)})
         (edn 401 {:message "Not logged in!"})))
  (POST "/auth" {{:keys [username password]} :edn-parameters}
        (if (validate username password)
          (do
            (populate-session! (begin-authenticated-session username))
            (populate-user! (session->user (current-session)))
            (edn {:user (current-user-id) :key (current-session-id)}))
          (edn 401 {:message "Username or Password was wrong"})))
  (DELETE "/auth" []
          (if-let [session (authenticate)]
            (do
              (end-authenticated-session)
              (unpopulate-session!)
              (unpopulate-user!)
              (edn {:message "Logged out!"}))
            (edn 401 {:message "No session was found"})))
  (PUT "/auth"  {{:keys [username password]} :edn-parameters}
       (if-not (user-exists? username)
         (do
           (add-user username password true)
           (populate-session! (begin-authenticated-session username))
           (edn {:username username}))
         (edn 401 {:message "Registration failed"})))
  (PATCH "/auth"  {{:keys [password]} :edn-parameters}
         (if (current-session)
           (if (set-password (current-user-id) password)
             (edn {:message "Password changed"})
             (edn 401 {:message "Password not changed"}))
           (edn 401 {:message "Not logged in!"}))))
