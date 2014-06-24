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

(def BUFFER_SIZE 1000)

;; FIXME: Don't enqueue all of them at once.  `split` is NOT lazy!
(defn- enq-video-ids-from-file
  ":: chan -> ()"
  [in-file to-ch]
  (let [lines     (str/split-lines (slurp in-file))
        video-ids (map #(first (str/split % #"\t")) lines)]
    (admitter/enq to-ch video-ids)))

;;--------------------------

(defn admit-video-ids-from-file
  ":: str-file-name -> chan"
  [in-file num-per-request]
  (async/partition num-per-request
                   (admitter/admit (partial enq-video-ids-from-file in-file))
                   BUFFER_SIZE))
