(ns fowles.yt-fetch.reporter
  "Provides function to write results to results-socket."
  (:use [clojure.core.match :only [match]])
  (:require [fowles 
             [util :as util]]
            [zeromq.zmq :as zmq]
            [clojure.data.json :as json]))

(defn- push-items
  [pusher request resp-body]
  (let [id-name (:id-name request)]
    (doseq [item (get resp-body "items")]
      (zmq/send-str pusher
                    (json/write-str
                     {:request  (:query-type request)
                      id-name   (get item (name id-name))
                      :response item})))))

(defn- push-playlist
  [pusher request resp-body]
  (let [id-name (:id-name request)]
    (zmq/send-str pusher
                  (json/write-str
                   {:reqeust  (:query-type request)
                    id-name   (get-in request [:args id-name])
                    :response (get resp-body "items")}))))

;;-------------------------------

(defn- push-activities-simple
  [pusher request resp-body]
  (zmq/send-str pusher (json/write-str resp-body)))

(defn- push-activities-fancy
  [pusher request resp-body]
  (let [snippets (map #(get % "snippet") (get resp-body "items"))]
    (zmq/send-str pusher
                  (json/write-str {:request   (:query-type request)
                                   :channelId (get-in request [:args :channelId])
                                   :response  snippets}))))

;;-------------------------------

(defn mk-pusher
  [host port]
  (let [pusher (util/mk-pusher host port)]
    ;; 'resp-body' is a clj data structure (not json).
    (fn [{:keys [request resp-body]}]
      (match (:query-type request)
             (:or :channels :videos) (push-items pusher request resp-body)
             :activities             (push-activities-fancy pusher request resp-body)
             :playlistItems          (push-playlist pusher request resp-body)
             ;; throw!
             :else nil))))
