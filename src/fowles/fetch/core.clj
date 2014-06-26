(ns fowles.fetch.core
  "Fetch videos by id (or list of ids)."
  (:require [fowles
             [plumbing :as plumbing]
             [failed :as failed]
             [cfg :as secret-cfg]]
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

(defn- mk-requests-ch []
  (uris/video-requests (mk-ids-ch)
                       (cfg/part)
                       (cfg/fields)))

(defn- get-output-fn []
  (reporter/mk-videos-pusher (cfg/out-host) (cfg/out-port)))

(defn- mk-failed-ch []
  (failed/mk-ch (cfg/failed-host) (cfg/failed-port)))

(defn- fetch []
  ;; Start plumbing Thread.
  (plumbing/report (mk-requests-ch)
                   (mk-failed-ch)
                   (secret-cfg/api-keys)
                   (cfg/batch-size)
                   (cfg/frequency-ms)
                   (cfg/sleep-ms)
                   (get-output-fn))
  (while true))

;;---------------

(defn -main []
  (cfg/validate)
  (fetch))
