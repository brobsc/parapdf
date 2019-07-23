(ns smart-pdf.events
  (:require [cljfx.api :as fx])
  (:import [javafx.application Platform]))

(defmulti event-handler :event/type)

(defmethod event-handler ::drag-file-hover
  [{:keys [drag-target fx/context]}]
  {:context
   (let [old-target (fx/sub context :drag-target)]
     (if-not (= old-target drag-target)
       (fx/swap-context context assoc :drag-target drag-target)
       context))})

(defmethod event-handler ::drag-file-start
  [{:keys [drag-file fx/context] :as e}]
  (let [fx-event (:fx/event e)]
    (-> fx-event
        (.getSource)
        (.startFullDrag))
    (.consume fx-event))
  {:context (fx/swap-context context assoc :drag-src drag-file) })

(defmethod event-handler ::drag-file-end
  [{:keys [fx/context]}]
  {:context (let [files (fx/sub context :files)
                  target (fx/sub context :drag-target)
                  src (fx/sub context :drag-src)
                  new-files (if-not (= target src)
                              (let [idx-target (.indexOf files target)
                                    idx-src (.indexOf files src)]
                                (-> (vec files)
                                    (assoc idx-target src)
                                    (assoc idx-src target)))
                              files)]
              (fx/swap-context context merge {:drag-src {}
                                              :drag-target {}
                                              :files new-files}))})

(defmethod event-handler ::open-file-dialog
  [{:keys [fx/context]}]
  {:file-dialog {:type :open
                 :on-choose {:event/type ::add-file}}})

(defmethod event-handler ::add-file
  [{:keys [fx/context files]}]
  {:context (fx/swap-context context update :files into files)})

(defmethod event-handler ::file-click
  [{:keys [fx/context click-target] :as e}]
  (when (= 2 (.getClickCount (:fx/event e)))
    {:context (fx/swap-context context assoc :current-file click-target)}))

(defmethod event-handler ::save-pdf
  [{:keys [fx/context]}]
  {:save-pdf {:files (fx/sub context :files)}})

(defmethod event-handler :default
  [e]
  (println "Unhadled event" (:event/type e))
  (prn e))
