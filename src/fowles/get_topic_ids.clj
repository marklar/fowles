(ns fowles.get-topic-ids
  (:require [cemerick.url :refer [url-decode]]
            [clojure.string :as str]
            [clojure.data.json :as json]))

(def IN_FILE "io/fetch/video_json.txt")

(defn get-topic-ids
  [file-name]
  (let [lines     (str/split-lines (slurp file-name))
        hmaps     (map json/read-str lines)]
    ;; also get relevantTopicIds ?
    (remove nil?
            (flatten
             (map #(get-in % ["topicDetails" "topicIds"]) hmaps)))))

(defn -main []
  (let [topic-ids (get-topic-ids IN_FILE)]
    (doseq [i topic-ids]
      (println i))))
