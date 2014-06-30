(ns fowles.grab
  (:use [net.cgrand.enlive-html])
  (:require [net.cgrand.enlive-html :as enlive]
            [clj-http.client :as client]
            [clojure.string :as str]))

;;
;; <cite>www.<b>youtube.com/channel/</b>...ID...</cite>
;;
;; (defn extract-channel-names [html]
;;   (let [ch-names (re-find #"<cite>www.<b>youtube\.com/channels/UC</b>/(.*?)</cite>" html)]
;;     ch-names))


(def USER_AGENT "Firefox/3.0.15")

(defn fetch-html
  "Downloads a document as html"
  [uri]
  (-> (client/get uri {:headers {"User-Agent" USER_AGENT}})
      :body))

(defn fetch-enlive
  "Downloads a document as an html-resource"
  [uri]
  (let [html (fetch-html uri)]
    (enlive/html-snippet html)))

(def NUM_PER_PAGE 100)
(def ROOT_URI "http://www.google.com/search")
(def QUERY "site:youtube.com+youtube.com/channels/UC")
(defn- page-uri [prefix n]
  (str ROOT_URI "?q=" QUERY prefix
       "&num=" NUM_PER_PAGE
       "&start=" (* n NUM_PER_PAGE)))

;; div.srg li.g div.rc h3.r a:href

(def LEN_OF_CHAN_ID 24)

(defn with-slash? [tag-content]
  (try
    (= "/" (nth tag-content 2))
    (catch Exception e false)))

(defn mk-id-str [tag-content]
  (try
    (str (first (:content (nth tag-content 3)))
         (nth tag-content 4))
    (catch Exception e nil)))

(defn of-proper-length? [str]
  (= LEN_OF_CHAN_ID (count str)))

(defn extract-channel-names [snippet]
  ;; (select snippet [:div.srg :li.g :div.rc :h3.4 :a]))
  (->> (select snippet [:cite])
       (map :content)
       (filter with-slash?)
       (map mk-id-str)
       (filter of-proper-length?)))

(def CHARS
  (concat [\_ \-] (map char (concat (range 48 58) (range 97 123)))))

(defn make-prefixes [len]
  (for [x CHARS
        y CHARS
        z CHARS]
    (str x y z)))

(defn -main []
  (doseq [prefix (make-prefixes 3)]
    (let [uri (page-uri prefix 0)
          snippet (fetch-enlive uri)]
      (println uri)
      (doseq [n (extract-channel-names snippet)]
        (println n)))))
