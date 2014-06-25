(ns fowles.fetch.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles 
             [util :as util]
             [reporter :as reporter]]
            [clojure.data.json :as json]))

(defn- get-item-jsons
  ":: hmap -> [json-str]"
  [resp-body]
  (let [items (get resp-body "items")]
    (map json/write-str items)))

;;-------------------------------

;; TODO:
;; Enclose over an io/writer,
;; so you don't need to keep re-creating it.
(defn mk-videos-writer
  ":: str -> (str -> ())"
  [out-file]
  (fn [resp-body]
    (let [item-json-strs (get-item-jsons resp-body)]
      (util/append-strs-to-file item-json-strs out-file))))
    
(defn mk-videos-pusher
  ":: int -> (str -> ())"
  [port]
  (let [pusher (reporter/mk-pusher port)]
    (fn [resp-body]
      (let [item-json-strs (get-item-jsons resp-body)]
        (reporter/send-seq pusher item-json-strs)))))
