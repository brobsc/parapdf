(ns smart-pdf.components
  (:require
    [cljfx.api :as fx]
    [smart-pdf.events :as events]
    [clojure.java.io :as io]))

(defn add-file-button [_]
  {:fx/type :button
   :text "Adicionar arquivos"
   :on-action {:event/type ::events/open-file-dialog}})

(defn files-list [{:keys [fx/context]}]
  (let [target (fx/sub context :drag-target)]
    {:fx/type :list-view
     :style {:-fx-background-color :lightgray
             :-fx-alignment :top-left}
     :cell-factory (fn [f]
                     {:text (.getName f)
                      :style {:-fx-border-color :lightblue
                              :-fx-font-size 10
                              :-fx-border-width (if (= f target)
                                                  [3 0 0 0]
                                                  [0 0 0 0])}
                      :on-drag-detected {:event/type ::events/drag-file-start
                                         :fx/sync true
                                         :drag-file f}
                      :on-mouse-drag-entered {:event/type ::events/drag-file-hover
                                              :drag-target f}
                      :on-mouse-drag-released {:event/type ::events/drag-file-end
                                               :drag-target f}

                      :on-mouse-clicked {:event/type ::events/file-click
                                         :click-target f}})
     :items (fx/sub context :files)}))

(defn file-view [{:keys [fx/context]}]
  {:fx/type :web-view
   :url (when (fx/sub context :current-file)
          (str (io/resource "pdfjs/web/viewer.html")
               "?file=" (.toURI (fx/sub context :current-file))))})

(defn top-button-bar [{:keys [fx/context]}]
  {:fx/type :h-box
   :spacing 20
   :alignment :center
   :children [{:fx/type :button
               :text "Extrair"}
              {:fx/type :button
               :text "Dividir"}
              {:fx/type :button
               :on-action {:event/type ::events/optimize-pdf
                           :file (fx/sub context :current-file)}
               :text "Otimizar"}]})

(defn save-button [{:keys [fx/context]}]
  {:fx/type :button
   :text "Salvar"
   :default-button true
   :on-action {:event/type ::events/save-pdf}})
