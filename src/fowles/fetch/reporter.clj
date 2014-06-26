(ns fowles.fetch.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles 
             [util :as util]]
            [zeromq.zmq :as zmq]
            [clojure.data.json :as json]))

(defn- get-item-jsons
  ":: hmap -> [json-str]"
  [resp-body]
  (let [items (get resp-body "items")]
    (map json/write-str items)))

;;-------------------------------

(defn mk-videos-pusher
  ":: int -> (str -> ())"
  [host port]
  (let [pusher (util/mk-pusher host port)]
    (fn [resp-body]
        (doseq [js (get-item-jsons resp-body)]
          (zmq/send-str pusher js)))))
