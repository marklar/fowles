(ns fowles.requester
  (:refer-clojure :exclude [partition])
  (:require [fowles.uris :as uris]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go >! partition map< alts!! to-chan]
             :as async]))

(def IDS_PER_QUERY 4)

;; Channels
;;   1. video-id   -> [video-id]
;;   2. [video-id] -> uri
;;   3. uri        -> response-promise           
(defn request
  [api-key from-ch]
  (->> from-ch
       (async/partition IDS_PER_QUERY)
       (map< #(uris/mk-video-uri api-key %))
       (map< http/get)))
