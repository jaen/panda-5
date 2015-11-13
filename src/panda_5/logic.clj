(ns panda-5.logic
  (require [clj-time.core :as time]
           [clj-time.predicates :as time-predicates]

           [panda-5.api :as api]))

;; Defines.

  (def opening-hours-mapping
    "A map that stores what hours the amusement park is open on given days.
     Hours are in Europe/Warsaw timezone"
    {:weekday [12 21]
     :weekend [10 23]})


  (def pl-timezone
    "Timezone for Warsaw, Poland."
    (time/time-zone-for-id "Europe/Warsaw"))

;; Opening hours logic.

  (defn start-all! [carousels]
    "Sends a start request for all carousels in `carousels` seq.
     Returns a map form carousel id to call success."
    (into {} (for [{:keys [id]} carousels]
               [id (api/start-carousel! id)])))

  (defn stop-all! [carousels]
    "Sends a stop request for all carousels in `carousels` seq.
     Returns a map form carousel id to call success."
    (into {} (for [{:keys [id]} carousels]
               [id (api/stop-carousel! id)])))

  (defn opening-hours [date]
    "Returns a clj-time interval of opening hours for a given date."
    (let [at-midnight          (time/floor date time/day)
          [opens-at closes-at] (cond
                                 (time-predicates/weekday? date) (:weekday opening-hours-mapping)
                                 (time-predicates/weekend? date) (:weekend opening-hours-mapping))]
      (time/interval (time/from-time-zone (time/plus at-midnight (time/hours opens-at)) pl-timezone)
                     (time/from-time-zone (time/plus at-midnight (time/hours closes-at)) pl-timezone))))

  (defn park-open?
    "Tests if the amusement park is open. If date is ommited, then current datetime is assumed."
    ([]     (park-open? (time/now)))
    ([date] (time/within? (opening-hours date) date)))

;; Park checking logic.

  (defn process-amusement-park-state! []
    "Processes the current state of the amusement park and acts accordingly."
    (let [{:keys [stopped running] :as carousels-by-state} (group-by :state (api/list-carousels))
          time (time/now)
          park-open? (park-open? time)
          carousels-modified (if park-open?
                               (start-all! stopped)
                               (stop-all! running))]
      (assoc {:check-time time
              :park-open? park-open?
              :carousels carousels-by-state}
        (if park-open? :started :stopped) carousels-modified)))