(ns fowles.scrape
  (:use [clojure.java.io])
  (:require [clojure.data.json :as json]
            [zeromq.zmq :as zmq]
            [clojure.core.async :refer [chan timeout go-loop <!! <! >! >!!]]
            [org.httpkit.client :as http]))

(defn load-cfg
  "If cfg file exists, return cfg data.  Else, throw exception."
  [file-name]
  (if (.exists (as-file file-name))
    (let [json-str (slurp file-name)
          hmap     (json/read-str json-str)]
      (clojure.walk/keywordize-keys hmap))
    (throw (Exception. (str "Configuration file '"
                            file-name
                            "' NOT found.")))))

(def CFG_FILE_NAME "config/scrape.json")

(defn get-cfg []
  (load-cfg CFG_FILE_NAME))

;;---------------------------

(def zmq-context (zmq/context 1))

(defn mk-connect-addr
  [host port]
  (str "tcp://" host ":" port))

(defn mk-socket
  ":: (keyword, str, int) -> zmq-socket"
  [push-or-pull host port]
  (doto (zmq/socket zmq-context push-or-pull)
    (zmq/connect (mk-connect-addr host port))))

(def mk-pusher (partial mk-socket :push))
(def mk-puller (partial mk-socket :pull))

;;---------------------------

(def HEADERS
  {"User-Agent"      "Firefox/3.0.15"
   "accept"          "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
   "accept-language" "en-US,en;q=0.8"
   "cache-control"   "max-age=0"})
  
(defn mk-uri
  [video-id]
  (str "https://www.youtube.com/watch?v=" video-id))

(defn async-get
  [responses-ch video-id]
  (http/get (mk-uri video-id)
            {:headers HEADERS, :video-id video-id}
            #(>!! responses-ch %)))

;;---------------------------

(defn scrape-val
  ([html re-str]
     (re-find (re-pattern re-str) html))
  ([html re-str n]
     (get (scrape-val html re-str) n)))

(defn get-values
  [regexps html]
  (apply merge
   (for [[nm {:keys [regexp match_index]}] (sort regexps)]
     {nm (scrape-val html regexp match_index)})))

;;---------------------------

(defn- success?
  ":: int -> bool"
  [status]
  (and (>= status 200)
       (< status 300)))

(defn- handle-responses
  "Take responses off responses-ch, get vals, and report."
  [regexps-map responses-ch output-sock failed-sock]
  (go-loop []
    (let [{:keys [headers status body opts]} (<! responses-ch)
          video-id (:video-id opts)]
      (if-not (success? status)
        (do
          (println "failed:" video-id)
          (>! failed-sock video-id))
        (do
          (println "ok:" video-id)
          (let [vals-map (get-values regexps-map body)]
            (zmq/send-str output-sock
                          (json/write-str {:video_id video-id
                                           :values   vals-map}))))))
    (recur)))

(def BATCH_SIZE 5)
(def MS 250)

(defn- perform-queries
  "Get input, make requests, put on responses-ch."
  [cfg input-sock responses-ch]
  (let [batch-size  (-> cfg :concurrency :batch_size)
        interval-ms (-> cfg :concurrency :interval_ms)]
    (loop [i 0]
      (if (= i batch-size)
        (do
          (println "pausing:" interval-ms "ms")
          (<!! (timeout interval-ms))
          (recur 0))
        (let [video-id (zmq/receive-str input-sock)]
          (if-not video-id
            (do
              (println "done, but for now we recur anyway")
              (recur (inc i)))
            (do
              (println "receiving:" video-id)
              (async-get responses-ch video-id)
              (recur (inc i)))))))))

(defn- report-readiness
  [cfg]
  (println "Ready to receive input from addr:"
           (mk-connect-addr (-> cfg :servers :input :host)
                            (-> cfg :servers :input :port))))

(defn -main
  []
  (let [cfg         (get-cfg)
        regexps-map (:values cfg)
        input-sock   (mk-puller (-> cfg :servers :input :host)
                                (-> cfg :servers :input :port))
        output-sock  (mk-pusher (-> cfg :servers :output :host)
                                (-> cfg :servers :output :port))
        failed-sock  (mk-pusher (-> cfg :servers :failed :host)
                                (-> cfg :servers :failed :port))
        responses-ch (chan)]

    (report-readiness cfg)
    (handle-responses regexps-map responses-ch output-sock failed-sock)
    (perform-queries cfg input-sock responses-ch)))
