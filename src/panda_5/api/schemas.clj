(ns panda-5.api.schemas
  (:require [schema.core :as s])
  (:import [org.joda.time DateTime]))

;; == schemas

  (def CarouselState
    (s/enum :running :stopped :broken :destroyed))

  (def Carousel
    {:id             s/Int
     :name           s/Str
     :carousel-state CarouselState
     :price          s/Int})

  (def CarouselList
    [Carousel])

  (def Accident
    {:accident-id                    s/Int
     :diagnostic-number              s/Int
     :people-injured                 s/Int
     :people-dead                    s/Int
     :created-by-inspection          s/Bool
     :auto-created                   s/Bool
     :created-on-mechanical-overview s/Bool
     :description                    s/Str
     :date-created                   DateTime})

  (def AccidentList
    [Accident])

  (def AccidentType
    (s/enum :stability :electric :mechanical :death))

  (def RepairRequest
    {:diagnostic-number s/Int
     :type              AccidentType
     :carousel-id       s/Int
     :scheduled-time    DateTime})

  (def TeamInfo
    {:name   s/Str
     :points s/Int})