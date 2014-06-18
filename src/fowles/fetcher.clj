(ns fowles.fetcher
  (:refer-clojure :exclude [partition])
  (:require [fowles.cfg :as cfg]
            [fowles.uris :as uris]
             ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go partition map< split >! <! take! alts!!]
             :as async]))

(def BASE_URI "https://www.googleapis.com/youtube/v3/videos")
(def IDS_PER_QUERY 2)

;;-------------------------------------------------

;; Channels
;;   1. video-id   -> [video-id]
;;   2. [video-id] -> uri
;;   3. uri        -> response-promise           
(defn mk-pipeline-chan
  [api-key video-ids]
  (->> (async/to-chan video-ids)
       (async/partition IDS_PER_QUERY)
       (async/map< #(uris/mk-video-uri api-key %))
       ;; http://http-kit.org/client.html
       (async/map< http/get)))

;; Could also do this?
;;   4. response -> good-response | bad-response   (`split`)

(defn enq-fetches
  [api-key video-ids]
  (mk-pipeline-chan api-key video-ids))

(defn enq-responses
  [out-ch n-times]
  (let [in-ch (chan)]
    (go (dotimes [_ n-times]
          (async/>! in-ch (deref (async/<! out-ch)))))
    in-ch))

(defn deq-responses
  [out-ch n-times]
  (dotimes [_ n-times]
    (let [[v c] (async/alts!! [out-ch])]
      (println v))))

(defn query-count [seq]
  (/ (count seq) IDS_PER_QUERY))

(defn -main []
  (if-let [api-key (cfg/get-api-key)]
    (time
     (let [video-ids    ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"]
           n-times      (query-count video-ids)
           promises-ch  (enq-fetches api-key video-ids)
           responses-ch (enq-responses promises-ch n-times)]
       (deq-responses responses-ch n-times)
       (async/close! promises-ch)
       (async/close! responses-ch)
       ))))

