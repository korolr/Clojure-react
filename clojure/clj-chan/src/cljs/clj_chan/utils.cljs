(ns clj-chan.client.utils
  "A bunch of utils used in other namespaces."
  (:require [clojure.browser.repl :as repl]))


;; For live coding
(repl/connect "http://localhost:9000/repl")

(defn log [m] (.log js/console m))

