(ns fowles.fetcher
  (:refer-clojure :exclude [partition])
  (:require [fowles.cfg :as cfg]
            [fowles.uris :as uris]
             ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go partition map< split >! <! take! alts! alts!!]
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
       (async/map< http/get)))

;; Could also do this?
;;   4. response -> good-response | bad-response   (`split`)

(defn enq-fetches
  [api-key video-ids]
  (mk-pipeline-chan api-key video-ids))

;; Use 'send' w/ agent?
(defn enq-responses
  [from-ch to-ch]
  ;; Thread dedicated to gathering responses.
  (.start (Thread.
           #(loop []
              ;; alts!! blocks until completed.
              (let [[v c] (async/alts!! [from-ch])]
                (if (nil? v)
                  ;; (async/close! to-ch)
                  nil
                  (do
                    (async/go (async/>! to-ch (deref v)))
                    (recur))))))))

(defn deq-responses
  [from-ch]
  (.start (Thread. #(loop []
                      (let [[v c] (async/alts!! [from-ch])]
                        (if (nil? v)
                          nil
                          (do
                            (println v)
                            (recur))))))))

(defn query-count [seq]
  (/ (count seq) IDS_PER_QUERY))

(defn -main []
  (if-let [api-key (cfg/get-api-key)]
    (time
     (let [video-ids    ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"]
           promise-ch   (enq-fetches api-key video-ids)
           response-ch  (chan)]
       (enq-responses promise-ch response-ch)
       (deq-responses response-ch)
       (while true)
       ))))

