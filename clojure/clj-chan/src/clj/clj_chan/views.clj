(ns clj-chan.views
  "Templates to render basic pages of the imageboard."
  (:require [hiccup.page :as hp]
            [hiccup.form :as hf]))


;; ## Common elements

(def style
  (hp/html5
   (hp/include-css (str "http://fonts.googleapis.com/css?"
                        "family=Press+Start+2P&subset=latin,cyrillic"))
   (hp/include-css "/css/board.css")))

;; ## Login page

(def login-view
  (hp/html5
   [:head
    [:title "login"]
    style]
   [:body
    (hf/form-to
     [:post "/login"]
     [:div
      (hf/label "username" "Username")
      (hf/text-field "username")]
     [:div
      (hf/label "password" "Password")
      (hf/password-field "password")]
     [:div (hf/submit-button "login")])]))

;; ## Board page

(defn board-view
  "HTML base for a specific board."
  [topic]
  (hp/html5
   [:head
    [:title (str "Best chan ever - /" topic)]
    style
    (hp/include-js "/js/board.js")]
   [:body
    [:header (str "/" topic)]
    [:div#new-post
     [:div
      (hf/label "new-author" "Name")
      (hf/text-field {:id "new-author"} "new-author")]
     [:div
      (hf/label "new-image" "Image link")
      (hf/text-field {:id "new-image"} "new-image")]
     [:div
      (hf/label "new-text" "Comment")
      (hf/text-area {:id "new-text"} "new-text")]
     [:div (hf/submit-button {:id "post-submit"} "Add post")]]
    [:hr]
    [:div#posts [:div#post-anchor]]]))
