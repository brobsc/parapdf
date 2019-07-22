(ns smart-pdf.core
  (:require
    [cljfx.api :as fx]
    [smart-pdf.views :as v]
    [smart-pdf.events :as e])
  (:import [javafx.scene.input TransferMode
            ClipboardContent MouseEvent]))

(def ctx (atom
             (fx/create-context
               {:files []
                :showing true
                :file-dialog true
                :drag-src ""
                :drag-target ""
                :current-file "pdfjs/web/relatorio.pdf"
                :prefs {}})))

(defn- process-event [_ f e dispatch-async! *maybe-promise]
  (f e dispatch-async!)
  (when *maybe-promise
    (deliver *maybe-promise nil)))

(defn wrap-async [f]
  (fn dispatch! [e]
    (if-not (:fx/sync e)
      (let [*agent (agent nil)
            *promise (promise)]
        (send *agent process-event f e dispatch! *promise)
        @*promise)
      (f e dispatch!))))

#_(defn http-effect [v dispatch!]
  (try
    (http/request
      (-> v
          (assoc :async? true :as :byte-array)
          (dissoc :on-response :on-exception))
      (fn [response]
        (dispatch! (assoc (:on-response v) :response response)))
      (fn [exception]
        (dispatch! (assoc (:on-exception v) :exception exception))))
    (catch Exception e
      (dispatch! (assoc (:on-exception v) :exception e)))))

(defn show-file-dialog [v dispatch!]
  (fx/on-fx-thread
    (fx/instance
      (fx/create-component
        {:fx/type fx/ext-instance-factory
         :create #(dispatch! (assoc (:on-choose v) :files
                                    (-> (javafx.stage.FileChooser.)
                                        (.showOpenMultipleDialog nil))))}))))

(def event-handler
  (-> e/event-handler
      (fx/wrap-co-effects
        {:fx/context (fx/make-deref-co-effect ctx)})
      (fx/wrap-effects
        {:context (fx/make-reset-effect ctx)
         :dispatch fx/dispatch-effect
         :file-dialog show-file-dialog})
      #_(wrap-async)))

(def renderer
  (fx/create-renderer
    :middleware (comp fx/wrap-context-desc
                      (fx/wrap-map-desc (fn [_] {:fx/type v/main-view})))
    :opts {:fx.opt/map-event-handler event-handler
           :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))

(defn remount []
  (fx/mount-renderer
    ctx
    renderer))

(defn -main
  "FIXME: Add doc"
  [& args]
  (println "Hello World!"))
