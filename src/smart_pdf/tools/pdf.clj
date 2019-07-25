(ns smart-pdf.tools.pdf
  (:require
    [clojure.java.io :as io]
    [smart-pdf.tools.file :refer [fpath pdf?
                                  temp-file-from]]
    [smart-pdf.tools.dimensional :refer [A4
                                         pd-img-from-file
                                         pd-img-from-jpeg
                                         fit-in-a4 center-in-a4]])
  (:import
    [java.io
     File
     InputStream]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream]
    [org.apache.pdfbox.multipdf
     PDFMergerUtility]
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
        (pmap (fn [page]
               (.renderImageWithDPI renderer page 96))
             (range (.getNumberOfPages doc)))))))

(defn append-image
  "Appends `img` to existing `doc`, compressed to `quality`."
  [img ^PDDocument doc quality]
  (let [pd-img (pd-img-from-jpeg doc img quality)
        [width height] (fit-in-a4 pd-img)
        [x y] (center-in-a4 [width height])
        page (PDPage. A4)]
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
                      (float height))))))

(defn imgs->pdf
  "Writes to `out` all `imgs` compressed to `quality`."
  [^File out quality imgs]
  (with-open [doc (PDDocument.)]
    (doseq [img imgs]
      (append-image img doc quality))
    (.save doc out))
  out)

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

  Reduces dpi to 96 and quality by 20%."
  [^File pdf]
  (let [result (temp-file-from pdf)]
    (->> pdf
        (pdf->imgs)
        (imgs->pdf result 0.8))))
