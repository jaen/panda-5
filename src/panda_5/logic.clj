(ns panda-5.logic
  (require [panda-5.persistence.events :as events]
           [panda-5.api.core :as api]

           [clj-time.core :as time]
           [clj-time.predicates :as time-predicates]
           [clojure.core.match :refer [match]]
           [cuerdas.core :as str]
           [taoensso.timbre :as log]
           [clj-uuid :as uuid]
           [schema.core :as s]))

;; Defines.

  (def OPENING-HOURS-MAPPING
    "A map that stores what hours the amusement park is open on given days.
     Hours are in Europe/Warsaw timezone"

    {:weekday [12 21]
     :weekend [10 23]})


  (def PL-TIMEZONE
    "Timezone for Warsaw, Poland."
    
    (time/time-zone-for-id "Europe/Warsaw"))

  (defonce ENVIRONMENT
    ; ""
    (atom :development))

  (def EnvName
    (s/enum :production :development))

;; Environment control

  (s/defn set-environment! [env :- EnvName]
    (condp = env
      :development  (api/mock-api! true)
      :production   (api/mock-api! false))
    (reset! ENVIRONMENT env))

;; Opening hours logic.

  (defn start-all!
    "Sends a start request for all carousels in `carousels` seq.
     Returns a map form carousel id to call success."
    [carousels]

    (into {} (for [{:keys [id]} carousels]
               [id (api/start-carousel! id)])))

  (defn stop-all!
    "Sends a stop request for all carousels in `carousels` seq.
     Returns a map form carousel id to call success."
    [carousels]

    (into {} (for [{:keys [id]} carousels]
               [id (api/stop-carousel! id)])))

  (defn opening-hours
    "Returns a clj-time interval of opening hours for a given date."
    [date]

    (let [at-midnight          (time/floor date time/day)
          [opens-at closes-at] (cond
                                 (time-predicates/weekday? date) (:weekday OPENING-HOURS-MAPPING)
                                 (time-predicates/weekend? date) (:weekend OPENING-HOURS-MAPPING))]
      (time/interval (time/from-time-zone (time/plus at-midnight (time/hours opens-at)) PL-TIMEZONE)
                     (time/from-time-zone (time/plus at-midnight (time/hours closes-at)) PL-TIMEZONE))))

  (defn park-open?
    "Tests if the amusement park is open. If date is ommited, then current datetime is assumed."
    ([]     (park-open? (time/now)))
    ([date] (time/within? (opening-hours date) date)))

;; Accident classification logic

  (defn contains-code? [description]
    (str/contains? description "CODE"))

  (defn classify-accident [{:keys [description people-dead]
                            inspection? :created-by-inspection
                            mechanic? :created-on-mechanical-overview
                            automated? :auto-created
                            :as accident}]
    (cond
      inspection?
        :stability

      (or mechanic? automated?)
        (if (contains-code? description)
          :mechanical
          :electric)

      (and (not-any? true? [inspection? mechanic? automated?])
           (empty? description)
           (> people-dead 0))
        :death))

  (defn schedule-in [{:keys [people-dead]}]
    (if (> people-dead 0)
      [(time/days 2) (time/minutes 1)]
      [(time/days 1) (time/minutes 1)]))

  (defn calculate-repair-date-time
    [{:keys [date-created] :as accident}]

    (time/latest
     (apply time/plus date-created (schedule-in accident))
     (time/plus (time/now) (time/minutes 1))))

  (defn repair-request-for-accident [{:keys [diagnostic-number carousel-id] :as accident}]
    (when-let [accident-type (classify-accident accident)]
      {:diagnostic-number diagnostic-number
       :type accident-type
       :carousel-id carousel-id
       :scheduled-time (calculate-repair-date-time accident)}))

;;

  (defn schedule-repair-request! [request]
    (api/repair-request request))

;; Park checking logic.

  (defn process-amusement-park-state!
    "Processes the current state of the amusement park and acts accordingly."
    []

    (match (api/list-carousels)
      (carousels :guard api/valid?)
        (let [{:keys [stopped running broken] :as carousels-by-state} (group-by :carousel-state carousels)
              time (time/now)
              park-open? (park-open? time)
              carousels-modified (if park-open?
                                   (start-all! stopped)
                                   (stop-all! running))
              repair-requests (for [{:keys [id]} broken
                                             :let [accidents (api/get-accidents-for-carousel id)
                                                   assoc-id  #(assoc % :carousel-id id)]]
                                         [id (map (comp repair-request-for-accident assoc-id) accidents)])
              unhandled-repair-requests (mapcat
                                          (fn [[_ requests]]
                                            (filter (fn [request]
                                                          (not (events/get-event {:type :repair-requested
                                                                                  :payload (select-keys request [:carousel-id :diagnostic-number])})))
                                                    requests))
                                          repair-requests)]
          (log/debug "Unhandled repair requests that would be scheduled: " (vec unhandled-repair-requests))
          (doseq [request unhandled-repair-requests]
            (log/debug "Handling repair request: " request)
            (if (schedule-repair-request! request)
              (do
                (events/create-event! {:uuid    (uuid/v1)
                                       :type    :repair-requested
                                       :payload request})
                (log/debug "Repair request scheduled."))
              (log/debug "Repair request schedule call falied.")))
          (assoc {:check-time      time
                  :park-open?      park-open?
                  :carousels       carousels-by-state
                  :repair-requests unhandled-repair-requests}
            (if park-open? :started :stopped) carousels-modified))

      error
        {:check-time (time/now)
         :error?     error}))