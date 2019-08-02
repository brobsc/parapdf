(ns parapdf.core
  (:require
    [cljfx.api :as fx]
    [parapdf.views :as v]
    [parapdf.tools.pdf :as pdf]
    [parapdf.events :as e])
  (:import
    [javafx.stage
     FileChooser
     FileChooser$ExtensionFilter]))

(set! *warn-on-reflection* true)

(def ctx (atom
             (fx/create-context
               {:files []
                :showing true
                :file-dialog true
                :drag-src ""
                :drag-target ""
                :current-file nil
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

(defn show-file-dialog [v dispatch!]
  (fx/on-fx-thread
    (fx/instance
      (fx/create-component
        {:fx/type fx/ext-instance-factory
         :create #(dispatch!
                    (let [fc (doto (FileChooser.)
                               (.setTitle "Escolher arquivo(s)"))]
                      (-> fc
                          (.getExtensionFilters)
                          (.addAll
                            (to-array
                              [(FileChooser$ExtensionFilter.
                                 "PDFs ou Imagens"
                                 ["*.pdf" "*.png" "*.jpg"])
                               (FileChooser$ExtensionFilter.
                                 "PDFs" ["*.pdf"])])))
                      (assoc (:on-choose v) :files
                             (-> fc
                                 (.showOpenMultipleDialog nil)))))}))))

(defn file-effect [{:keys [files method target on-success]}
                  dispatch!]
  (let [result (condp = method
                 :compress (map pdf/compress files)
                 :add (map pdf/file->pdf files)
                 :save (pdf/join files target)
                 files)]
    (doall
      (map #(dispatch! (assoc on-success :result %)) result))))

(def event-handler
  (-> e/event-handler
      (fx/wrap-co-effects
        {:fx/context (fx/make-deref-co-effect ctx)})
      (fx/wrap-effects
        {:context (fx/make-reset-effect ctx)
         :dispatch fx/dispatch-effect
         :file file-effect
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
