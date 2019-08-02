(ns parapdf.tools.pdf
  (:require
    [clojure.java.io :as io]
    [parapdf.tools
     [dimensional :refer [append-image!]]
     [file :refer [fpath pdf? temp-file-from]]])
  (:import
    [java.io
     File
     InputStream]
    [org.apache.pdfbox.multipdf
     PDFMergerUtility]
    [org.apache.pdfbox.pdmodel
     PDDocument]
    [org.apache.pdfbox.rendering
     PDFRenderer]))

(set! *warn-on-reflection* true)

(defn join
  "Joins a vector of pdfs `in` into `out`."
  [in out]
  (let [merger (doto
                 (PDFMergerUtility.)
                 (.setDestinationFileName (fpath out)))]
    (doseq [f in]
      (.addSource merger (io/input-stream f)))
    (.mergeDocuments merger)))

(defn pdf->imgs
  "Converts a `pdf` File to many BufferedImage, returning a `seq` of them."
  [^File pdf]
  (with-open [doc (PDDocument/load pdf)]
    (let [renderer (doto (PDFRenderer. doc)
                     (.setSubsamplingAllowed true))]
      (doall
        (map (fn [page]
                (.renderImageWithDPI renderer page 96))
              (range (.getNumberOfPages doc)))))))


(defn imgs->pdf
  "Writes to `out` all `imgs` compressed to `quality`."
  [^File out quality imgs]
  (with-open [doc (PDDocument.)]
    (doseq [img imgs]
      (append-image! doc img :quality quality))
    (.save doc out))
  out)

(defn file->pdf
  "Returns a pdf `File` result of `f` converted to it, or `f` if it's already one.

  Currently only processes image files (png/jpeg)."
  [f]
  (let [f (io/file f)]
    (if-not (pdf? f)
      (with-open [doc (PDDocument.)]
        (let [temp-file (temp-file-from f :ext "pdf")]
          (append-image! doc f)
          (.save doc temp-file)
          temp-file))
      f)))

(defn compress
  "Returns compressed `pdf` `File`.

  Reduces dpi to 96 and quality by 20%."
  [^File pdf]
  (let [result (temp-file-from pdf)]
    (->> pdf
         (pdf->imgs)
         (imgs->pdf result 0.8))))
