(ns smart-pdf.events
  (:require
    [cljfx.api :as fx]
    [clojure.pprint :refer [pprint]])
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
                 :on-choose {:event/type ::add-file }}})

(defmethod event-handler ::add-file
  [{:keys [files]}]
  {:file {:files files
          :method :add
          :on-success {:event/type ::conj-file
                       :fx/sync true}}})

(defmethod event-handler ::conj-file
  [{:keys [fx/context result]}]
  {:context (fx/swap-context context update :files conj result)})

(defmethod event-handler ::file-click
  [{:keys [fx/context click-target] :as e}]
  (when (= 2 (.getClickCount (:fx/event e)))
    {:context (fx/swap-context context assoc :current-file click-target)}))

(defmethod event-handler ::save-pdf
  [{:keys [fx/context]}]
  {:file {:files (fx/sub context :files)
          :method :save
          :target "foo.pdf"
          :on-success {:event/type ::saved}}})

(defmethod event-handler ::optimize-pdf
  [{:keys [file]}]
  {:file {:files [file]
         :method :compress
         :on-success {:event/type ::sub-file
                      :src file}}})

(defmethod event-handler ::sub-file
  [{:keys [src fx/context] target :result}]
  {:context
   (let [files (fx/sub context :files)
         files (if-not (= target src)
                 (let [idx-src (.indexOf files src)]
                   (-> (vec files)
                       (assoc idx-src target)))
                 files)]
     (fx/swap-context context merge {:current-file nil
                                     :files files}))})

(defmethod event-handler :default
  [e]
  (println "==============")
  (println "Unhandled event" (:event/type e))
  (println "==============")
  (println)
  (pprint e))
