(ns smart-pdf.tools.pdf
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [pdfboxing.merge :as m])
  (:import
    [java.io
     File]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream]
    [org.apache.pdfbox.pdmodel.graphics.image
     PDImageXObject]
    [org.apache.pdfbox.pdmodel.common
     PDRectangle]
    [org.apache.pdfbox.tools.imageio
     ImageIOUtil]
    [org.apache.pdfbox.rendering
     PDFRenderer]))

(def ^PDRectangle A4 (PDRectangle/A4))

(defprotocol NamedFile
  "Retrieves name from objects."
  (->name ^String [x] "Retrieves name of `x`."))

(extend-protocol NamedFile
  nil
  (->name [_] nil)

  String
  (->name [s]
    (let [sep (File/separator)
          idx (string/last-index-of s sep)]
      (if idx
        (subs s (inc idx))
        s)))

  File
  (->name [f] (.getName f)))

(defprotocol Dimensional
  "Things that have a width and height."
  (width [x] "Returns width of `x`.")
  (height [x] "Returns height of `x`."))

(extend-protocol Dimensional
  clojure.lang.PersistentVector
  (width [[w _]] w)
  (height [[_ h]] h)

  PDRectangle
  (width [pd] (.getWidth pd))
  (height [pd] (.getHeight pd))

  PDImageXObject
  (width [img] (.getWidth img))
  (height [img] (.getHeight img)))

(defn dimensions
  "Returns [width height] of `x`."
  [x]
  [(width x) (height x)])

(defn ext
  "Returns extension of `arg` name.

  file.xml    => xml
  file.2.xml  => xml
  .file       => file
  file.       => (empty)"
  [arg]
  (let [s (->name arg)]
    (->> (string/last-index-of s ".")
         (inc)
         (subs s))))

(defn pdf?
  "Returns `true` when `f` has pdf extension."
  [f]
  (= "pdf" (ext f)))

(defn strip-ext
  "Removes possible extension from `arg` name.

  file.xml   => file
  file.2.xml => file.2
  file.      => file"
  [arg]
  (let [s (->name arg)
        idx (string/last-index-of s ".")]
    (if idx
      (subs s 0 idx)
      s)))

(defn fit
  "Returns [width height] of `a` fit inside `b`.

  `a` and `b` must be `Dimensional`."
  [a b]
  (->> [b a]
      (map dimensions)
      (apply map /)
      (apply min)
      (repeat 2)
      (map * (dimensions a))))

(defn center
  "Returns [x y] of `a` centered inside `b`.

  `a` and `b` must be `Dimensional`."
  [a b]
  (->> [b a]
       (map dimensions)
       (apply map -)
       (map #(/ % 2))
       (map float)))

(defn fit-in-a4
  "Returns [width height] of `arg` fit inside an A4 doc."
  [arg]
  (fit arg A4))

(defn center-in-a4
  "Returns [x y] of `arg` centered inside an A4 doc."
  [arg]
  (center arg A4))

(defn join
  "Joins a vector of pdfs `in` into `out`."
  [in out]
  (m/merge-pdfs :input in :output out))

(defn pdf->imgs
  "Converts a `pdf` File to many images compressed to `quality`, returning a `seq` of them."
  [^File pdf ^Float quality]
  (with-open [doc (PDDocument/load pdf)]
    (let [renderer (doto (PDFRenderer. doc)
                     (.setSubsamplingAllowed true))]
      (doall
       (map (fn [page]
              (let [bim (.renderImageWithDPI renderer page 72)
                    temp-file (File/createTempFile
                                (str (strip-ext pdf)
                                     "-"
                                     (inc page)
                                     "-") ".jpg")]
                (ImageIOUtil/writeImage bim (.getPath temp-file) 72 quality)
                temp-file))
            (range (.getNumberOfPages doc)))))))


(defn file->pdf
  "Returns a pdf `File` result of `f` converted to it, or `f` if it's already one.

  Currently only processes image files (png/jpeg)."
  [f]
  (let [f (io/file f)]
    (if-not (pdf? f)
      (with-open [doc (PDDocument.)]
        (let [img-name (strip-ext f)
              temp-file (File/createTempFile (str img-name "-pdf") ".pdf")
              pd-img (PDImageXObject/createFromFile (.getPath f) doc)
              page (PDPage. PDRectangle/A4)
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

(defn optimize-pdf
  "Returns compressed `pdf`.

  Attempts to optimize by reducing its quality by 10%."
  [^File pdf]
  (let [images (pdf->imgs pdf 0.9)
        temp-file (File/createTempFile
                    (str (strip-ext pdf) "-opt")
                    ".pdf")
        path (.getPath temp-file)]

    (as-> images images
      (map file->pdf images)
      (map (fn [^File f] (.getPath f)) images)
      (join images path))
     temp-file))
