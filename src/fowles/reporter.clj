(ns fowles.reporter
  "Thread dedicated to outputing responses."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core.async :refer [go chan alts!! >!]]))

(defn- append-strs-to-file
  [strs file-name]
  ;; (doseq [s strs] (spit file-name s :append true)))
  (with-open [wrtr (io/writer file-name :append true)]
    (doseq [s strs] (.write wrtr (str s "\n")))))

;;-----------------------

(defn- mk-page-uri
  [uri page-token]
  ;; If there's already a pageToken, it'll be the last query-string arg.
  ;; So remove it.
  (let [[root-uri] (clojure.string/split uri #"\&pageToken=")]
    (str root-uri "&pageToken=" page-token)))

(defn- queue-next-uri
  [uris-ch prev-uri resp-body]
  (if-let [page-token (get resp-body "nextPageToken")]
    (let [new-uri (mk-page-uri prev-uri page-token)]
      (println "new-uri:" new-uri)
      (go (>! uris-ch new-uri)))))

;;-----------------------

(def VIDEO_DATA_FILE_NAME "video_data.txt")

(defn- output-videos
  [response]
  ;; TODO: Verify that the response was valid.
  ;; If so, continue.  If not, requeue that video-id.
  (spit VIDEO_DATA_FILE_NAME
        (:body response)
        :append true))

;;-----------------------

(def VIDEO_IDS_FILE_NAME "video_ids.txt")

(defn- get-video-ids
  [resp-body]
  (let [items (get resp-body "items")]
    (map #(-> % (get "id") (get "videoId")) items)))

(defn- output-video-ids
  [uris-ch response]
  (let [uri       (-> response :opts :url)
        resp-body (json/read-str (:body response))]
    (append-strs-to-file (get-video-ids resp-body) VIDEO_IDS_FILE_NAME)
    (queue-next-uri uris-ch uri resp-body)))

;;-----------------------

;;
;; This is where we want to do the re-queue-ing.
;; Need a reference here to the requester chan.
;;
;; Search results have a `nextPageToken` field.
;; Also, `publishedAfter` and `publishedBefore`.
;; If `nextPageToken` is present, push another URI onto queue.
;; 

(def CHANNEL_IDS_FILE_NAME "channel_ids.txt")

(defn- get-channel-ids
  [resp-body]
  (let [items (get resp-body "items")]
    (map #(-> % (get "snippet") (get "channelId")) items)))

(defn- output-channel-ids
  [uris-ch response]
  (let [uri       (-> response :opts :url)
        resp-body (json/read-str (:body response))]
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
  [from-ch uri-ch]
  (report from-ch (partial output-video-ids uri-ch)))

(defn report-channel-ids
  [from-ch uri-ch]
  (report from-ch (partial output-channel-ids uri-ch)))
