(ns foxx-cljs.middleware
  (:require [foxx-cljs.arango
             :refer [Foxx foxx-auth arango-is errors FormatMiddleware
                     Sessions Users CookieAuthentication Authentication]]
            [cljs.reader :refer [read-string]]
            [foxx-cljs.route :refer [edn response] :as core]
            [foxx-cljs.repository :refer [create-collection!]]
            [foxx-cljs.route
             :refer-macros [defmiddleware]]))

(defn edn-request?
  [req]
  (when-let [content-type (-> (aget req "headers")
                              (aget "content-type"))]
    (seq (re-find #"^application/(vnd.+)?edn" content-type))))

(defmiddleware wrap-edn []
  [req res]
  (when (edn-request? req)
    (->> (.rawBody req)
         ;; TODO: what about empty rawBody? invalid edn?
         read-string
         (aset req "ednParameters"))))

(defn wrap-format [routes & extensions]
  (fn [app]
    (routes app)
    (->> extensions
         (apply array)
         (FormatMiddleware.)
         (.before app))))

(defn add-session! [req session user]
  (aset req "currentSession" session)
  (aset req "user" user))

(defn clear-session! [req]
  (aset req "currentSession" nil)
  (aset req "user" nil))

(defn wrap-authentication [routes path]
  (fn [app]
    (routes app)
    (create-collection! "users")
    (create-collection! "sessions")
    (let [sessions (Sessions. js/applicationContext
                              #js {:sessionLifetime 400})
          cookieAuth (CookieAuthentication. js/applicationContext
                                            #js {:cookieName "my_cookie"
                                                 :cookieLifetime 360000})
          auth (Authentication. js/applicationContext sessions cookieAuth)
          users (Users. js/applicationContext)]
      ;; authenticate requests
      (.before app (fn [req res]
                     (let [auth-result (.authenticate auth req)
                           session (.-currentSession req)]
                       (when (.existy arango-is session)
                         (.update session))
                       (if (= (.-errorNum auth-result) (.-ERROR_NO_ERROR errors))
                         (add-session! req
                                       (.-session auth-result)
                                       (.get users (-> auth-result .-session .-identifier)))
                         (clear-session! req)))))
      ;; login
      (.post app path
             (fn [req res]
               (let [{:keys [username password]} (aget req "ednParameters")]
                 (if (.isValid users username password)
                   (do
                     (add-session! req
                                   (.beginSession auth req, res, username, #js {})
                                   (.get users (-> req .-currentSession .-identifier)))
                     (response res (edn {:user (-> req .-user .-identifier)
                                         :key (-> req .-currentSession .-_key)})))
                   (response res (edn 401
                                      {:message "Username or Password was wrong"}))))))
      ;; logout
      (.del app path
            (fn [req res]
              (if (.existy arango-is (.-currentSession req))
                (do
                  (.endSession auth req, res, (-> req .-currentSession .-_key))
                  (clear-session! req)
                  (response res (edn {:message "Logged out!"})))
                (response res (edn 401 {:message "No session was found"})))))
      ;; register
      (.put app path
            (fn [req res]
              (let [{:keys [username password]} (aget req "ednParameters")]
                (if-not (.exists users username)
                  (do
                    (add-session! req
                                  (.beginSession auth req res username {})
                                  (.add users username password true #js {}))
                    (response res (edn {:username username})))
                  (response res (edn 401 {:message "Registration failed"}))))))
      ;; change password
      (.patch app path
            (fn [req res]
              (let [{:keys [password]} (aget req "ednParameters")]
                (if (.existy arango-is (.-currentSession req))
                  (if (.setPassword users (-> req .-currentSession .-identifier) password)
                    (response res (edn {:message "Password changed"}))
                    (response res (edn 401 {:message "Password not changed"})))
                  (response res (edn 401 {:message "Not logged in!"})))))))))
