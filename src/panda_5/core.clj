(ns panda-5.core
  (:require [panda-5.logic :as logic]
            [panda-5.views.index :as index-view]
            [panda-5.api.core :as api]
            [panda-5.persistence.migrations :as migrations]
            [panda-5.persistence.core :as persistence]

            [immutant.web :as web]
            [immutant.scheduling :as scheduling]
            [environ.core :as environ]
            [clj-time.core :as time]
            [cljs.core.match :refer [match]]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.core :as appenders]
            [schema.core :as s]))

;; Defines.

  (defonce log
    ; "Keeps track of what happens on scheduled amusement park state updates."

    (atom (sorted-map)))

  (defonce check-amusement-park-job-handle
    ; "Atom which holds the handle for the park state checking."

    (atom nil))

  (def JOB-INTERVAL
    "Park state checking job update interval."

    {:every [10 :seconds] #_[3 :minutes]})

  (defonce web-server-handle
    ; "Atom which holds the handle for the web server."

    (atom nil))

;; Global setup

  (log/merge-config!
    {:appenders {:println {:enabled? false}
                 :spit (appenders/spit-appender {:fname "./logs/timbre.log"})}})

  (s/set-fn-validation! true)

;; Domain logic

  (defn handler
    "Simple Ring handler that shows the last check status value."
    [request]

    (if (= (:path-info request) "/")
      (let [start-time (time/now)
            [_ last-status] (last @log)
            team-info (api/team-info)
            fresh-carousels (future (let [carousels (api/list-carousels)]
                                      (if (api/valid? carousels)
                                        (group-by :carousel-state carousels)
                                        carousels)))
            body (index-view/index (assoc last-status
                                     :park-open? (:park-open? last-status)
                                     :check-time (:check-time last-status)
                                     :carousels fresh-carousels
                                     :historical-updates (vals @log)
                                     :team-info team-info))]
        {:status  200
         :headers {"Content-Type"   "text/html"
                   "X-Generated-In" (time/in-millis (time/interval start-time (time/now)))}
         :body    body})
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body "Go away."}))

  (defn check-amusement-park-job
    "The handler for the scheduled job of checking the amusement park state.
     After the state is checked it updates the log with check results."
    []

    (let [{:keys [check-time] :as check-result} (logic/process-amusement-park-state!)]
      (swap! log assoc check-time check-result)))

  (defn start!
    "Starts the application."
    []

    (let [env  (or (some-> environ/env :env keyword)
                   :development)
          port (or (some-> environ/env :port Integer.)
                   8080)
          db-host (or (:db-host environ/env)
                      "localhost")
          db-port (or (some-> environ/env :db-port Integer.)
                      5432)
          db-database (or (:db-database environ/env)
                          "panda5_dev")
          db-user (or (:db-user environ/env)
                      "panda5")
          db-password (or (:db-password environ/env)
                          "panda5")
          db-params {:host db-host
                     :port-number db-port
                     :database db-database
                     :username db-user
                     :password db-password}]
      (log/info "Starting the application with: " {:env env :port port :db db-params})
      (logic/set-environment! env)
      (persistence/set-data-source!
        (persistence/make-hikari-data-source db-params))
      (migrations/ensure-database!)
      (reset! check-amusement-park-job-handle (scheduling/schedule check-amusement-park-job JOB-INTERVAL))
      (reset! web-server-handle (web/run handler {:host "0.0.0.0" :port port}))))

  (defn stop!
    "Stops the application."
    []

    (when (scheduling/stop @check-amusement-park-job-handle)
      (reset! check-amusement-park-job-handle nil))
    (when (web/stop @web-server-handle)
      (reset! web-server-handle nil)))

  (defn -main
    "The main function."
    []

    (start!))