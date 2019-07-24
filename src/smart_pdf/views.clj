(ns smart-pdf.views
  (:require [cljfx.api :as fx]
            [smart-pdf.components :as c]))

(defn file-chooser [{:keys [fx/context]}]
  {:fx/type :file-chooser
   :showing (fx/sub context :file-dialog)})

(defn main-view [{:keys [fx/context]}]
  {:fx/type :stage
   :showing (fx/sub context :showing)
   :width 560
   :height 800
   :resizable false
   :scene {:fx/type :scene
           :root {:fx/type :anchor-pane
                  :children [{:fx/type c/top-button-bar
                              :anchor-pane/top 40
                              :anchor-pane/left 200
                              :anchor-pane/right 37}
                             {:fx/type c/files-list
                              :anchor-pane/top 80
                              :anchor-pane/left 37
                              :anchor-pane/right 375
                              :anchor-pane/bottom 80}
                             {:fx/type c/add-file-button
                              :anchor-pane/bottom 40
                              :anchor-pane/left 37}
                             {:fx/type c/file-view
                              :anchor-pane/right 37
                              :anchor-pane/top 80
                              :anchor-pane/left 200
                              :anchor-pane/bottom 80}
                             {:fx/type c/save-button
                              :anchor-pane/right 37
                              :anchor-pane/bottom 40 }]}}})
