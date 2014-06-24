(ns fowles.fetch.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  ;; [com.keminglabs.zmq-async.core :refer [register-socket!]]
  (:refer-clojure :exclude [partition])
  (:require [fowles
             [admitter :as admitter]
             [util :as util]]
            [clojure.string :as str]
            [clojure.core.async :refer [partition >!! close!] :as async]))

(def BUFFER_SIZE 1000)

(defn- enq-video-ids-from-file
  ":: (str, chan) -> ()"
  [in-file to-ch]
  (util/line-by-line in-file
                     (fn [ln]
                       ;; The line has both a video-id and a channel-id.
                       (let [[id _] (str/split ln #"\t")]
                         (>!! to-ch id))))
  (close! to-ch))

;;--------------------------

(defn admit-video-ids-from-file
  ":: str-file-name -> chan"
  [in-file num-per-request]
  (async/partition num-per-request
                   (admitter/admit (partial enq-video-ids-from-file in-file))
                   BUFFER_SIZE))
