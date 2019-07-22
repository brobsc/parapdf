(ns smart-pdf.dev
  (:require [smart-pdf.core]
            [nrepl.server :as server]
            [reply.main :as reply]))

(defn nrepl-handler []
  (require 'cider.nrepl)
  (ns-resolve 'cider.nrepl 'cider-nrepl-handler))

(defn -main []
  (println "Dev started...")
  (let [port (+ 6690 (rand-int 100))]
    (println (format "Starting nREPL server at port %d" port))
    (spit (doto (clojure.java.io/file "./.nrepl-port") .deleteOnExit) port)
    (server/start-server :port port :handler (nrepl-handler))
    (reply/launch-nrepl {:attach (str port)}))
  (shutdown-agents)
  (System/exit 0))
