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
     File
     PipedInputStream
     InputStream
     ByteArrayOutputStream
     PipedOutputStream]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream]
    [org.apache.pdfbox.tools.imageio
     ImageIOUtil]
    [org.apache.pdfbox.multipdf
     PDFMergerUtility]
    [org.apache.pdfbox.rendering
     PDFRenderer]
    [org.apache.pdfbox.pdmodel.graphics.image
     PDImageXObject]))

(set! *warn-on-reflection* true)

(defn join
  "Joins a vector of pdfs `in` into `out`."
  [in out]
  (let [merger (doto
                 (PDFMergerUtility.)
                 (.setDestinationFileName (fpath out)))]
    (doseq [f in]
      (.addSource merger ^InputStream f))
    (.mergeDocuments merger)))

(defn pdf->imgs
  "Converts a `pdf` File to many images compressed to `quality`, returning a `seq` of them."
  [^File pdf ^Float quality]
  (with-open [doc (PDDocument/load pdf)]
    (let [renderer (doto (PDFRenderer. doc)
                     (.setSubsamplingAllowed true))]
      (doall
        (map (fn [page]
               (let [bim (.renderImageWithDPI renderer page 96)
                     pis (PipedInputStream.)
                     pos (PipedOutputStream. pis)]
                 (future (ImageIOUtil/writeImage bim "jpeg"
                                                 pos
                                                 96 quality)
                         (.close pos))
                 pis))
             (range (.getNumberOfPages doc)))))))

(defn stream->pdf
  [^PipedInputStream in]
  (let [pis (PipedInputStream.)
        pos (PipedOutputStream. pis)]
    (future
      (with-open [doc (PDDocument.)]
        (let [b-array (.readAllBytes in)
              _ (.close in)
              pd-img (PDImageXObject/createFromByteArray
                       doc
                       b-array
                       nil)
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
          (.save doc pos)
          (.close pos))))
    pis))

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
  (let [images (pdf->imgs pdf 0.8)
        temp-file (temp-file-from pdf)]
    (as-> images images
      (pmap stream->pdf images)
      (join images temp-file))
     temp-file))
