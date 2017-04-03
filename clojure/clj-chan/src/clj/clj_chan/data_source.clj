(ns clj-chan.data-source
  "Functions to interact with basic imageboard's entities: board, topic,
post."
  (:require [monger.core :as mg]
            [monger.collection :as mgc])
  (:import [org.bson.types ObjectId]
           [com.mongodb WriteConcern]))


;; ## DB protocols

;; TODO create few separate protocols
;; (something like db-reader, db-writer, db-maintenance (init/close)...)
(defprotocol BoardDAO
  "Describes an interface to a chan's board - collection of posts."
  (add-topic [self topic]
    "Adds a new topic (specified by name) to board.")
  (topic-exists? [self topic]
    "Checks whether the board has the given topic (specified by name).")
  (get-topics [self]
    "Returns all existing topics on board (set of their names).")
  (add-post [self topic post]
    "Adds a new post (map with optional keys :author, :image, :text to a
board's topic (specified by name).")
  (get-posts [self topic]
    "Returns all posts from a board's topic (specified by name) in a form of
list of map instances.

Each post contains keys :author, :date, :image, :text."))

;; ## Common utils

(defn create-post
  "Creates a post in a given topic using the given function.

Post given to a savefn is a map with keys :_id, :author, :date, :image and
:text. Returns the same map, but without the :_id."
  [topic post save-fn]
  (let [{:keys [author image text]
         :or {author "Anon" text "" image ""}} post
        text (if (and (empty? text) (empty? image)) "sth" text)
        post {:_id (ObjectId.) :author author :date (java.util.Date.)
              :image image :text text}]
    (save-fn post)
    (dissoc post :_id)))

;; ## In-memory data source

;;  atom with map {:topic [post_1 ... post_n]}.
(defrecord InMemoryBoard [posts-atom]
  BoardDAO
  (add-topic [_ topic]
    (when-not (get @posts-atom topic)
      (swap! posts-atom assoc topic [])))
  (get-topics [_]
    (into #{} (keys @posts-atom)))
  (topic-exists? [_ topic]
    (contains? @posts-atom topic))
  (add-post [_ topic post]
    (create-post topic post
                 #(swap! posts-atom update-in [topic] (fn [ps] (conj ps %)))))
  (get-posts [_ topic]
    (map #(dissoc % :_id) (get @posts-atom topic []))))

;; ## MongoDB data source

(defn get-mongo-db
  "Connects to a MongoDB instance and returns object that represents it."
  [connection-string]
  (mg/get-db (mg/connect-via-uri! connection-string) "chan"))

(defrecord MongoDBBoard [db]
  BoardDAO
  (add-topic [self topic]
    (when-not (topic-exists? self topic)
      (mgc/insert-and-return db "topics"
                             {:_id (ObjectId.) :name topic :posts []}
                             WriteConcern/SAFE)))
  (get-topics [_]
    (into #{} (map :name (mgc/find-maps db "topics" {} [:name]))))
  (topic-exists? [_ topic]
    (mgc/any? db "topics" {:name topic}))
  (add-post [_ topic post]
    (create-post topic post
                 #(mgc/update "topics" {:name topic} {"$push" {:posts %}})))
  (get-posts [_ topic]
    (if-let [posts (first (mgc/find-maps db "topics" {:name topic} [:posts]))]
      (map (comp #(dissoc % :_id) (partial into {})) (:posts posts))
      [])))
