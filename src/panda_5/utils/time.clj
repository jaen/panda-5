(ns panda-5.utils.time
  (:require [clj-time.format :as time-format]
            [clj-time.core :as time]))

;; Generic formatter

  (def app-time-formatter
    (time-format/formatters :date-time))

  (defn to-str [date-time]
    (time-format/unparse app-time-formatter date-time))

  (defn from-str [date-time-str]
    (time-format/parse app-time-formatter date-time-str))

;; DB formatters

  (def db-date-time-formatter
    (time-format/formatters :date-time))

  (defn to-db-json-str [datetime]
    (time-format/unparse db-date-time-formatter datetime))

  (defn from-db-json-str [datetime-str]
    (time-format/parse db-date-time-formatter datetime-str))

;; === View formatters

  (def pl-timezone
    "Timezone for Warsaw, Poland."

    (time/time-zone-for-id "Europe/Warsaw"))

  (def check-time-formatter
    "Formats the datetime for check time."

    (time-format/with-zone (time-format/formatter "HH:mm:ss dd.MM.YYYY") pl-timezone))

  (defn to-check-time-str [date-time]
    (time-format/unparse check-time-formatter date-time))

  (def check-time-as-id-formatter
    "Formats the datetime for check time."

    (time-format/with-zone (time-format/formatter "YYYY-MM-DD-'at'-HH-mm-ss") pl-timezone))

  (defn to-check-time-as-id-str [date-time]
    (time-format/unparse check-time-as-id-formatter date-time))