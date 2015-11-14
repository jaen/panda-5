(ns panda-5.api
  (:require [bidi.bidi :as bidi]
            [clj-http.client :as client]))

;; Defines

  (def api-key
    "The API access key."
    
    "7c256dd0-4768-4f38-9ee9-b174ef417d53")

  (def client-opts
    "Common clj-http options."

    {:as :json
     :throw-exceptions false})

;; API path related functionality

  (def base-path
    "Base path for API endpoints."

    "https://ansi.lgbs.pl/api")

  (def api-routes
    "A datastructure representing currently known API endpoints routes in bidi format."

    ["/" [[[:api-key "/"] [["carousels/" [["findAll"    :carousels/list]
                                          [[:id]         :carousels/get]
                                          [["run/" :id]  :carousels/start]
                                          [["stop/" :id] :carousels/stop]]]]]
          ["teams/"       [[[:team-key] :teams/get]]]]])

  (defn api-path-for
    "Returns an API path for given route symbol and arguments."
    [key & {:as args}]

    (str base-path (apply bidi/path-for api-routes key (flatten (seq (assoc args :api-key api-key))))))

;; API data mapping

  (def state-mapping
    "Maps carousel state string enum to keywords."

    {"STOPPED"   :stopped
     "RUNNING"   :running
     "BROKEN"    :broken
     "DESTROYED" :destroyed})

  (defn api->carousel-state
    "Return a carousel state symbol for given string."
    [state]

    (state-mapping state))

  (defn api->carousel
    "Returns the internal representation of the carousel for given external representation."
    [api-repr]

    {:id    (long (:id api-repr))
     :name  (:name api-repr)
     :state (api->carousel-state (:carouselState api-repr))
     :price (long (:price api-repr))})

;; API data fetching functionality

  (defn list-carousels 
    "Calls the amusement park API to list info about all the carousels."
    []

    (let [{:keys [status body]} (client/get (api-path-for :carousels/list) client-opts)]
      (when (= 200 status)
        (map api->carousel body))))

  (defn get-carousel 
    "Calls the amusement park API to get info about a single carousel."
    [id]

    (let [{:keys [status body]} (client/get (api-path-for :carousels/get :id id) client-opts)]
      (when (= 200 status)
        (api->carousel body))))

  (defn start-carousel!
    "Calls the amusement park API to start a single carousel by given `id`.
     Observe how PUT instead of POST is used."
    [id]

    (let [{:keys [status]} (client/put (api-path-for :carousels/start :id id) client-opts)]
      (= status 200)))

  (defn stop-carousel!
    "Calls the amusement park API to stop a single carousel by given `id`.
     Observe how PUT instead of POST is used."
    [id]

    (let [{:keys [status]} (client/put (api-path-for :carousels/stop :id id) client-opts)]
      (= status 200)))

  (defn team-info 
    "Calls the amusement park API to get team info."
    []

    (let [{:keys [status body]} (client/get (api-path-for :teams/get :team-key api-key) client-opts)]
      (when (= status 200)
        body)))
