(ns fowles.yt-fetch.reporter
  "Provides function to write results to results-socket."
  (:use [clojure.core.match :only [match]])
  (:require [fowles 
             [util :as util]]
            [zeromq.zmq :as zmq]
            [clojure.data.json :as json]))

(defn- push-items
  [pusher request resp-bodies]
  (let [id-name (:id-name request)]
    ;; TODO: Possible to combine these two `doseq`s?
    (doseq [resp-body resp-bodies]
      (doseq [item (get resp-body "items")]
        (zmq/send-str pusher
                      (json/write-str
                       {:request  (:query-type request)
                        id-name   (get item (name id-name))
                        :response item}))))))

(defn- push-playlist
  [pusher request resp-bodies]
  (let [id-name   (:id-name request)
        all-items (flatten (map #(get % "items") resp-bodies))]
    (zmq/send-str pusher
                  (json/write-str
                   {:request  (:query-type request)
                    id-name   (get-in request [:args id-name])
                    :response all-items}))))

;;-------------------------------

(defn- push-activities-simple
  [pusher request resp-bodies]
  (let [all-items (flatten (map #(get % "items") resp-bodies))]
    (zmq/send-str pusher (json/write-str all-items))))

(defn- push-activities-fancy
  [pusher request resp-bodies]
  (let [all-snippets (map #(get % "snippet")
                          (flatten (map #(get % "items") resp-bodies)))]
    (zmq/send-str pusher
                  (json/write-str
                   {:request   (:query-type request)
                    :channelId (get-in request [:args :channelId])
                    :response  all-snippets}))))

;;-------------------------------

(defn mk-pusher
  [host port]
  (let [pusher (util/mk-pusher host port)]
    ;; acc is seq of 'resp-body', each a clj data structure (not a json str).
    (fn [{:keys [request resp-bodies]}]
      (match (:query-type request)
             ;; We don't use `:or` here because it seems not to work sometimes!?
             :channels      (push-items pusher request resp-bodies)
             :videos        (push-items pusher request resp-bodies)
             :activities    (push-activities-fancy pusher request resp-bodies)
             :playlistItems (push-playlist pusher request resp-bodies)
             ;; throw!
             :else nil))))
