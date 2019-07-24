(ns smart-pdf.tools.pdf-test
  (:require
    [clojure.test :refer :all]
    [smart-pdf.tools.pdf :refer :all]))

(deftest ext-strip-ext-test
  (are [result input] (= result ((juxt strip-ext ext) input))
       ["very.long.name" "xml"] "very.long.name.xml"
       ["" "test"] ".test"
       ["test" "xml"] "test.xml"
       ["test.2" "xml"] "test.2.xml"
       ["test" "2"] "test.2"
       ["test" ""] "test."
       ["test" "xml"] "/archive/in/path/test.xml"
       ["test" "xml"] (java.io.File. "with-file/in/path/test.xml")))

(deftest dimensionable-test
  (are [result input] (= result (map int (dimensions input)))
       [100 200] [100 200]
       [595 841] A4))

(deftest fit-test
  (are [result a b] (= result (map int (fit a b)))
       [595 595] [100 100] A4
       [595 841] [210 297] A4
       [200 100] [20 10] [200 100]))

(deftest center-test
  (are [result a b] (= result (map int (center a b)))
       [0 0] A4 A4
       [5 5] [10 10] [20 20]))
