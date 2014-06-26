(ns fowles.fetch.admitter
  "Take in whatever input is required to perform data collection.
   + Source: from wherever (zmq socket?).
   + Data:   video-ids, queries.
   + Output: Put for requester."
  (:refer-clojure :exclude [partition])
  (:require [fowles
             [admitter :as admitter]
             [util :as util]]
            [clojure.string :as str]
            [clojure.core.async
             :refer [partition >!! go >!]
             :as async]))

(def BUFFER_SIZE 1000)

(defn video-ids-from-puller
  ":: (str, int, int) -> chan"
  [host port num-per-request]
  (let [single-id-ch (admitter/from-puller host port)]
    (async/partition num-per-request
                     single-id-ch
                     BUFFER_SIZE)))
  
