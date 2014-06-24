(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:require [clojure.core.async :refer [chan]]
            [fowles
             [cfg :as cfg]
             [util :as util]
             [requester :as requester]
             [gatherer :as gatherer]]
            [fowles.fetch
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

;;
;; TODO: Add new channel:
;;   + failed-uris - between gatherer and for-later
;; 

;;
;; TODO: explicitly define all channels here.
;; Then pass them into fns as necessary to use them.
;;

;; 
;; The framework provides two hooks.
;;
;; INPUT
;;   1. You call a function `enqueue-uris-from-file`.
;;      + takes a file name (string).  File contains one URI per line.
;;   -OR-
;;   2. You call function `enqueue-uris`
;;      + takes a sequence (collection) of URIs (string)
;;   -OR-
;;   3. You repeatedly call a function `enqueue-uri`.
;;      + takes either a URI (string) or nil
;;
;;   Asynchronous.  Return () immediately.
;;
;; OUTPUT
;;   * You provide a callback function, for output.
;;      + takes an hmap (or alternately, a JSON string)
;;      + uses it (outputs it) however you like
;;
;; --------------------
;;
;; PYTHON
;;   Provide a Python library that uses ZeroMQ.
;;   Input:  push URIs onto socket.
;;   Output: take response body JSON off socket.
;;   
;;
;; CMD-LINE FETCHER
;;   + cmd line: filename of videoIds
;;   + stdout:   json response bodies
;;   - Since we know how many videoIds there are,
;;     we can also know when we're done and exit.
;;


(defn- fetch
  [api-key]
  (let [;; you must specify these parts
        ids-ch        (admitter/admit-video-ids)
        uris-ch       (uris/video-uris api-key ids-ch)
        ;; let's hide these parts
        sleep-ch      (chan)
        responses-ch  (requester/get-responses uris-ch sleep-ch)
        bodies-ch     (gatherer/gather responses-ch uris-ch sleep-ch)]
    ;; then you specify this part
    (util/report bodies-ch reporter/output-videos))
  (while true))

(defn -main []
  (util/prep-shutdown)
  (fetch (cfg/cfg-get :api-key)))
