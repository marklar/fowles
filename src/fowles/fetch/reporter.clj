(ns fowles.fetch.reporter
  "Thread dedicated to outputing responses."
  (:require [fowles.util :as util]
            [clojure.data.json :as json]))

(defn- get-item-jsons
  ":: hmap -> [json-str]"
  [resp-body]
  (let [items (get resp-body "items")]
    (map json/write-str items)))

;;-------------------------------

(defn output-videos
  ":: (str, hmap) -> ()"
  [out-file resp-body]
  (let [item-json-strs (get-item-jsons resp-body)]
    (util/append-strs-to-file item-json-strs out-file)))
