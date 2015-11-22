(ns panda-5.api.coercion
  (:require [schema.core :as s]
            [clojure.set :as set]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [schema.coerce :as schema-coerce]
            [schema.spec.core :as schema-spec]
            [schema.utils :as schema-utils]
            [schema.macros :as schema-macros]
            [clojure.walk :as walk]
            [panda-5.api.schemas :as schemas])
  (:import [org.joda.time DateTime]
           [clojure.lang Keyword]
           [schema.core OptionalKey]))


(def PL-TIMEZONE
  "Timezone for Warsaw, Poland."

  (time/time-zone-for-id "Europe/Warsaw"))

(def CAROUSEL-STATE-MAPPING
  "Maps carousel state string enum returned from API to keywords."

  {"STOPPED"   :stopped
   "RUNNING"   :running
   "BROKEN"    :broken
   "DESTROYED" :destroyed})

(def CAROUSEL-STATE-MAPPING-REVERSE
  "Maps carousel state keyword to string enum required by the API."

  (set/map-invert CAROUSEL-STATE-MAPPING))

(defn api->carousel-state [state]
  (get CAROUSEL-STATE-MAPPING state))

(defn carousel-state->api [state]
  (get CAROUSEL-STATE-MAPPING-REVERSE state))


(def ACCIDENT-TYPE-MAPPING
  "Maps accident type string enum returned from API to keywords."

  {"STABILITY"  :stability
   "ELECTRIC"   :electric
   "MECHANICAL" :mechanical
   "DEATH"      :death})

(def ACCIDENT-TYPE-MAPPING-REVERSE
  "Maps accident type keyword to string enum required by the API."

  (set/map-invert ACCIDENT-TYPE-MAPPING))

(defn api->accident-type [type]
  (get ACCIDENT-TYPE-MAPPING type))

(defn accident-type->api [type]
  (get ACCIDENT-TYPE-MAPPING-REVERSE type))

(defn api->date-time
  "Coerces milliseconds in UTC timezone into a Joda Time DateTime."
  [millisecond-timestamp]

  (time/to-time-zone (time-coerce/from-long millisecond-timestamp) PL-TIMEZONE))

(defn date-time->api
  "Coerces a Joda Time DateTime into milliseconds in UTC."
  [date-time]

  (time-coerce/to-long date-time))

(def make-coercer
  schema-coerce/coercer)

(defn make-reverse-coercer
  "Creates a coercer which first validates the schema and then applies a coercion function."
  [schema coercion-matcher]

  (schema-spec/run-checker
   (fn [s params]
     (let [checker (schema-spec/checker (s/spec s) params)]
       (if-let [coercer (coercion-matcher s)]
         (fn [x]
           (schema-macros/try-catchall
            (let [check-result (checker x)]
              (if (schema-utils/error? check-result)
                check-result
                (coercer check-result)))
            (catch t
                   (schema-macros/validation-error s x t))))
         checker)))
   true
   schema))

(defn wrap-optional-key [key]
  (if (instance? OptionalKey key)
    key
    (s/optional-key key)))

(defn transform-keys
  "Recursively transforms all map keys from strings to with given function."
  [m f]

  (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m))

#_(defn make-all-keys-optional [schema]
    (transform-keys schema (fn [[key value]] [(wrap-optional-key key) value])))

(defn make-all-keys-optional [schema]
  (into {} (map (fn [[k v]] [(wrap-optional-key k) v]) schema)))

;; ===================


(def api->domain-matcher
  {schemas/CarouselState api->carousel-state
   schemas/AccidentType  api->accident-type
   DateTime              api->date-time
   s/Keyword             schema-coerce/string->keyword
   s/Bool                schema-coerce/string->boolean
   s/Int                 schema-coerce/safe-long-cast
   Keyword               schema-coerce/string->keyword})

(def domain->api-matcher
  {schemas/CarouselState carousel-state->api
   schemas/AccidentType  accident-type->api
   DateTime              date-time->api})

(defn api->domain-coercer [schema & [options]]
  (make-coercer schema (merge api->domain-matcher (:matcher options {}))))

(defn domain->api-coercer [schema & [options]]
  (make-reverse-coercer schema (merge domain->api-matcher (:matcher options {}))))

(defn valid? [schema-result]
  (not (schema-utils/error? schema-result)))
