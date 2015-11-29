(ns panda-5.utils.core)

;; Clojure, y u no zip : |
(defn zip
  "Zips collections."
  [& colls]

  (partition (count colls) (apply interleave colls)))
