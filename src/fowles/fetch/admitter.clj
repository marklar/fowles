(ns fowles.fetch.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:refer-clojure :exclude [partition])
  (:require [fowles.admitter :as admitter]
            [clojure.string :as str]
            [clojure.core.async :refer [partition] :as async]))

(def VIDEO_IDS_FILE_NAME "io/video_and_channel_ids.txt")

(def BUFFER_SIZE 1000)

(defn- enq-video-ids
  ":: chan -> ()"
  [to-ch]
  (let [lines     (str/split-lines (slurp VIDEO_IDS_FILE_NAME))
        video-ids (map #(first (str/split % #"\t")) lines)]
    (admitter/enq to-ch (take BUFFER_SIZE video-ids))))

;;
;; TODO
;; Use a timeout here to push whatever there is onto the channel
;; if there are fewer than IDS_PER_QUERY there.
;;

;;--------------------------

(def IDS_PER_QUERY 5)
(defn admit-video-ids
  ":: () -> chan"
  []
  (async/partition IDS_PER_QUERY
                   (admitter/admit enq-video-ids)))

