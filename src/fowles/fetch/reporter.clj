(ns fowles.fetch.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.reporter :as reporter]
            [clojure.data.json :as json]))

(def VIDEO_DATA_FILE_NAME "io/video_data.txt")

(defn- get-item-jsons
  ":: hmap -> [json-str]"
  [resp-body]
  (let [items (get resp-body "items")]
    (map json/write-str items)))

(defn- output-videos
  ":: hmap -> ()"
  [resp-body]
  (let [item-json-strs (get-item-jsons resp-body)]
    (reporter/append-strs-to-file item-json-strs VIDEO_DATA_FILE_NAME)))

;;------------------------

(defn report
  ":: chan -> ()
   Given channel of responses, 'output' them in own Thread."
  [responses-ch uri-ch sleep-ch]
  (reporter/report responses-ch uri-ch sleep-ch
                   output-videos))
