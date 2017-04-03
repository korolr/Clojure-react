(ns clj-chan.handlers
  "Handlers (controllers) of an app - both for HTTP and WebSocket
connections."
  (:require [clj-chan.data-source :as ds]
            [clj-chan.templates :as templates]
            [org.httpkit.server :as s]))


;; ## WebSocket

;; Each request is a valid Clojure map with required keys :action and :topic.
;; Actions could also use some additional keys.
;;
;; :topic specifies a board on which page this request was originated.
;;
;; :action specifies actual action that this request represent. Currently
;; supports only two:
;;
;; 1) :post - adds a message stored in :post key to a data source and sends it
;; to all clients subscribed to the same board
;;
;; 2) :init - initializes a connection with server, subscribes client to a
;; specified board and sends all existing messages in it to subscriber

(defmulti on-message
  "Responds to a client's WebSocket request based on its :action."
  (fn [db subscriptions ws-conn message] (:action message)))

(defmethod on-message :post
  [db subscriptions ws-conn message]
  (let [{:keys [topic post]} message
        post (ds/add-post db topic post)]
    (doseq [c (get @subscriptions topic [])]
      (s/send-mesg c (pr-str post)))))

(defmethod on-message :init
  [db subscriptions ws-conn message]
  (let [{:keys [topic]} message]
    (when-not (ds/topic-exists? db topic) (ds/add-topic db topic))
    (swap! subscriptions update-in [topic] #(into #{} (conj % ws-conn)))
    (doseq [p (ds/get-posts db topic)] (s/send-mesg ws-conn (pr-str p)))))

(defn generate-on-message
  "Generates a handler for WebSocket incoming message - actual action depends
on the message :action element (it's assumed that each message is a string
with correct Clojure map.

Generated handler uses a given database, subscription and WebSocket
connection."
  [db subscriptions ws-conn]
  (fn [m]
    (on-message db subscriptions ws-conn (read-string m))))

(defn generate-on-close
  "Generates a handler for WebSocket close connection event - removes client
from the current board subscription.

Generated handler uses a given database, subscription and WebSocket
connection."
  [db subscriptions ws-conn]
  (fn [status]
    (letfn [(remove-connection [s]
              (into {} (map #(let [[k v] %] [k (disj v ws-conn)]) s)))]
        (swap! subscriptions remove-connection))))

;; ## Board Handler

(defn board-handler
  "Generates a WebSocket and HTTP handler for a board page. Uses a given data
base and atom (map) for storing clients' subscriptions to boards."
  [db subscriptions]
  (fn [request]
    (let [{:keys [board]} (:params request)]
      (s/if-ws-request
       request ws-conn
       (do (s/on-mesg ws-conn (generate-on-message db subscriptions ws-conn))
           (s/on-close ws-conn (generate-on-close db subscriptions ws-conn)))
       (templates/board-view board)))))

;; ## Boards-list

(defn boards-list-handler
  [db]
  (fn [request] (templates/boards-list (ds/get-topics db))))

;; ## Login Handler

(defn login-handler
  "Ring handler for a login page."
  [_]
  templates/login-view)
