(ns fowles.reporter
  "Thread dedicated to outputing responses."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.core.async :refer [chan alts!!]]))

(defn- append-strs-to-file
  [strs file-name]
  ;; (doseq [s strs] (spit file-name s :append true)))
  (with-open [wrtr (io/writer file-name :append true)]
    (doseq [s strs] (.write wrtr (str s "\n")))))

(def VIDEO_IDS_FILE_NAME "video_ids.txt")

(defn- output-video-ids
  [{:keys [body]}]
  (let [resp-body (json/read-str body)
        items     (get resp-body "items")
        video-ids (map #(-> % (get "id") (get "videoId")) items)]
    (append-strs-to-file video-ids VIDEO_IDS_FILE_NAME)))

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
  [from-ch]
  (report from-ch output-video-ids))
