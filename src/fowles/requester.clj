(ns fowles.requester
  (:require [org.httpkit.client :as http]
            [clojure.core.async :refer [chan map> pipe] :as async]))

(defn mk-promises
  ":: chan -> chan"
  [from-ch]
  (let [to-ch (map> http/get (chan 1000))]
    (pipe from-ch to-ch)
    to-ch))
