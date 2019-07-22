(ns smart-pdf.core
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io])
  (:import [javafx.scene.input TransferMode
            ClipboardContent MouseEvent]))


(def ctx (atom
             (fx/create-context
               {:files ["ae"
                        "bc"
                        "fg"]
                :showing true
                :drag-src ""
                :drag-target ""
                :current-file "pdfjs/web/relatorio.pdf"
                :prefs {}})))

(defn file-cell [{:keys [fx/context file]}]
  {:fx/type :label
   :text file})

(defn files-list [{:keys [fx/context]}]
  (let [target (fx/sub context :drag-target)]
    {:fx/type :list-view
     :style {:-fx-background-color :lightgray
             :-fx-alignment :top-left}
     :cell-factory (fn [f]
                     {:text f
                      :style {:-fx-border-color :lightblue
                              :-fx-border-width (if (= f target)
                                                  [3 0 0 0]
                                                  [0 0 0 0])}
                      :on-drag-detected {:event/type ::drag-file-start
                                         :drag-file f}
                      :on-mouse-drag-entered {:event/type ::drag-file-hover
                                              :drag-target f}
                      :on-mouse-drag-released {:event/type ::drag-file-end
                                               :drag-target f}})
     :items (fx/sub context :files)}))

(defn file-view [{:keys [fx/context]}]
  {:fx/type :web-view
   :url (str (io/resource "pdfjs/web/viewer.html")
             "?file=" (io/resource (fx/sub context :current-file)))})

(defn top-button-bar [{:keys [fx/context]}]
  {:fx/type :h-box
   :spacing 20
   :alignment :center
   :children [{:fx/type :button
               :text "Extrair"}
              {:fx/type :button
               :text "Dividir"}
              {:fx/type :button
               :text "Otimizar"}]})

(defn main-view [{:keys [fx/context]}]
  {:fx/type :stage
   :showing (fx/sub context :showing)
   :width 560
   :height 800
   :resizable false
   :scene {:fx/type :scene
           :root {:fx/type :anchor-pane
                  :children [{:fx/type top-button-bar
                              :anchor-pane/top 40
                              :anchor-pane/left 200
                              :anchor-pane/right 40}
                             {:fx/type files-list
                              :anchor-pane/top 80
                              :anchor-pane/left 40
                              :anchor-pane/right 375
                              :anchor-pane/bottom 80}
                             {:fx/type :button
                              :anchor-pane/bottom 40
                              :anchor-pane/left 40
                              :text "+"}
                             {:fx/type file-view
                              :anchor-pane/right 40
                              :anchor-pane/top 80
                              :anchor-pane/left 200
                              :anchor-pane/bottom 80}
                             {:fx/type :button
                              :anchor-pane/right 40
                              :anchor-pane/bottom 40
                              :text "Salvar"}]}}})

(defmulti event-handler :event/type)
(defmethod event-handler ::close-win
  [_]
  (swap! ctx fx/swap-context update-in [:showing] not))

(defmethod event-handler ::drag-file-hover
  [{:keys [drag-target]}]
  (let [drag-src (fx/sub @ctx :drag-src)]
    (when-not (= drag-src drag-target)
      (swap! ctx fx/swap-context assoc-in [:drag-target] drag-target))))

(defmethod event-handler ::drag-file-start
  [{:keys [drag-file] :as e}]
  (let [fx-event (:fx/event e)]
    (println "drag from " drag-file)
    (-> fx-event
        (.getSource)
        (.startFullDrag))
    (swap! ctx fx/swap-context assoc-in [:drag-src] drag-file)
    (.consume fx-event)))

(defmethod event-handler ::drag-file-end
  [e]

  (let [files (fx/sub @ctx :files)
        target (fx/sub @ctx :drag-target)
        src (fx/sub @ctx :drag-src)
        new-files (if-not (= target src)
                    (let [idx-target (.indexOf files target)
                          idx-src (.indexOf files src)]
                      (assoc files idx-target src idx-src target))
                    files)]
    (swap! ctx fx/swap-context merge {:drag-src ""
                                    :drag-target ""
                                    :files new-files}))
  )

(defmethod event-handler :default
  [e]
  (println "default")
  (println e))

(def renderer
  (fx/create-renderer
    :middleware (comp fx/wrap-context-desc
                      (fx/wrap-map-desc (fn [_] {:fx/type main-view})))
    :opts {:fx.opt/map-event-handler event-handler
           :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))

#_(fx/mount-renderer
  ctx
  renderer)

#_(renderer)


(defn -main
  "FIXME: Add doc"
  [& args]
  (println "Hello World!"))
