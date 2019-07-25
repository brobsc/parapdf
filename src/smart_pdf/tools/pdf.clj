(ns smart-pdf.tools.pdf
  (:require
    [clojure.java.io :as io]
    [smart-pdf.tools.file :refer [strip-ext fpath pdf?
                                  temp-file-from]]
    [smart-pdf.tools.dimensional :refer [A4 pd-img-from-file
                                         fit-in-a4 center-in-a4]]
    [pdfboxing.merge :as m])
  (:import
    [java.io
     File]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream]
    [org.apache.pdfbox.tools.imageio
     ImageIOUtil]
    [org.apache.pdfbox.rendering
     PDFRenderer]))

(set! *warn-on-reflection* true)

(defn join
  "Joins a vector of pdfs `in` into `out`."
  [in out]
  (m/merge-pdfs :input (map fpath in) :output out))

(defn pdf->imgs
  "Converts a `pdf` File to many images compressed to `quality`, returning a `seq` of them."
  [^File pdf ^Float quality]
  (with-open [doc (PDDocument/load pdf)]
    (let [renderer (doto (PDFRenderer. doc)
                     (.setSubsamplingAllowed true))]
      (doall
       (map (fn [page]
              (let [bim (.renderImageWithDPI renderer page 72)
                    temp-file (temp-file-from pdf :ext "jpg" :number (inc page))]
                (ImageIOUtil/writeImage bim (fpath temp-file) 72 quality)
                temp-file))
            (range (.getNumberOfPages doc)))))))


(defn file->pdf
  "Returns a pdf `File` result of `f` converted to it, or `f` if it's already one.

  Currently only processes image files (png/jpeg)."
  [f]
  (let [f (io/file f)]
    (if-not (pdf? f)
      (with-open [doc (PDDocument.)]
        (let [temp-file (temp-file-from f :ext "pdf")
              pd-img (pd-img-from-file f doc)
              page (PDPage. A4)
              [width height] (fit-in-a4 pd-img)
              [x y] (center-in-a4 [width height])]
          (.addPage doc page)
          (with-open [content-stream (PDPageContentStream.
                                       doc
                                       page
                                       true
                                       false)]
            (-> content-stream
                (.drawImage pd-img
                            (float x)
                            (float y)
                            (float width)
                            (float height))))
          (.save doc temp-file)
          temp-file))
      f)))

(defn compress
  "Returns compressed `pdf` `File`.

  Reduces dpi to 72 and quality by 10%."
  [^File pdf]
  (let [images (pdf->imgs pdf 0.9)
        temp-file (temp-file-from pdf)
        path (fpath temp-file)]
    (as-> images images
      (doall (pmap file->pdf images))
      (map fpath images)
      (join images path))
     temp-file))
