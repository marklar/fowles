(ns fowles.yt-fetch.ids
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [fowles
             [util :as util]
             [admitter :as admitter]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [chan map< pub sub]]))

(def MAX_WAIT_MS 2000)

(defn- mk-grouped-ch
  [size in-ch]
  (let [out-ch (chan)]
    (.start
     (Thread.
      #(util/pipe-groups-of-up-to-n in-ch out-ch size MAX_WAIT_MS)))
    out-ch))

;;
;; We expect `msg` to be of one of the following types:
;;   {'request': 'videos',        'id':         x}
;;   {'request': 'channels',      'id':         x}
;;   {'request': 'activities',    'channelId':  x}
;;   {'request': 'playlistItems', 'playlistId': x}
;;
;; The first two request types support multiple IDs per query.
;; The latter two allow only one ID per query.
;; 

;; TODO: Get this is from config file.
(def TOPIC_2_ID_NAME
  {:videos "id"
   :channels "id"
   :activities "channelId"
   :playlistItems "playlistId"})

(defn- mk-sub-ch-map
  [publication topic id-name]
  (let [ch (chan)]
    (sub publication topic ch)
    {topic
     (map< #(get % id-name) ch)}))

(defn- mk-type-2-ch
  [publication]
  (apply merge (map (fn [[t n]] (mk-sub-ch-map publication t n))
                    TOPIC_2_ID_NAME)))

(defn- re-wrap
  "turns it into a msg again, more or less"
  [[topic ids-ch]]
  (let [id-name (get TOPIC_2_ID_NAME topic)]
    {topic (map< #(hash-map :topic topic, (keyword id-name) %)
                 ids-ch)}))

(defn- groups
  [type-2-ch num-vid-ids num-chan-ids]
  (assoc type-2-ch
    :videos   (mk-grouped-ch num-vid-ids  (:videos type-2-ch))
    :channels (mk-grouped-ch num-chan-ids (:channels type-2-ch))))

;;-------------------
  
(defn id-chs-from-puller
  "returns hmap: topic-name -> id-ch"
  [msg-ch num-vid-ids num-chan-ids]
  (let [json-msg-ch  (map< json/read-str msg-ch)
        publication  (pub json-msg-ch #(keyword (get % "request")))
        type-2-ch    (mk-type-2-ch publication)]
    (apply merge
           (map re-wrap
                (groups type-2-ch num-vid-ids num-chan-ids)))))
