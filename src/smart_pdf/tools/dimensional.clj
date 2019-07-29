(ns smart-pdf.tools.dimensional
  (:import
    [org.apache.pdfbox.pdmodel.common
     PDRectangle]
    [org.apache.pdfbox.pdmodel.graphics.image
     JPEGFactory
     PDImageXObject]))

(set! *warn-on-reflection* true)

(def ^PDRectangle A4 (PDRectangle/A4))

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

(defn pd-img-from-file ^PDImageXObject
  [file doc]
  (PDImageXObject/createFromFileByContent file doc))

(defn pd-img-from-jpeg ^PDImageXObject
  [img doc quality]
  (JPEGFactory/createFromImage doc img quality))
