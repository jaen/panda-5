(ns panda-5.api
  (:require [bidi.bidi :as bidi]
            [clj-http.client :as client]
            [clojure.core.match :refer [match]])
  (:import [java.net SocketTimeoutException]))

;; Defines

  (def API-KEY
    "The API access key."
    
    "7c256dd0-4768-4f38-9ee9-b174ef417d53")

  (def TIMEOUT
    "Timeout for API calls (in milliseconds)."

    1000)

  (def client-opts
    "Common clj-http options."

    {:as               :json
     :throw-exceptions false
     :conn-timeout     TIMEOUT
     :socket-timeout   TIMEOUT})

;; API path related functionality

  (def BASE-PATH
    "Base path for API endpoints."

    "https://ansi.lgbs.pl/api")

  (def API-ROUTES
    "A datastructure representing currently known API endpoints routes in bidi format."

    ["/" [[[:api-key "/"] [["carousels/" [["findAll"    :carousels/list]
                                          [[:id]         :carousels/get]
                                          [["run/" :id]  :carousels/start]
                                          [["stop/" :id] :carousels/stop]]]]]
          ["teams/"       [[[:team-key] :teams/get]]]]])

  (defn api-path-for
    "Returns an API path for given route symbol and arguments."
    [key & {:as args}]

    (str BASE-PATH (apply bidi/path-for API-ROUTES key (flatten (seq (assoc args :api-key API-KEY))))))

;; API data mapping

  (def STATE-MAPPING
    "Maps carousel state string enum to keywords."

    {"STOPPED"   :stopped
     "RUNNING"   :running
     "BROKEN"    :broken
     "DESTROYED" :destroyed})

  (defn api->carousel-state
    "Return a carousel state symbol for given string."
    [state]

    (get STATE-MAPPING state))

  (defn api->carousel
    "Returns the internal representation of the carousel for given external representation."
    [api-repr]

    {:id    (long (:id api-repr))
     :name  (:name api-repr)
     :state (api->carousel-state (:carouselState api-repr))
     :price (long (:price api-repr))})

;; HTTP wrappers turning timeout exception into a false return

  (defn api-get [path-args opts]
    (try
      (let [path     (apply api-path-for path-args)
            response (client/get path (merge client-opts opts))]
        [(:status response) response])

      (catch SocketTimeoutException e
        [false :timeout])))

  (defn api-put [path-args opts]
    (try
      (let [path     (apply api-path-for path-args)
            response (client/put path (merge client-opts opts))]
        [(:status response) response])

      (catch SocketTimeoutException e
        [false :timeout])))

;; API data fetching functionality helpers

  (defn success? [code]
    (and integer? code
         (or (>= code 200)
             (<  code 300))))

  (defn request-error? [code]
    (and integer? code
         (or (>= code 200)
             (<  code 300))))

  (defn server-error? [code]
    (and integer? code
         (or (>= code 200)
             (<  code 300))))

  (defn valid?
    "Test whether response is valid."
    [response]

    (not (keyword? response)))

  (defn process-response [[code {:keys [body] :as response}] & [{:keys [process-fn] :as options}]]
    (cond
      (success? code)               (if (and (valid? response) process-fn)
                                      (process-fn body)
                                      body)
      (request-error? code)         :request-error
      (server-error? code)          :server-error?
      (= response [false :timeout]) :timeout
      :else                         :unknown-error))

;; API data fetching functionality calls

  (defn list-carousels 
    "Calls the amusement park API to list info about all the carousels."
    []

    (process-response (api-get [:carousels/list] client-opts) {:process-fn (partial map api->carousel)}))

  (defn get-carousel 
    "Calls the amusement park API to get info about a single carousel."
    [id]

    (process-response (api-get [:carousels/get :id id] client-opts) {:process-fn api->carousel}))

  (defn start-carousel!
    "Calls the amusement park API to start a single carousel by given `id`.
     Observe how PUT instead of POST is used."
    [id]

    (process-response (api-put [:carousels/start :id id] client-opts)))

  (defn stop-carousel!
    "Calls the amusement park API to stop a single carousel by given `id`.
     Observe how PUT instead of POST is used."
    [id]

    (process-response (api-put [:carousels/stop :id id] client-opts)))

  (defn team-info 
    "Calls the amusement park API to get team info."
    []

    (process-response (api-get [:teams/get :team-key API-KEY] client-opts)))
