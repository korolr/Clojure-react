(ns clj-chan.config
  "Imageboard's default configuration."
  (:require [cemerick.friend.credentials :as creds]))

(def default-config
  {:port 1337
   ;; TODO read from system env properties
   :db-connection-string "mongodb://root:root@127.0.0.1/chan"
   ;; TODO shoudln't be here
   :users {"root" {:username "root"
                   :password (creds/hash-bcrypt "root")
                   :roles #{:user}}}})
