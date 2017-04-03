(ns clj-chan.client.board
  "Defines main operations with a specific board's page."
  (:require        [clj-chan.client.web-socket :as socket]
                   [clj-chan.client.utils :as u]
                   [enfocus.core :as ef]
                   [dommy.template :as t])
  (:require-macros [enfocus.macros :as em]))

(defn gen-post-html
  "Generates a DOM element which contains post."
  [post]
  (let [{author :author image :image text :text} post]
    (t/node
    [:div.post
     [:div.post-author author]
     [:div.post-image
      (when (seq image)
        [:a {:href image :target "_blank"}
         [:img {:src image :alt "some pic"}]])]
     [:div.post-text text]
     [:hr]])))

(defn show-post
  "Adds a new post."
  [post]
  (em/at js/document
         ["div#posts > div#post-anchor"]
         (em/after (gen-post-html post))))

(defn read-new-post-data []
  (let [post (em/from js/document
                      :author ["#new-post #new-author"] (em/get-prop :value)
                      :image ["#new-post #new-image"] (em/get-prop :value)
                      :text ["#new-post #new-text"] (em/get-prop :value))]
    (into {} (filter #(not ((comp empty? second) %)) post))))

(em/defaction setup [ws]
  ["#post-submit"]
  (em/listen
   :click #(socket/send-message ws
                                (merge
                                 ;; TODO fix this (send actual topic, not all
                                 ;; path
                                 {:topic (.-pathname (.-location js/window))
                                  :action :post}
                                 {:post (read-new-post-data)}))))

(defn start []
  (let [ws (socket/ws)]
    (socket/init-ws ws
                    (assoc socket/ws-handlers
                      :onopen
                      (fn []
                        (socket/send-message
                         ws
                         {:topic (.-pathname (.-location js/window))
                          :action :init}))
                      :onmessage
                      (fn [i] (let [post (socket/decode-post i)]
                               (show-post post)))))
    (setup ws)))

(set! (.-onload js/window) #(start))


