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
            [schema.core :as s]
            [ring.middleware.resource :as ring-resource]
            [ring.middleware.content-type :as ring-content-type]
            [com.palletops.log-config.timbre.tools-logging :as timbre-logging]
            )
  (:gen-class))

;; Defines.

  (defonce log
    ; "Keeps track of what happens on scheduled amusement park state updates."

    (atom (sorted-map)))

  (defonce check-amusement-park-job-handle
    ; "Atom which holds the handle for the park state checking."

    (atom nil))

  (def JOB-INTERVAL
    "Park state checking job update interval."

    {:every [2 :minutes]})

  (defonce web-server-handle
    ; "Atom which holds the handle for the web server."

    (atom nil))

  (defonce fresh-check-results
    ; ""

    (atom nil))

  (defonce fresh-team-info
    ; ""

    (atom nil))

;; Global setup

  (log/merge-config!
    {:appenders {:println {:enabled? false}
                 :spit (appenders/spit-appender {:fname "./logs/timbre.log"})
                 :jl (timbre-logging/make-tools-logging-appender {})
                 }})

  (s/set-fn-validation! true)

;; Application logic

  (defn make-carousel-check! []
    (let [carousels (api/list-carousels)]
      (reset! fresh-check-results
              (if (api/valid? carousels)
                (group-by :carousel-state carousels)
                carousels))))

  (defn make-team-check! []
    (let [team-info (api/team-info)]
      (reset! fresh-team-info team-info)))

  (defn index-handler
    "Simple Ring handler that shows the last check status value."
    [request]

    (if (= (:path-info request) "/")
      (let [start-time (time/now)
            _ (.start (Thread. make-carousel-check!))
            _ (.start (Thread. make-team-check!))
            [_ last-status] (last @log)
            body (index-view/index (assoc last-status
                                     :park-open? (:park-open? last-status)
                                     :check-time (:check-time last-status)
                                     :carousels fresh-check-results
                                     :historical-updates (vals @log)
                                     :team-info fresh-team-info))]
        {:status  200
         :headers {"Content-Type"   "text/html"
                   "X-Generated-In" (time/in-millis (time/interval start-time (time/now)))}
         :body    body})
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body "Go away."}))

  (def composed-handler
    (-> index-handler
        (ring-resource/wrap-resource "public")
        (ring-content-type/wrap-content-type)))

  (defn handler [request]
    (composed-handler request))

  (defn check-amusement-park-job
    "The handler for the scheduled job of checking the amusement park state.
     After the state is checked it updates the log with check results."
    []

    (let [{:keys [check-time] :as check-result} (logic/process-amusement-park-state!)]
      (swap! log assoc check-time check-result)))

  (defn start!
    "Starts the application."
    []

    (let [env  (or (some-> environ/env :panda5-env keyword)
                   :development)
          port (or (some-> environ/env :panda5-port Integer.)
                   8080)
          ssl-port (or (some-> environ/env :panda5-ssl-port Integer.)
                       8443)
          db-host (or (:panda5-db-host environ/env)
                      "localhost")
          db-port (or (some-> environ/env :panda5-db-port Integer.)
                      5432)
          db-database (or (:panda5-db-database environ/env)
                          "panda5_dev")
          db-user (or (:panda5-db-user environ/env)
                      "panda5")
          db-password (or (:panda5-db-password environ/env)
                          "panda5")
          db-params {:host db-host
                     :port db-port
                     :database db-database
                     :username db-user
                     :password db-password}
          mock-api? (when-let [value (:panda5-mock-requests environ/env)]
                      (= value "true"))
          immutant-params (when (= env :development)
                            {:host "0.0.0.0"
                             :port port
                             :ssl-port ssl-port
                             :http2? true
                             :keystore "certs/server.keystore"
                             :key-password "panda5"
                             :truststore "certs/server.truststore"
                             :trust-password "panda5"})]
      (log/info "Starting the application with: " {:env env
                                                   :port port
                                                   :db db-params
                                                   :mock-api? mock-api?})
      (logic/set-environment! env)
      (when mock-api?
        (api/mock-api! true))
      (persistence/set-data-source!
        (persistence/make-hikari-data-source db-params))
      (migrations/ensure-database!)
      (log/info "Pre-querying carousel status.")
      (make-carousel-check!)
      (log/info "Pre-querying team info.")
      (make-team-check!)
      (reset! check-amusement-park-job-handle (scheduling/schedule check-amusement-park-job (merge JOB-INTERVAL
                                                                                                   {:singleton true})))
      (reset! web-server-handle (condp = env
                                  :production (web/run handler (or immutant-params {}))
                                  (web/run-dmc handler immutant-params)))))

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
