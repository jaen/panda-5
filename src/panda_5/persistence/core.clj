(ns panda-5.persistence.core
  (:require [panda-5.persistence.postgresql-extensions]
            [panda-5.api.coercion :as api-coercion]
            [panda-5.api.core :as api]

            [hikari-cp.core :as hikari]
            [jdbc.core :as jdbc]
            [clj-time.format :as time-format])
  (:import [org.joda.time DateTime]))

(defn make-hikari-data-source [{:keys [host port database username password]}]
  (hikari/make-datasource
   {:connection-timeout 30000
    :idle-timeout 600000
    :max-lifetime 1800000
    :minimum-idle 10
    :maximum-pool-size  10
    :adapter "postgresql"
    :username username
    :password password
    :database-name database
    :server-name host
    :port-number port}))

(defonce data-source (atom nil))

(defn set-data-source! [new-val]
  (reset! data-source new-val))

(defn get-data-source []
  @data-source)

(defn get-connection []
  (jdbc/connection @data-source))

(defn db-init-dev! []
  (set-data-source! (make-hikari-data-source {:host "localhost"
                                              :port-number 5432
                                              :database "panda5_dev"
                                              :username "panda5"
                                              :password "panda5"})))

(defmacro with-data-source [data-source & body]
  `(let [old-data-source (get-data-source)]
     (set-data-source! data-source)
     ~@body
     (set-data-source! old-data-source)))

(def db-date-time-formatter
  (time-format/formatters :date-time))

(defn date-time->db-json [datetime]
  (time-format/unparse db-date-time-formatter datetime))

(defn db-json->date-time [datetime-str]
  (time-format/parse db-date-time-formatter datetime-str))


(defn db->map [what schema & [options]]
  (let [coercer (api-coercion/api->domain-coercer
                 schema
                 {:matcher (merge {DateTime db-json->date-time}
                                  (:matcher options {}))})]
    (-> what
        (api-coercion/transform-keys api/api->keyword-key)
        (coercer))))

(defn map->db [what schema & [options]]
  (let [coercer (api-coercion/domain->api-coercer
                 schema
                 {:matcher (merge {DateTime date-time->db-json}
                                  (:matcher options {}))})]
    (-> what
        (coercer)
        (api-coercion/transform-keys api/keyword->api-key))))

