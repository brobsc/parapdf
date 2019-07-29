(ns smart-pdf.tools.pdf
  (:require
    [clojure.java.io :as io]
    [smart-pdf.tools
     [dimensional :refer [A4 center-in-a4 fit-in-a4
                          pd-img-from-file pd-img-from-jpeg]]
     [file :refer [fpath pdf? temp-file-from]]])
  (:import
    [java.io
     File
     InputStream]
    [org.apache.pdfbox.multipdf
     PDFMergerUtility]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream]
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

(defn- append-pd-image
  [pd-img ^PDDocument doc]
  (let [[width height] (fit-in-a4 pd-img)
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

(defmulti append-image!
  "Appends `img` to existing `doc`.

  `img` must be a `File` or `BufferedImage`. If it's the latter, `quality` must be provided."
  {:arglists '([img doc quality])}
  (fn [img _ _] (class img)))

(defmethod append-image! File
  [file ^PDDocument doc _]
  (append-pd-image (pd-img-from-file file doc) doc))

(defmethod append-image! java.awt.image.BufferedImage
  [img ^PDDocument doc quality]
  (append-pd-image (pd-img-from-jpeg img doc quality) doc))

(defn imgs->pdf
  "Writes to `out` all `imgs` compressed to `quality`."
  [^File out quality imgs]
  (with-open [doc (PDDocument.)]
    (doseq [img imgs]
      (append-image! img doc quality))
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
          (append-image! f doc 1.0)
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
