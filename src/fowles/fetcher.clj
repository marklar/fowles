(ns fowles.fetcher
  (:refer-clojure :exclude [partition])
  (:require ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [clojure.core.async
             :refer [chan go partition map< split >! <! take! alts!!]
             :as async]))

;;-------------------------------------------

;; HTTP client

(def api-key "AIzaSyBV8NYXYUl42UtckkRNov98i1mRrNxLJ_4")

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
    (str "https://www.googleapis.com/youtube/v3/videos?"
         (hmap->query-string args))))


(def IDS_PER_QUERY 2)

;; Channels
;;   1. video-id   -> [video-id]                 (`partition`)
;;   2. [video-id] -> uri                        (`map<`)
;;   3. uri        -> response-promise           
;;   4. response-promise -> good-response | bad-response   (`split`)

(defn mk-pipeline-chan
  [api-key video-ids parts]
  (->> (async/to-chan video-ids)
       (async/partition IDS_PER_QUERY)
       (async/map< #(mk-video-uri api-key % parts))
       ;; http://http-kit.org/client.html
       (async/map< #(deref (http/get %)))))

(defn query-count [seq]
  (/ (count seq) IDS_PER_QUERY))

(defn -main []
  (let [video-ids ["7lCDEYXw3mM" "MjtOzLfebgY" "6QIw1BQIvT4" "2xJWQPdG7jE"]
        parts     ["snippet" "contentDetails" "statistics" "status"]
        ch        (mk-pipeline-chan api-key video-ids parts)]

    ;; (go (dotimes [_ (query-count video-ids)]
    ;;      (println (async/<! ch))))
    ;;      ;; (async/take! ch println))
    ;; (Thread/sleep 10000)

    (dotimes [_ (query-count video-ids)]
      (let [[v c] (async/alts!! [ch])]
        (println v)))

    (async/close! ch)
))
