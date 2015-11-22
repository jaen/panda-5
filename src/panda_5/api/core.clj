(ns panda-5.api.core
  (:require [panda-5.api.coercion :as coercion]
            [panda-5.api.schemas :as schemas]

            [bidi.bidi :as bidi]
            [clj-http.client :as client]
            [clojure.core.match :refer [match]]
            [cuerdas.core :as str]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.timbre :as log])
  (:import [java.net SocketTimeoutException]))

; Defines

  (def API-KEY
    "The API access key."

    "7c256dd0-4768-4f38-9ee9-b174ef417d53")

  (def TIMEOUT
    "Timeout for API calls (in milliseconds)."

    1000)

  (def CLIENT-OPTS
    "Common clj-http options."

    {:as               :json-string-keys
     :throw-exceptions false
     :conn-timeout     TIMEOUT
     :socket-timeout   TIMEOUT})

  (defonce MOCK-API?
    ; "Should we mock the API?"
    (atom true))

;; Destructive APIs mocking for development

  (defn mock-api! [value]
    (reset! MOCK-API? value))

;; API path related functionality

  (def BASE-PATH
    "Base path for API endpoints."

    "https://ansi.lgbs.pl/api")

  (def API-ROUTES
    "A datastructure representing currently known API endpoints routes in bidi format."

    ["/" [[[:api-key "/"] [["carousels/" [["findAll"         :carousels/list]
                                          [[:id]             :carousels/get]
                                          [["run/" :id]      :carousels/start]
                                          [["stop/" :id]     :carousels/stop]]]
                           ["accidents/" [[["id/" :id]       :accidents/get]
                                          [["carousel/" :id] :accidents/list-for-carousel]
                                          ["schedule/"       :accidents/schedule-repair]]]]]
          ["teams/"       [[[:team-key] :teams/get]]]]])

  (defn api-path-for
    "Returns an API path for given route symbol and arguments."
    [key & {:as args}]

    (str BASE-PATH (apply bidi/path-for API-ROUTES key (flatten (seq (assoc args :api-key API-KEY))))))

;; Fetching and mapping

  (defn api->keyword-key [[key value]]
    [(keyword (str/dasherize (name key))) value])

  (defn keyword->api-key [[key value]]
    [(str/camelize (name key)) value])

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

  (defn process-response [[code {:keys [body] :as response}]]
    (cond
      (success? code)               [true response]
      (request-error? code)         [false :request-error]
      (server-error? code)          [false :server-error?]
      (= response [false :timeout]) [false :timeout]
      :else                         [false :unknown-error]))


;; HTTP wrappers turning timeout exception into a false return and validating

  (def METHOD-MAP
    {:get client/get
     :put client/put})

  (defn- api-call-internal [method path opts]
    (try
      (let [client-fn (get METHOD-MAP method)
            response  (client-fn path opts)]
        [(:status response) response])

      (catch SocketTimeoutException e
        [false :timeout])))

  (defn api-call
    "Calls the API"

    ([method path-args & [opts]]
      (let [opts                        (or opts {})
            path                        (apply api-path-for path-args)
            {request-schema  :request
             response-schema :response} (:schema opts)
            request-coercer  (if request-schema
                               (fn [request]
                                 (let [coercer (coercion/domain->api-coercer request-schema)]
                                   (-> request
                                       (coercer)
                                       (coercion/transform-keys keyword->api-key))))
                               (fn [request]
                                 (coercion/transform-keys request keyword->api-key)))
            response-coercer (if response-schema
                               (fn [response]
                                 (let [coercer (coercion/api->domain-coercer response-schema)]
                                   (-> response
                                       (coercion/transform-keys api->keyword-key)
                                       (coercer))))
                               (fn [response]
                                 (coercion/transform-keys response api->keyword-key)))
            transformed-body            (when-let [body (:form-params opts)]
                                          (let [coerced-body (request-coercer body)]
                                            (if (coercion/valid? coerced-body)
                                              coerced-body
                                              (throw coerced-body))))
            effective-opts              (as-> opts $
                                          (merge CLIENT-OPTS $)
                                          (if transformed-body
                                            (assoc $ :form-params transformed-body)
                                            $))]

        (if (and @MOCK-API? (= method :put))
          (do
            (log/debug "Made a mock request: " {:method method
                                                :path path
                                                :opts effective-opts})
            [true {:mocked true}]) ; mock only puts for now
          (let [[status response] (api-call-internal method path effective-opts)
                processed-response (when-let [body (:body response)]
                                     (let [coerced-body (response-coercer body)]
                                       (if (coercion/valid? coerced-body)
                                         (assoc response :body coerced-body)
                                         (throw+ coerced-body))))]
            (process-response [status processed-response]))))))

;; ===

  (defn list-carousels
    "Calls the amusement park API to list info about all the carousels."
    []

    (match (api-call :get [:carousels/list]
                     {:schema {:response schemas/CarouselList}})
      [true {:body body}] body
      [false error]       error))

  (defn get-carousel
    "Calls the amusement park API to get info about a single carousel."
    [id]

    (match (api-call :get [:carousels/get :id id]
                     {:schema {:response schemas/Carousel}})
      [true {:body body}] body
      [false error]       error))

  (defn start-carousel!
    "Calls the amusement park API to start a single carousel by given `id`.
     Observe how PUT instead of POST is used."
    [id]

    (match (api-call :put [:carousels/start :id id])
      [true _]      true
      [false error] error))

  (defn stop-carousel!
    "Calls the amusement park API to stop a single carousel by given `id`.
     Observe how PUT instead of POST is used."
    [id]

    (match (api-call :put [:carousels/stop :id id])
      [true _]      true
      [false error] error))

  (defn get-accident
    [id]

    (match (api-call :get [:accidents/get :id id]
                     {:schema {:response schemas/Accident}})
      [true {:body body}] body
      [false error]       error))

  (defn get-accidents-for-carousel
    [carousel-id]

    (match (api-call :get [:accidents/list-for-carousel :id carousel-id]
                     {:schema {:response schemas/AccidentList}})
      [true {:body body}] body
      [false error]       error))

  (defn repair-request
    [body]

    (match (api-call :put [:accidents/schedule-repair]
                     {:schema {:request schemas/RepairRequest}
                      :content-type :json
                      :form-params body})
           [true _]      true
           [false error] error))

  (defn team-info
    "Calls the amusement park API to get team info."
    []

    (match (api-call :get [:teams/get :team-key API-KEY]
              {:schema {:response schemas/TeamInfo}})
      [true {:body body}] body
      [false error]       error))