(ns fowles.fetcher
  (:refer-clojure :exclude [partition])
  (:use [clojure.java.io])
  (:require ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go partition map< split >! <! take! alts!!]
             :as async]))

(def BASE_URI "https://www.googleapis.com/youtube/v3/videos")
(def IDS_PER_QUERY 2)
;; (def PARTS
;;   ["id" "snippet" "contentDetails" "fileDetails" "liveStreamingDetails"
;;    "player" "processingDetails" "recordingDetails" "statistics" "status"
;;    "suggestions" "topicDetails"])
(def PARTS ["snippet" "contentDetails" "statistics" "status"])

(defn load-cfg
  "If cfg file exists, return cfg data.
   If it does not, return default-cfg."
  [file-name]
  (if (.exists (as-file file-name))
    (let [json-str (slurp file-name)]
      (json/read-str json-str))
    (do
      (println "No configuration file found.  Using default config settings.")
      nil)))

(defn get-api-key []
  (if-let [cfg (load-cfg ".config.json")]
    (get cfg "api-key")))


;;-------------------------------------------------

(defn csv [strs]
  (str/join "," strs))

(defn hmap->query-string [hmap]
  (->> hmap
       clojure.walk/stringify-keys
       (map (fn [[k v]] (str k "=" v)))
       (str/join "&")))

;; https://developers.google.com/youtube/v3/docs/videos/list?hl=ca
(defn mk-video-uri [api-key video-ids parts]
  (let [args {:key api-key
              :id (csv video-ids)
              :part (csv parts)}]
    (str BASE_URI "?" (hmap->query-string args))))

;; Channels
;;   1. video-id   -> [video-id]
;;   2. [video-id] -> uri
;;   3. uri        -> response-promise           
(defn mk-pipeline-chan
  [api-key video-ids parts]
  (->> (async/to-chan video-ids)
       (async/partition IDS_PER_QUERY)
       (async/map< #(mk-video-uri api-key % parts))
       ;; http://http-kit.org/client.html
       (async/map< http/get)))

;; Could also do this?
;;   4. response -> good-response | bad-response   (`split`)

(defn query-count [seq]
  (/ (count seq) IDS_PER_QUERY))

(defn enq-fetches
  [api-key video-ids]
  (mk-pipeline-chan api-key video-ids PARTS))

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

(defn -main []
  (if-let [api-key (get-api-key)]
    (time
     (let [video-ids    ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"]
           n-times      (query-count video-ids)
           promises-ch  (enq-fetches api-key video-ids)
           responses-ch (enq-responses promises-ch n-times)]
       (deq-responses responses-ch n-times)
       (async/close! promises-ch)
       (async/close! responses-ch)
       ))))

