(ns fowles.reporter
  "Thread dedicated to outputing responses."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core.async :refer [go chan alts!! >!]]))

;;
;; TODO: Rather than creating a new wrtr each time,
;; create one the first time and then reuse.
;;
(defn- append-strs-to-file
  [strs file-name]
  ;; Is one of these more efficient than the other?
  ;; (spit file-name
  ;;       (str (clojure.string/join "\n" strs) "\n")
  ;;       :append true))))
  (with-open [wrtr (io/writer file-name :append true)]
    (doseq [s strs] (.write wrtr (str s "\n")))))

;;-----------------------

(defn- mk-page-uri
  ":: (str, str) -> str"
  [uri page-token]
  ;; If there's already a pageToken:
  ;;   + It'll be the last query-string arg.
  ;;   + Remove it.
  ;; Then append new pageToken.
  (let [[root-uri] (clojure.string/split uri #"\&pageToken=")]
    (str root-uri "&pageToken=" page-token)))

(defn- queue-next-uri
  [uris-ch prev-uri resp-body]
  (if-let [page-token (get resp-body "nextPageToken")]
    (let [new-uri (mk-page-uri prev-uri page-token)]
      ;; (println "new-uri:" new-uri)
      (go (>! uris-ch new-uri)))))

;;-----------------------

(def VIDEO_DATA_FILE_NAME "io/video_data.txt")

(defn- get-item-jsons
  ":: json-str -> [json-str]"
  [body]
  (let [bj (json/read-str body)
        is (get bj "items")]
    (map json/write-str is)))

(defn- output-videos
  [response]
  ;; TODO: Verify that the response was valid.
  ;; If so, continue.  If not, requeue that video-id.
  (if-let [b (:body response)]
    (let [item-jsons (get-item-jsons b)]
      (append-strs-to-file item-jsons VIDEO_DATA_FILE_NAME))))

;;-----------------------

;; (def VIDEO_IDS_FILE_NAME "io/video_ids.txt")
(def IDS_FILE_NAME "io/video_and_channel_ids.txt")

(defn- get-video-and-channel-ids
  [resp-body]
  (let [items   (get resp-body "items")
        get-ids (fn [item]
                  (let [v-id (-> item (get "id")      (get "videoId"))
                        c-id (-> item (get "snippet") (get "channelId"))]
                    (str v-id "\t" c-id)))]
    (map get-ids items)))

(defn- output-video-and-channel-ids
  [uris-ch response]
  (let [uri       (-> response :opts :url)
        resp-body (json/read-str (:body response))]
    (append-strs-to-file (get-video-and-channel-ids resp-body) IDS_FILE_NAME)
    (queue-next-uri uris-ch uri resp-body)))

;;-----------------------

(def CHANNEL_IDS_FILE_NAME "io/channel_ids.txt")

(defn- get-channel-ids
  [resp-body]
  (let [items (get resp-body "items")]
    (map #(-> % (get "snippet") (get "channelId")) items)))

(defn- output-channel-ids
  [uris-ch response]
  (let [uri       (-> response :opts :url)
        resp-body (json/read-str (:body response))]
    ;; (println "uri:" uri)
    ;; (println "body:" resp-body)
    (append-strs-to-file (get-channel-ids resp-body) CHANNEL_IDS_FILE_NAME)
    (queue-next-uri uris-ch uri resp-body)))

;;-----------------------

(defn- dequeue
  [from-ch output-fn]
  (loop []
    (let [[v c] (alts!! [from-ch])]
      (if (nil? v)
        nil
        (do
          (output-fn v)
          (recur))))))

(defn- report
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [from-ch output-fn]
  (.start (Thread. #(dequeue from-ch output-fn))))

;;-----------------------

(defn report-results
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [from-ch]
  (report from-ch output-videos))

(defn report-search-result-ids
  ":: (chan, chan) -> ()"
  [from-ch uri-ch]
  (report from-ch (partial output-video-and-channel-ids uri-ch)))

(defn report-channel-ids
  ":: (chan, chan) -> ()"
  [from-ch uri-ch]
  (report from-ch (partial output-channel-ids uri-ch)))
