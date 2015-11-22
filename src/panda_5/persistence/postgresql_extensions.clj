(ns panda-5.persistence.postgresql-extensions
  (:require [cheshire.core :as json]
            [jdbc.proto :as proto]
            [clj-time.coerce :as time-coerce])
  (:import [org.joda.time DateTime]
           [clojure.lang IPersistentMap]
           [org.postgresql.util PGobject]
           [java.sql Timestamp]))

(extend-protocol proto/ISQLType
  IPersistentMap

  (set-stmt-parameter! [this conn stmt index]
    (let [value (proto/as-sql-type this conn)]
      (.setObject stmt index value)))

  (as-sql-type [this conn]
    (doto (PGobject.)
      (.setType "jsonb")
      (.setValue (json/encode this))))

  DateTime
  (set-stmt-parameter! [this conn stmt index]
    (let [value (proto/as-sql-type this conn)]
      (.setTimestamp stmt index value)))

  (as-sql-type [this conn]
    (doto (PGobject.)
      (.setType "timestamp")
      (.setValue (time-coerce/to-sql-time this)))))

(extend-protocol proto/ISQLResultSetReadColumn
  PGobject
  (from-sql-type [pgobj conn metadata idx]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "jsonb" (json/parse-string value)
        "json"  (json/parse-string value)
        :else value)))

  Timestamp
  (result-set-read-column [value metadata idx]
    (time-coerce/from-sql-time value)))