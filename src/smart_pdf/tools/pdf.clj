(ns smart-pdf.tools.pdf
  (:require
    [pdfboxing.merge :as m]
    [pdfboxing.split :as s])
  (:import
    [java.io File]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream
     PDPageContentStream$AppendMode]
    [org.apache.pdfbox.pdmodel.graphics.image
     PDImageXObject]
    [org.apache.pdfbox.pdmodel.common
     PDRectangle]))

(defn strip-extension
  "Removes an extension from a string representing a file name.

  file.xml   -> file
  file.2.xml -> file.2
  file.      -> file"
  [^String s]
  (let [idx (clojure.string/last-index-of s ".")]
    (if-not (= -1 idx)
      (subs s 0 idx)
      s)))

(defn fit-img
  "Fits an `img` inside a A4 document with `padding`, returning a vector of width and height."
  [^PDImageXObject img padding]
  (let [w-ratio (/ (- (.getWidth PDRectangle/A4) padding)
                   (.getWidth img))
        h-ratio (/ (- (.getHeight PDRectangle/A4) padding)
                    (.getHeight img))
        ratio (min w-ratio h-ratio)]
    [(* ratio (.getWidth img)) (* ratio (.getHeight img))]))

(defn center-img
  "Centers width and height inside a A4 document, returning a vector of x and y."
  [w h]
  [(/ (- (.getWidth PDRectangle/A4) w) 2)
   (/ (- (.getHeight PDRectangle/A4) h) 2)])

(defn join
  "Joins a vector of pdfs `in` into `out`."
  [in out]
  (m/merge-pdfs :input in :output out))

(defn img->pdf
  "Converts an `img` File object to a PDF. Returns a PDF File object."
  [^File img]
  (with-open [doc (PDDocument.)]
    (let [img-name (-> (.getName img) (strip-extension))
          temp-file (File/createTempFile (str img-name "-pdf") ".pdf")
          pd-img (PDImageXObject/createFromFile (.getPath img) doc)
          page (PDPage. PDRectangle/A4)
          [width height] (fit-img pd-img 20)
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
