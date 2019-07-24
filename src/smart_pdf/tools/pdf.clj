(ns smart-pdf.tools.pdf
  (:require
    [pdfboxing.merge :as m]
    [pdfboxing.split :as s])
  (:import
    [java.io
     File]
    [java.awt.image
     BufferedImage]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream
     PDPageContentStream$AppendMode]
    [org.apache.pdfbox.pdmodel.graphics.image
     PDImageXObject]
    [org.apache.pdfbox.pdmodel.common
     PDRectangle]
    [org.apache.pdfbox.tools.imageio
     ImageIOUtil]
    [org.apache.pdfbox.rendering
     ImageType
     PDFRenderer]))

(defn strip-extension
  "Removes an extension from a `s` representing a file name.

  file.xml   -> file
  file.2.xml -> file.2
  file.      -> file"
  [^String s]
  (let [idx (clojure.string/last-index-of s ".")]
    (if-not (= -1 idx)
      (subs s 0 idx)
      s)))

(defn pdf->imgs
  "Converts a `pdf` File to many images compressed to `quality`, returning a `seq` of image files."
  [^File pdf ^Float quality]
  (with-open [doc (PDDocument/load pdf)]
    (let [renderer (doto (PDFRenderer. doc)
                     (.setSubsamplingAllowed true))]
      (doall
       (map (fn [page]
              (let [bim (.renderImageWithDPI renderer page 72)
                    temp-file (File/createTempFile
                                (str (strip-extension (.getName pdf))
                                     "-"
                                     (inc page)
                                     "-") ".jpg")]
                (ImageIOUtil/writeImage bim (.getPath temp-file) 72 quality)
                temp-file))
            (range (.getNumberOfPages doc)))))))

(defn fit-img
  "Returns  [width height] of `img` fit inside an A4 doc, with optional `padding`."
  ([^PDImageXObject img] (fit-img img 0))
  ([^PDImageXObject img padding]
  (let [w-ratio (/ (- (.getWidth PDRectangle/A4) padding)
                   (.getWidth img))
        h-ratio (/ (- (.getHeight PDRectangle/A4) padding)
                    (.getHeight img))
        ratio (min w-ratio h-ratio)]
    [(* ratio (.getWidth img)) (* ratio (.getHeight img))])))

(defn center-img
  "Returns [x y] of a `width` and `height` centered inside an A4 doc."
  [w h]
  [(/ (- (.getWidth PDRectangle/A4) w) 2)
   (/ (- (.getHeight PDRectangle/A4) h) 2)])

(defn join
  "Joins a vector of pdfs `in` into `out`."
  [in out]
  (m/merge-pdfs :input in :output out))

(defn img->pdf
  "Returns a pdf `File` result of `img` converted to it."
  [^File img]
  (with-open [doc (PDDocument.)]
    (let [img-name (-> (.getName img) (strip-extension))
          temp-file (File/createTempFile (str img-name "-pdf") ".pdf")
          pd-img (PDImageXObject/createFromFile (.getPath img) doc)
          page (PDPage. PDRectangle/A4)
          [width height] (fit-img pd-img)
          [x y] (center-img width height)]
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
      temp-file)))

(defn optimize-pdf
  "Returns compressed `pdf`.

  Attemps to optimize by reducing its quality by 30%."
  [^File pdf]
  (let [images (pdf->imgs pdf 0.7)
        temp-file (File/createTempFile
                    (str (strip-extension (.getName pdf)) "-opt")
                    ".pdf")
        path (.getPath temp-file) ]

    (as-> images  images
      (map img->pdf images)
      (map (fn [^File f] (.getPath f)) images)
      (join images path))
     temp-file))
