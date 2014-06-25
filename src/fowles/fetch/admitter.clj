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

(defn- enq-video-ids-from-file
  ":: (str, chan) -> ()"
  [in-file to-ch]
  (util/line-by-line in-file
                     (fn [ln]
                       ;; Line has: video-id \t channel-id.
                       (let [[id _] (str/split ln #"\t")]
                         (>!! to-ch id)))))

;;-------------------

(defn video-ids-from-file
  ":: str-file-name -> chan"
  [in-file num-per-request]
  (let [single-id-ch (admitter/admit (partial enq-video-ids-from-file in-file))]
    (async/partition num-per-request
                     single-id-ch
                     BUFFER_SIZE)))

;;--------------------------

(defn video-ids-from-puller
  ""
  [port num-per-request]
  (let [single-id-ch (admitter/from-puller port)]
    (async/partition num-per-request
                     single-id-ch
                     BUFFER_SIZE)))
  
