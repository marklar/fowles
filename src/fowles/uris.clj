(ns fowles.uris
  (:require [clojure.string :as str]
            [cemerick.url :refer [url-encode]]
            [clojure.core.async
             :refer [chan pipe filter> map<]
             :as async]))

(def BASE_URI "https://www.googleapis.com/youtube/v3")

(defn- fix-value
  ":: [str] | str | str-able -> str"
  [v]
  (if (vector? v)
    (str/join "," (map url-encode v))
    (url-encode (str v))))

(defn- fix-arg
  [[k v]]
  (if (nil? v)
    nil
    (str k "=" (fix-value v))))
  
(defn- hmap->query-string
  ":: {keyword, str-able} -> str"
  [hmap]
  (->> hmap
       clojure.walk/stringify-keys
       (map fix-arg)
       (remove nil?)
       (str/join "&")))

;;---------------------

(defn mk-uri
  [query-type args]
  (str BASE_URI "/" query-type "?" (hmap->query-string args)))

