(ns fowles.yt-fetch.core
  "Fetch videos by id (or list of ids)."
  (:refer-clojure :exclude [merge])
  (:require [clojure.core.async :refer [merge] :as async]
            [fowles
             [plumbing :as plumbing]
             [failed :as failed]
             [cfg :as secret-cfg]]
            [fowles.yt-fetch
             [cfg :as cfg]
             [admitter :as admitter]
             [requests :as requests]
             [reporter :as reporter]]))

;; TODO: Get rid of redundancy here & admitter.
(def TOPICS [:videos :channels :activities :playlistItems])

(defn- mk-ids-chs
  ":: () -> {:topic ch}"
  []
  (admitter/id-chs-from-puller
   (cfg/in-host)
   (cfg/in-port)
   (cfg/num-per-request :videos)
   (cfg/num-per-request :channels)))

(defn- mk-typed-requests-ch [topic-2-ch topic]
  (requests/get-requests-ch topic
                            (get topic-2-ch topic)
                            (keyword (cfg/id-name topic))
                            (cfg/part topic)
                            (cfg/fields topic)))

(defn- mk-requests-ch []
  (let [topic-2-ch (mk-ids-chs)]
    (async/merge (map (partial mk-typed-requests-ch topic-2-ch)
                      TOPICS))))

(defn- get-output-fn []
  (reporter/mk-pusher (cfg/out-host) (cfg/out-port)))

(defn- mk-failed-ch []
  (failed/mk-ch (cfg/failed-host) (cfg/failed-port)))

(defn- fetch []
  ;; Start plumbing Thread.
  (plumbing/report (mk-requests-ch)
                   (mk-failed-ch)
                   (secret-cfg/api-keys)
                   (cfg/batch-size)
                   (cfg/interval-ms)
                   (cfg/sleep-ms)
                   (get-output-fn))
  (while true))

;;---------------

(defn -main []
  (cfg/validate)
  (fetch))
