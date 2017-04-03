(ns clj-chan.server
  "Entry point of the imageboard - routes are defined here as well as
server's start function."
  (:require [clj-chan.config :as conf]
            [clj-chan.data-source :as ds]
            [clj-chan.handlers :as handlers]
            [compojure.core :as c]
            [compojure.route :as route]
            [compojure.handler :as ch]
            [org.httpkit.server :as s]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]))
  (:gen-class :main true))


;; ## Ring application

(defn app
  "Generates a Ring-compatible handler for imageboard that uses a given
database and uses a given atom with map for storing clients' subscriptions
to topics."
  [db subs]
  (c/routes
   (c/GET
    ["/"] []
    (friend/authorize #{:user} (handlers/boards-list-handler db)))
   (c/GET
    ["/boards/:board", :board #"[a-zA-Z0-9_\-]+"] []
    (friend/authorize #{:user} (handlers/board-handler db subs)))
   (c/GET "/login" [] handlers/login-handler)
   (friend/logout (c/ANY "/logout" [] (ring.util.response/redirect "/login")))
   (route/resources "/")
   (route/not-found "Page not found")))

(defn wrapped-app
  "Generates a Ring-compatible handler for imageboard that uses a given
database and uses a given atom with map for storing clients' subscriptions
to topics with additional enabled Friend authentication and Compojure site
middleware. Initial settings are from a specified map."
  [settings db subs]
  (-> (app db subs)
      (friend/authenticate
       {:credential-fn (partial creds/bcrypt-credential-fn (:users settings))
        :workflows [(workflows/interactive-form)]
        :default-landing-uri "/"})
      ch/site))

;; ## Server

(defn start-server
  "Starts the imageboard with given settings."
  [settings]
  (let [settings (merge conf/default-config settings)
        {:keys [port db-connection-string]} settings]
    (s/run-server
     (wrapped-app
      settings
      (ds/->MongoDBBoard (ds/get-mongo-db db-connection-string))
      (atom {}))
     {:port port})))

(defn -main [& args]
  ;; TODO transform args to map
  (start-server {}))
