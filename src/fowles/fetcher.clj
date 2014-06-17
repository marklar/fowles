(ns fowles.fetcher
  (:require [com.keminglabs.zmq-async.core :refer [register-socket!]]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [clojure.core.async :refer [alt! alts! timeout >! <! go chan
                                        sliding-buffer close!]]))

;; HTTP client

;; Mark's personal API key
;; AIzaSyBV8NYXYUl42UtckkRNov98i1mRrNxLJ_4

(def api-key "AIzaSyBV8NYXYUl42UtckkRNov98i1mRrNxLJ_4")

(defn hmap->query-string [hmap]
  (->> hmap
       clojure.walk/stringify-keys
       (map (fn [[k v]] (str k "=" v)))
       (str/join "&")))

(defn csv [strs]
  (str/join "," strs))

;; https://developers.google.com/youtube/v3/docs/videos/list?hl=ca
(defn mk-video-url [api-key video-ids parts]
  (let [args {:key api-key
              :id (csv video-ids)
              :part (csv parts)}]
    (str "https://www.googleapis.com/youtube/v3/videos?"
         (hmap->query-string args))))

(def video-ids ["7lCDEYXw3mM"])
(defn foo []
  ;; start concurrent requests, get promise, half the waiting time
  (let [parts ["snippet" "contentDetails" "statistics" "status"]
        response1 (http/get (mk-video-url api-key video-ids parts))
        response2 (http/get "http://clojure.org/")]
    ;; Handle responses one-by-one, blocking as necessary
    ;; Other keys :headers :body :error :opts
    ;; (println "response1's status:" (:status @response1))
    (println "response1:" @response1)))
    ;; (println "response2's status:" (:status @response2))))

(defn -main []
  (foo))
