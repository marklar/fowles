(ns fowles.requester
  (:refer-clojure :exclude [partition])
  (:require [fowles.uris :as uris]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go >! partition filter< map< alts!! to-chan]
             :as async]))

(def IDS_PER_QUERY 50)

;; Channels
;;   1. video-id   -> [video-id]
;;   2. [video-id] -> uri
;;   3. uri        -> response-promise           
(defn request-videos
  [api-key from-ch]
  (->> from-ch
       (async/partition IDS_PER_QUERY)
       (map< #(uris/mk-video-uri api-key %))
       (map< http/get)))

;;---------------------------

(defn- longer? [n s]
  (>= (count s) n))

;; Channels
;;   1. word -> word
;;   2. word -> uri
;;   3. uri  -> response-promise
(defn request-searches
  [api-key from-ch]
  (->> from-ch
       (filter< #(longer? 2 %))
       (map< #(uris/mk-search-uri api-key %))
       (map< http/get)))
