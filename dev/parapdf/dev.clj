(ns parapdf.dev
  (:require [cider.nrepl :refer [cider-middleware]]
            [clojure.java.io :as io]
            [nrepl.server :as server]
            refactor-nrepl.middleware
            [reply.main :as reply]
            parapdf.core))

(def wrapped-handler
  (apply server/default-handler
         (cons #'refactor-nrepl.middleware/wrap-refactor
               (map resolve cider-middleware))))

(defn -main []
  (println "Dev started...")
  (with-open [s (server/start-server
                  :handler wrapped-handler)]
    (let [port (:port s)]
      (spit (doto (io/file "./.nrepl-port") .deleteOnExit) port)
      (println (format "Started nREPL server at port %d" port))
      (reply/launch-nrepl {:attach (str port)})))
  (shutdown-agents)
  (System/exit 0))
