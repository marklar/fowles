(ns fowles.fetch.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.reporter :as reporter]
            [clojure.data.json :as json]))

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
      (reporter/append-strs-to-file item-jsons VIDEO_DATA_FILE_NAME))))

;;------------------------

(defn report
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [from-ch]
  (reporter/report from-ch output-videos))
