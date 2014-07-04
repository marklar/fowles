(ns fowles.simple
  "Admit URL.  Make request.  Send to sink."
  ;; (:require [net.cgrand.enlive-html :as enlive]
  (:require [zeromq.zmq :as zmq]
            [clj-http.client :as client]))

(def HOST "127.0.0.1")
(def IN_PORT  6000)
(def OUT_PORT 6001)

;;---------------------------

(defn- mk-connect-addr
  [host port]
  (str "tcp://" host ":" port))

(defn- mk-socket
  ":: (keyword, str, int) -> zmq-socket"
  [push-or-pull context host port]
  (doto (zmq/socket context push-or-pull)
    (zmq/connect (mk-connect-addr host port))))

;;---------------------------

(defn- report-success [pusher body]
  (zmq/send-str pusher (str "body: " body)))

(def USER_AGENT "Firefox/3.0.15")

(defn- go
  [puller pusher]
  (loop []
    (let [uri (zmq/receive-str puller)]
      (println "receiving:" uri)
      ;; also: status, headers
      (let [{:keys [body error]}
            (client/get uri {:headers {"User-Agent" USER_AGENT}})]
        (if error
          (zmq/send-str pusher (str "failed: " uri))
          (do
            (report-success pusher body)
            (recur)))))))

(defn -main []
  (let [context (zmq/context 1)]
    (with-open [puller (mk-socket :pull context HOST IN_PORT)
                pusher (mk-socket :push context HOST OUT_PORT)]
      (println "Ready to receive input...")
      (go puller pusher))))
