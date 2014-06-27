(ns fowles.fetch.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [fowles
             [util :as util]
             [admitter :as admitter]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [chan split map<]]))

(def BUFFER_SIZE 1000)
(def MAX_WAIT_MS 2000)

(defn mk-grouped-ch
  [size in-ch]
  (let [out-ch (chan BUFFER_SIZE)]
    (.start
     (Thread.
      #(util/pipe-groups-of-up-to-n in-ch out-ch size MAX_WAIT_MS)))
    out-ch))

(defn- is-video-id?
  "We expect `msg` to have one of the two following keys:
     + 'video_id'
     + 'channel_id'
  "
  [msg]
  (not (nil? (get msg "video_id"))))

(defn id-chs-from-puller
  "returns 2 chans: vid-ids-ch & chan-ids-ch"
  [host port num-vid-ids num-chan-ids]
  (let [msg-ch      (map< json/read-str (admitter/from-puller host port))
        [v-ch c-ch] (split is-video-id? msg-ch BUFFER_SIZE BUFFER_SIZE)]
    [(mk-grouped-ch num-vid-ids  (map< #(get % "video_id")   v-ch))
     (mk-grouped-ch num-chan-ids (map< #(get % "channel_id") c-ch))]))
