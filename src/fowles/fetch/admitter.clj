(ns fowles.fetch.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:require [fowles
             [util :as util]
             [admitter :as admitter]]
            [clojure.core.async :refer [chan]]))

(def BUFFER_SIZE 1000)
(def MAX_WAIT_MS 2000)

(defn video-ids-from-puller
  ":: (str, int, int) -> chan"
  [host port num-per-request]
  (let [single-id-ch (admitter/from-puller host port)
        out-ch (chan BUFFER_SIZE)]
    (.start (Thread. #(util/pipe-groups-of-up-to-n single-id-ch
                                                   out-ch
                                                   num-per-request
                                                   MAX_WAIT_MS)))
    out-ch))
