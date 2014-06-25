(defproject fowles "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [com.cemerick/url "0.1.1"]
                 [clj-time "0.7.0"]

                 [org.zeromq/cljzmq "0.1.4"]
                 [http-kit "2.1.16"]]
                 ;; [org.clojure/tools.cli "0.3.1"]]

  :main fowles.fetch.core
  :aot [fowles.fetch.core]

  ;; https://github.com/zeromq/jzmq/issues/29
  ;; https://github.com/technomancy/leiningen/blob/master/sample.project.clj#L280
  ;; ZeroMQ
  :jvm-opts ["-Djava.library.path=/usr/local/lib"]

  :profiles {:dev {:dependencies [[midje "1.6.0"]]}})

