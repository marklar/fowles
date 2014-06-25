(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:require [clojure.core.async :refer [chan alts!!]]
            [fowles
             [plumbing :as plumbing]]
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

(defn- mk-file-ids-ch []
  (admitter/video-ids-from-file
   (cfg/in-file)
   (cfg/num-per-request)))

(defn- mk-puller-ids-ch []
  (admitter/video-ids-from-puller
   (cfg/in-port)
   (cfg/num-per-request)))

(defn- mk-ids-ch []
  (if (nil? (cfg/in-port))
    (mk-file-ids-ch)
    (mk-puller-ids-ch)))

;;------------------

(defn- get-output-fn []
  (if (nil? (cfg/out-port))
    (reporter/mk-videos-writer (cfg/out-file))
    (reporter/mk-videos-pusher (cfg/out-port))))

;;------------------

(defn- mk-uris-ch []
  (let [ids-ch (mk-ids-ch)
        uris-ch (uris/video-uris ids-ch
                                 (cfg/api-key)
                                 (cfg/part)
                                 (cfg/fields))]
    uris-ch))

(defn- fetch []
  ;; Start plumbing Thread.
  (plumbing/report (mk-uris-ch)
                   (cfg/batch-size)
                   (cfg/frequency-ms)
                   (cfg/sleep-ms)
                   (cfg/failed-file)
                   (get-output-fn))
  (while true))

;;---------------

(defn -main []
  (cfg/validate)
  (fetch))
