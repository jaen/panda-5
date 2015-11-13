(ns panda-5.core
  (:require [immutant.web :as web]
            [immutant.scheduling :as scheduling]
            [bidi.bidi :as bidi]
            [clj-http.client :as client]
            [clj-time.core :as time]
            [clj-time.predicates :as time-predicates]
            [environ.core :as environ])
  (:import [java.io StringWriter]))

(def log (atom (sorted-map)))

(def api-key "7c256dd0-4768-4f38-9ee9-b174ef417d53")

(def pl-timezone (time/time-zone-for-id "Europe/Warsaw"))

(defn local-now []
  (time/to-time-zone (time/now) pl-timezone))

(def base-path
  (str "https://ansi.lgbs.pl/api"))

(def api-routes
  ["/" [[[:api-key "/"] [["carousels/" [["findAll"    :carousels/list]
                                       [[:id]         :carousels/get]
                                       [["run/" :id]  :carousels/start]
                                       [["stop/" :id] :carousels/stop]]]]]
        ["teams/"       [[[:team-key] :teams/get]]]]])

(def state-mapping {"STOPPED"   :stopped
                    "RUNNING"   :running
                    "BROKEN"    :broken
                    "DESTROYED" :destroyed})

(defn api->carousel-state [state]
  (state-mapping state))

(defn api->carousel [api-repr]
  {:id    (long (:id api-repr))
   :name  (:name api-repr)
   :state (api->carousel-state (:carouselState api-repr))
   :price (long (:price api-repr))})

(defn api-path-for [key & {:as args}]
  (str base-path (apply bidi/path-for api-routes key (flatten (seq (assoc args :api-key api-key))))))

(def client-opts
  {:as :json
   :throw-exceptions false})

(defn api-list-carousels []
  (let [{:keys [status body]} (client/get (api-path-for :carousels/list) client-opts)]
    (when (= 200 status)
      (map api->carousel body))))

(defn api-get-carousel [id]
  (let [{:keys [status body]} (client/get (api-path-for :carousels/get :id id) client-opts)]
    (when (= 200 status)
      (api->carousel body))))

(defn api-start-carousel! [id]
  (let [{:keys [status]} (client/put (api-path-for :carousels/start :id id) client-opts)]
    (= status 200)))

(defn api-stop-carousel! [id]
  (let [{:keys [status]} (client/put (api-path-for :carousels/stop :id id) client-opts)]
    (= status 200)))

(defn api-team-info []
  (let [{:keys [status body]} (client/get (api-path-for :teams/get :team-key api-key) client-opts)]
    (when (= status 200)
      body)))

(defn start-all! [carousels]
  (into {} (for [{:keys [id]} carousels]
             [id (api-start-carousel! id)])))

(defn stop-all! [carousels]
  (into {} (for [{:keys [id]} carousels]
             [id (api-stop-carousel! id)])))

; hourse are in Europe/Warsaw timezone
(def opening-hours-mapping {:weekday [12 21]
                            :weekend [10 23]})

(defn opening-hours [date]
  (let [at-midnight          (time/floor date time/day)
        [opens-at closes-at] (cond
                               (time-predicates/weekday? date) (:weekday opening-hours-mapping)
                               (time-predicates/weekend? date) (:weekend opening-hours-mapping))]
    (time/interval (time/from-time-zone (time/plus at-midnight (time/hours opens-at)) pl-timezone)
                   (time/from-time-zone (time/plus at-midnight (time/hours closes-at)) pl-timezone))))

(defn park-open?
  ([]     (park-open? (time/now)))
  ([time] (time/within? (opening-hours time) time)))

(defn process-amusement-park! []
  (let [{:keys [stopped running] :as carousels-by-state} (group-by :state (api-list-carousels))
        time (time/now)
        park-open? (park-open? time)
        carousels-modified (if park-open?
                         (start-all! stopped)
                         (stop-all! running))]

    (swap! log assoc time
               (assoc {:park-open? park-open?
                       :carousels carousels-by-state}
                 (if park-open? :started :stopped) carousels-modified))))

(def check-amusement-park-job (atom nil))

(def job-interval {:every [30 :seconds]})

(def web-server (atom nil))

(defn handler [request]
  (let [last-status (last @log)
        body (let [w (StringWriter.)] (clojure.pprint/pprint last-status w) (.toString w))]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body body}))

(defn start! []
  (let [port (or (some-> environ/env :port Integer.)
                 8080)]
    (reset! check-amusement-park-job (scheduling/schedule process-amusement-park! job-interval))
    (reset! web-server (web/run handler {:host "0.0.0.0" :port port}))))

(defn stop! []
  (when (scheduling/stop @check-amusement-park-job)
    (reset! check-amusement-park-job nil))
  (when (web/stop @web-server)
    (reset! web-server nil)))

(defn -main []
  (start!))