(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:require [fowles
             [plumbing :as plumbing]
             [failed :as failed]]
            [fowles.fetch
             [cfg :as cfg]
             [admitter :as admitter]
             [uris :as uris]
             [reporter :as reporter]]))


(defn- mk-ids-ch []
  (admitter/video-ids-from-puller
   (cfg/in-host)
   (cfg/in-port)
   (cfg/num-per-request)))

(defn- get-output-fn []
  (reporter/mk-videos-pusher (cfg/out-host) (cfg/out-port)))

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
                   (failed/mk-ch (cfg/failed-host) (cfg/failed-port))
                   (cfg/batch-size)
                   (cfg/frequency-ms)
                   (cfg/sleep-ms)
                   (get-output-fn))
  (while true))

;;---------------

(defn -main []
  (cfg/validate)
  (fetch))
