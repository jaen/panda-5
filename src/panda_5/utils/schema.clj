(ns panda-5.utils.schema
  (:require [schema-tools.core :as st]
            [schema-tools.walk :as st-walk]))

(defn with-optional-keys [schema]
  (st-walk/walk schema
                (fn [x]
                  (let [y (with-optional-keys x)]
                    (if (and (map? y) (not (record? y)))
                      (st/optional-keys y)
                      y)))
                identity))