(ns fowles.scrape
  (:use [clojure.java.io])
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]))

(defn load-cfg
  "If cfg file exists, return cfg data.  Else, throw exception."
  [file-name]
  (if (.exists (as-file file-name))
    (let [json-str (slurp file-name)
          hmap     (json/read-str json-str)]
      (clojure.walk/keywordize-keys hmap))
    (throw (Exception. (str "Configuration file '"
                            file-name
                            "' NOT found.")))))

(def CFG_FILE_NAME "config/scrape.example.json")

(defn get-regexps-map
  []
  (load-cfg CFG_FILE_NAME))

;;---------------------------

(def HEADERS
  {"User-Agent"      "Firefox/3.0.15"
   "accept"          "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
   "accept-language" "en-US,en;q=0.8"
   "cache-control"   "max-age=0"})
  
(defn get-html
  [uri]
  (let [response (client/get uri {:headers HEADERS})
        {:keys [body error]} response]
    (if error
      nil
      body)))

(defn mk-uri
  [video-id]
  (str "https://www.youtube.com/watch?v=" video-id))

;;---------------------------

(defn scrape-val
  ([html re-str]
     (re-find (re-pattern re-str) html))
  ([html re-str n]
     (get (scrape-val html re-str) n)))

(defn get-values
  [regexps html]
  (apply merge
   (for [[nm {:keys [regexp index]}] (sort regexps)]
     {nm (scrape-val html regexp index)})))

;;---------------------------

(defn -main
  []
  (let [regexps (get-regexps-map)
        html (get-html (mk-uri "3WngGeI9lnA"))]
    (if html
      (json/pprint (get-values regexps html))
      (println "Failure to fetch page."))))
