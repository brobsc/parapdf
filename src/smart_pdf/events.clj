(ns smart-pdf.events
  (:require [cljfx.api :as fx])
  (:import [javafx.application Platform]))

(defmulti event-handler :event/type)

(defmethod event-handler ::drag-file-hover
  [{:keys [drag-target fx/context]}]
  {:context
   (let [drag-src (fx/sub context :drag-src)]
     (if-not (= drag-src drag-target)
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
                                (assoc files idx-target src idx-src target))
                              files)]
              (fx/swap-context context merge {:drag-src ""
                                              :drag-target ""
                                              :files new-files}))})

(defmethod event-handler :default
  [e]
  (prn e))
