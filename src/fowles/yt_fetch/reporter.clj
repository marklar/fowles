(ns fowles.yt-fetch.reporter
  "Thread dedicated to outputing responses."
  (:use [clojure.core.match :only [match]])
  (:require [fowles 
             [util :as util]]
            [zeromq.zmq :as zmq]
            [clojure.data.json :as json]))

(defn- push-playlist-items
  [pusher request resp-body]
  (let [id-name (:id-name request)]
    (doseq [item (get resp-body "items")]
      (zmq/send-str pusher
                    (json/write-str
                     {:request  (:query-type request)
                      id-name   (get-in item ["snippet" (name id-name)])
                      :response item})))))

(defn- push-vids-or-chans
  [pusher request resp-body]
  (let [id-name (:id-name request)]
    (doseq [item (get resp-body "items")]
      (zmq/send-str pusher
                    (json/write-str
                     {:request  (:query-type request)
                      id-name   (get item (name id-name))
                      :response item})))))

(defn- push-activities-fancy
  [pusher request resp-body]
  (let [snippets (map #(get % "snippet") (get resp-body "items"))
        js (json/write-str {:request   (:query-type request)
                            :channelId (get-in request [:args :channelId])
                            :snippets  snippets})]
    (zmq/send-str pusher js)))

;;-------------------------------

(defn- push-activities-simple
  [pusher request resp-body]
  (zmq/send-str pusher (json/write-str resp-body)))

;;-------------------------------

(defn mk-pusher
  [host port]
  (let [pusher (util/mk-pusher host port)]
    ;; 'resp-body' is a clj data structure (not json).
    (fn [{:keys [request resp-body]}]
      (match (:query-type request)
             :activities             (push-activities-fancy pusher request resp-body)
             (:or :channels :videos) (push-vids-or-chans pusher request resp-body)
             :playlistItems          (push-playlist-items pusher request resp-body)
             ;; throw!
             :else nil))))
