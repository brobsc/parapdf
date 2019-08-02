(ns parapdf.tools.dimensional
  (:import
    [java.io
     File]
    [java.awt.image
     BufferedImage]
    [org.apache.pdfbox.pdmodel.common
     PDRectangle]
    [org.apache.pdfbox.pdmodel
     PDDocument
     PDPage
     PDPageContentStream]
    [org.apache.pdfbox.pdmodel.graphics.image
     JPEGFactory
     PDImageXObject]))

(set! *warn-on-reflection* true)

(def ^PDRectangle A4 (PDRectangle/A4))

(defprotocol PDImage
  "PDImageXObject creator."

  (pd-img
    [img doc quality]
    "Creates a PDImageXObject from an `img` to `doc`, at `quality`.

  `img` must be a File or BufferedImage.

  `quality` is only valid on BufferedImage."))

(extend-protocol PDImage
  File
  (pd-img [file doc quality] (PDImageXObject/createFromFileByContent file doc))

  BufferedImage
  (pd-img
    [img doc quality]
    (JPEGFactory/createFromImage doc img quality)))

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

(defn- append-pd-image
  [^PDDocument doc ^PDImageXObject img]
  (let [[width height] (fit-in-a4 img)
        [x y] (center-in-a4 [width height])
        page (PDPage. A4)]
    (.addPage doc page)
    (with-open [content-stream (PDPageContentStream.
                                 doc
                                 page
                                 true
                                 false)]
      (-> content-stream
          (.drawImage img
                      (float x)
                      (float y)
                      (float width)
                      (float height)))))
  doc)

(defn append-image!
  "Appends `img` to existing `doc`.

  `img` must be a `File` or `BufferedImage`. If it's the latter, `quality` may be provided (defaults to 1.0)."
  [doc img & {:keys [quality] :or {quality 1.0}}]
  (append-pd-image doc (pd-img img doc quality)))
