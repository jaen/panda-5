(ns panda-5.core
  (:require [immutant.web :as web]
            [immutant.scheduling :as scheduling]
            [environ.core :as environ]

            [panda-5.logic :as logic]
            [panda-5.views.index :as index-view]))

;; Defines.

  (defonce log
    ; "Keeps track of what happens on scheduled amusement park state updates."
    (atom (sorted-map)))

  (defonce check-amusement-park-job-handle
    ; "Atom which holds the handle for the park state checking."
    (atom nil))

  (def job-interval
    "Park state cheking job update interval."
    {:every [10 :seconds]})

  (defonce web-server-handle
    ; "Atom which holds the handle for the web server."
    (atom nil))

;; Domain logic

  (defn handler
    "Simple Ring handler that shows the last check status value."
    [request]

    (let [[_ last-status] (last @log)
          body (index-view/index last-status)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body body}))

  (defn check-amusement-park-job
    "The handler for the scheduled job of checking the amusement park state.
     After the state is checked it updates the log with check results."
    []

    (let [{:keys [check-time] :as check-result} (logic/process-amusement-park-state!)]
      (swap! log assoc check-time check-result)))

  (defn start!
    "Starts the application."
    []

    (let [port (or (some-> environ/env :port Integer.)
                   8080)]
      (reset! check-amusement-park-job-handle (scheduling/schedule check-amusement-park-job job-interval))
      (reset! web-server-handle (web/run-dmc handler {:host "0.0.0.0" :port port}))))

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