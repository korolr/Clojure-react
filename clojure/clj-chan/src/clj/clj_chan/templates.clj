(ns clj-chan.templates
  "Templates to render basic pages of the imageboard."
  (:require [hiccup.core :as hc]
            [hiccup.page :as hp]
            [hiccup.element :as he]
            [hiccup.form :as hf]))


;; ## Common elements

(def style
  (hc/html
   (hp/include-css (str "http://fonts.googleapis.com/css?"
                        "family=Press+Start+2P&subset=latin,cyrillic"))
   (hp/include-css "/css/board.css")))

(def menu
  (hc/html
   [:div#menu
    (he/link-to "/" [:button "Boards list"])
    (he/link-to "/logout" [:button "Exit"])]))

;; ## Login page

(def login-view
  "Login form."
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
  [board-name]
  (hp/html5
   [:head
    [:title (str "Best chan ever - /" board-name)]
    style
    (hp/include-js "/js/board.js")]
   [:body
    [:header (str "/" board-name)]
    menu
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

;; ## Index page

(defn board-element
  [name]
  (hc/html
   [:div.board-element
    (he/link-to name name)]))

(defn boards-list
  "Shows a list of all open boards."
  [boards]
  (hp/html5
   [:head
    [:title (str "Best chan ever")]
    style]
   [:body
    [:header (str "Choose your destiny...")]
    menu
    [:div#boards-list
     (doall (map board-element boards))]]))
