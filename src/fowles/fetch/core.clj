(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:require [clojure.core.async :refer [chan]]
            [fowles.plumbing :as plumbing]
            [fowles.fetch
             [cfg :as cfg]
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))

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

(defn- mk-uris-ch []
  (let [ids-ch (admitter/admit-video-ids-from-file
                (cfg/in-file)
                (cfg/num-per-request))
        uris-ch (uris/video-uris ids-ch
                                 (cfg/api-key)
                                 (cfg/part)
                                 (cfg/fields))]
    uris-ch))

(defn- fetch []
  (plumbing/report (mk-uris-ch)
                   (cfg/batch-size)
                   (cfg/frequency-ms)
                   (cfg/sleep-ms)
                   (cfg/failed-file)
                   (partial reporter/output-videos
                            (cfg/out-file))))

;;---------------

(defn -main []
  (cfg/validate)
  (fetch)
  (while true))
