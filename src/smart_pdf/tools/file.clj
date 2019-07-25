(ns smart-pdf.tools.file
  (:require
    [clojure.string :as string])
  (:import
    [java.io File]))

(set! *warn-on-reflection* true)

(defprotocol Filey
  "Objects representing files or quasi-files.

  Provides 'light-weight' results for pure strings (refrains from transforming it into a file whenever possible)."
  (fname ^String [x] "Retrieves name of `x`.")
  (fpath ^String [x] "Retrieves canonical path of `x`."))

(extend-protocol Filey
  nil
  (fname [_] nil)

  String
  (fname [s]
    (let [sep (File/separator)
          idx (string/last-index-of s sep)]
      (if idx
        (subs s (inc idx))
        s)))
  (fpath [s]
    (if-not (string/starts-with? s "/")
      (-> s
          (File.)
          (.getCanonicalPath))
      s))

  File
  (fname [f] (.getName f))
  (fpath [f] (.getCanonicalPath f)))

(defn ext
  "Returns extension of `arg` name.

  file.xml    => xml
  file.2.xml  => xml
  .file       => file
  file.       => (empty)"
  [arg]
  (let [s (fname arg)]
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
  (let [s (fname arg)
        idx (string/last-index-of s ".")]
    (if idx (subs s 0 idx) s)))

(defn temp-file-from
  "Creates a temp file from `base-file`, with optional `:ext` and `:number`.

  /path/to/file/myfile.pdf -> /<temp-path>/myfile-pdf<rand>.pdf
  /path/to/file/myfile.pdf :ext jpg -> .../myfile-pdf<rand>.jpg
  /path/to/file/myfile.pdf :ext jpg :number 1 -> .../myfile-pdf-1<rand>.jpg"
  ^File
  [base-file & {:keys [number] extension :ext}]
  (let [base-ext (ext base-file)]
    (File/createTempFile (str (strip-ext base-file)
                            "-"
                            base-ext
                            "-"
                            number)
                       (str "." (or extension base-ext)))))
