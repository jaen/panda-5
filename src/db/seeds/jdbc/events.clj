(ns db.seeds.jdbc.events
  (:require [jdbc.core :as jdbc]
            [clj-uuid :as uuid]
            [panda-5.persistence.events :as events]
            [clj-time.format :as time-format]
            [taoensso.timbre :as log]))

(defn parse-time [time-str]
  (time-format/parse (time-format/formatters :date-time) time-str))

(def handled-events
  [{:diagnostic-number 400729,
   :type :stability,
   :carousel-id 97,
   :scheduled-time (parse-time "2015-11-20T13:13:58.855+01:00")}
  {:diagnostic-number 290667,
   :type :electric,
   :carousel-id 119,
   :scheduled-time (parse-time "2015-11-20T13:13:58.855+01:00")}
  {:diagnostic-number 989081,
   :type :stability,
   :carousel-id 113,
   :scheduled-time (parse-time "2015-11-21T00:01:00.000+01:00")}
  {:diagnostic-number 577744,
   :type :stability,
   :carousel-id 99,
   :scheduled-time (parse-time "2015-11-21T00:01:00.000+01:00")}
  {:diagnostic-number 962611,
   :type :mechanical,
   :carousel-id 116,
   :scheduled-time (parse-time "2015-11-21T00:01:00.000+01:00")}
  {:diagnostic-number 508674,
   :type :mechanical,
   :carousel-id 91,
   :scheduled-time (parse-time "2015-11-22T00:01:00.000+01:00")}
  {:diagnostic-number 280720,
   :type :mechanical,
   :carousel-id 109,
   :scheduled-time (parse-time "2015-11-22T00:01:00.000+01:00")}
   {:diagnostic-number 280720
    :type :mechanical
    :carousel-id 109
    :scheduled-time (parse-time "2015-11-21T00:01:00.000+01:00")}])

(defn run [target & args]
  (let [db-spec (get-in target [:db :url])]
    (println target)
    (println args)
    (doseq [{:keys [diagnostic-number carousel-id]
             :as payload} handled-events]
      (let [event {:uuid (uuid/v1)
                   :type :repair-requested
                   :payload payload}]
        (if-not (events/get-event {:type    :repair-requested
                                   :payload {:diagnostic-number diagnostic-number
                                             :carousel-id carousel-id}})
          (do
            (events/create-event! event)
            (log/info "Event " (:diagnostic-number event) " created."))
          (log/info "Event " (:diagnostic-number event) " already exists."))))))