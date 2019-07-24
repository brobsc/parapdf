(ns smart-pdf.core
  (:require
    [cljfx.api :as fx]
    [smart-pdf.views :as v]
    [smart-pdf.tools.pdf :as pdf]
    [smart-pdf.events :as e])
  (:import
    [java.io
     File]
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

(defn save-pdf [v _]
  (when-let [files (some->> (:files v)
                            (map (fn [^java.io.File f]
                                   (.getPath f))))]
    (pdf/join files "foo.pdf")))

(defn ext [^File f]
  (let [s (.getName f)]
    (subs s (+ 1 (clojure.string/last-index-of s ".")))))

(defn add-files [{:keys [files]} _]
  (let [files (map (fn [f]
                     (if-not (= "pdf" (ext f))
                       (pdf/img->pdf f)
                       f)) files)]
    (swap! ctx fx/swap-context update :files concat files)))

(def event-handler
  (-> e/event-handler
      (fx/wrap-co-effects
        {:fx/context (fx/make-deref-co-effect ctx)})
      (fx/wrap-effects
        {:context (fx/make-reset-effect ctx)
         :dispatch fx/dispatch-effect
         :save-pdf save-pdf
         :add-files add-files
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
