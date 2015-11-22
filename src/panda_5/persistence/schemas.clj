(ns panda-5.persistence.schemas
  (:require [panda-5.utils.schema :as schema-utils]
            [panda-5.api.schemas :as api-schemas]

            [schema.core :as s])
  (:import [java.util UUID]))

(def EventType
  (s/enum :repair-requested
          :carousel-started
          :carousel-stopped
          :carousel-broken-down
          :carousel-repaired
          :carousel-destroyed))

(def RepairRequestedPayload
  api-schemas/RepairRequest)

(def Event
  (s/conditional
   #(or (= (:type %) :repair-requested)
        (= (:type %) "repairRequested"))
   {:uuid    UUID
    :type    EventType
    :payload RepairRequestedPayload}))

(def EventQuery (schema-utils/with-optional-keys Event))
