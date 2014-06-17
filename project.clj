(defproject fowles "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [http-kit "2.1.16"]]
                 ;; [org.clojure/data.json "0.2.4"]
                 ;; [org.clojure/core.match "0.2.1"]
                 ;; [org.clojure/tools.cli "0.3.1"]]
                 ;; [org.zeromq/cljzmq "0.1.5-SNAPSHOT"]]
                 ;; [com.keminglabs/zmq-async "0.1.0"]]

  :main fowles.fetcher
  :aot [fowles.fetcher]

  :profiles {:dev {:dependencies [[midje "1.6.0"]]}})

