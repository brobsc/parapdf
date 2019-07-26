(ns smart-pdf.dev
  (:require [cider.nrepl :refer [cider-middleware]]
            [nrepl.server :as server]
            [reply.main :as reply]
            [refactor-nrepl middleware]
            [smart-pdf.core]))

(def wrapped-handler
  (apply server/default-handler
         (cons #'refactor-nrepl.middleware/wrap-refactor
               (map resolve cider-middleware))))

(defn -main []
  (println "Dev started...")
  (let [port (+ 6690 (rand-int 100))]
    (spit (doto (clojure.java.io/file "./.nrepl-port") .deleteOnExit) port)
    (with-open [s (server/start-server :port port :handler wrapped-handler)]
      (println (format "Started nREPL server at port %d" port))
      (reply/launch-nrepl {:attach (str port)})))
  (shutdown-agents)
  (System/exit 0))

;; /usr/local/bin/clojure -Sdeps '{:deps {nrepl {:mvn/version "0.6.0"} refactor-nrepl {:mvn/version "2.4.0"} cider/cider-nrepl {:mvn/version "0.21.2-SNAPSHOT"}}}' -R:dev -m nrepl.cmdline --middleware '["refactor-nrepl.middleware/wrap-refactor", "cider.nrepl/cider-middleware"]'

