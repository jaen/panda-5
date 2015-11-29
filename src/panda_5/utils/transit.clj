(ns panda-5.utils.transit
  (:require [cognitect.transit :as transit]
            [panda-5.utils.time :as time-utils]
            [immutant.codecs.transit :as immutant-transit])
  (:import (org.joda.time DateTime)
           (java.io ByteArrayInputStream ByteArrayOutputStream)))

(def ^:private clj-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] v)
   (fn [v] (time-utils/to-str v))))

(def ^:private clj-time-reader
  (transit/read-handler
   (fn [v] (time-utils/from-str v))))

(def write-handlers
  {DateTime clj-time-writer})

(def read-handlers
  {"m" clj-time-reader})

(defn to-str [obj]
  (let [string-writer  (ByteArrayOutputStream.)
        transit-writer (transit/writer string-writer :json {:handlers write-handlers})]
    (transit/write transit-writer obj)
    (.toString string-writer)))

(defn from-str [str]
  (let [string-reader  (ByteArrayInputStream. (.getBytes str))
        transit-reader (transit/reader string-reader :json {:handlers read-handlers})]
    (transit/read transit-reader)))

(immutant-transit/register-transit-codec
  :name :transit-msgpack
  :type :msgpack
  :read-handlers read-handlers
  :write-handlers write-handlers)

;(immutant-transit/register-transit-codec
; :name :transit-json
; :type :json
; :read-handlers read-handlers
; :write-handlers write-handlers)