(ns fowles.yt-fetch.core
  "Fetch videos by id (or list of ids)."
  (:refer-clojure :exclude [merge])
  (:require [clojure.core.async :refer [merge map<] :as async]
            [fowles
             [admitter :as admitter]
             [util :as util]
             [plumbing :as plumbing]
             [failed :as failed]
             [cfg :as secret-cfg]]
            [fowles.yt-fetch
             [cfg :as cfg]
             [ids :as ids]
             [requests :as requests]
             [reporter :as reporter]]))

;; TODO: Get rid of redundancy here & admitter.
(def TOPICS [:videos :channels :activities :playlistItems])

(defn- mk-ids-chs
  ":: () -> {:topic ch}"
  [msg-ch]
  (ids/id-chs-from-puller msg-ch
                          (cfg/num-per-request :videos)
                          (cfg/num-per-request :channels)))

(defn- mk-typed-requests-ch [topic-2-ch topic]
  (requests/get-requests-ch topic
                            (get topic-2-ch topic)
                            (keyword (cfg/id-name topic))
                            (cfg/part topic)
                            (cfg/fields topic)))

;; MAPPING
;; (defn mapping
;;   "Returns a fn that takes a fn of [result input] and returns a fn
;;   that first calls f on the input"
;;   [f]
;;   (fn [f1]
;;     (fn
;;       ([result] (f1 result))
;;       ([result input]
;;          (f1 result (f input))))))

(defn- mk-requests-ch
  [msg-ch]
  (let [topic-2-ch (mk-ids-chs msg-ch)]
    ;; TODO: `map<` is deprecated.  Replace w/ `mapping` (see above).
    (map< (fn [req] {:request req, :resp-bodies []})
          (async/merge (map (partial mk-typed-requests-ch topic-2-ch)
                            TOPICS)))))

;;---------------------

(defn- get-output-fn []
  (reporter/mk-pusher (cfg/out-host) (cfg/out-port)))

(defn- mk-failed-ch []
  (failed/mk-ch (cfg/failed-host) (cfg/failed-port)))

(defn- fetch []
  ;; Start plumbing Thread.
  (let [msg-ch (admitter/from-puller (cfg/in-host) (cfg/in-port))]
    (util/prep-shutdown msg-ch)
    (plumbing/report (mk-requests-ch msg-ch)
                     (mk-failed-ch)
                     (secret-cfg/api-keys)
                     (cfg/batch-size)
                     (cfg/interval-ms)
                     (cfg/sleep-ms)
                     (get-output-fn)))
  (while true))

;;---------------

;; (defn- restart-self []
;;   (let [cmd (str "lein run")
;;
;;   /* Build command: java -jar application.jar */
;;   final ArrayList<String> command = new ArrayList<String>();
;;   command.add(javaBin);
;;   command.add("-jar");
;;   command.add(currentJar.getPath());
;;
;;   final ProcessBuilder builder = new ProcessBuilder(command);
;;   builder.start();
;;   System.exit(0);


(defn -main []
  (cfg/validate)
  (fetch))
