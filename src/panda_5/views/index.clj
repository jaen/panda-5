(ns panda-5.views.index
  (require [hiccup.core :as hiccup]
           [clj-time.core :as time]
           [clj-time.format :as time-format]
           [cuerdas.core :as str]
           [immutant.caching :as cache]
           [panda-5.utils.core :refer [zip]]
           [panda-5.utils.time :as time-utils]
           [taoensso.timbre :as log]))

;; Defines.

  (def cache
    (cache/with-codec (cache/cache "panda5.views.index-cache" :max-entries 3) :transit-msgpack))

;; Caching helper

  (defn- cached-impl [key value-fn]
    (if-let [cached-value (get cache key)]
      (do
        (log/debug "Got cached value for " key)
        cached-value)
      (let [value (value-fn)]
        (log/debug "Caching value for " key)
        (.put cache key value)
        value)))

  (defmacro cached [key & body]
    `(cached-impl ~key (fn [] ~@body)))

;; Index view.

  (defn carousel-info-row
    "Generates HTML for a single row of carousel table."
    [{:keys [id name state price]}]

    (hiccup/html
      [:tr
        [:td {:style "width:150px;"}
          [:span.text-muted "#" id]]
        [:td
          name]
        [:td {:style "width:100px;"}
          [:span.small.pull-right "$" price]]]))

  (defn check-results-for
    "Generates HTML for check results for a single carousel state."
    [state colour collapsible-id carousels]

    (hiccup/html
      (let [state (name state)
            collapsible-id (str collapsible-id "-" state)]
        [:div.check-results {:class state}
          [:h4 {:data-toggle "collapse" :data-target (str "#" collapsible-id) :style "cursor: pointer;"}
            [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
            [:span {:class (str "text-" colour)}
              " " (str/titleize state) " "]
            [:span.small
              "(" (count carousels) ")"]]
          [:div.indented.collapse.in {:id collapsible-id}
            (if (not-empty carousels)
              [:table.table.table-striped.table-hover
                [:tbody
                  (for [carousel carousels]
                    (carousel-info-row carousel))]]
              [:div.jumbotron.text-center {:style "background:none;"}
                [:h4.text-muted
                  "No carousels are currently " state "."]])]])))

  (defn display-error? [error]
    (hiccup/html
      [:div.jumbotron.text-center {:style "background:none;"}
        [:h4.text-muted
          (condp = error
            :request-error
              "A request error occured."
            :server-error
              "A server error occured."
            :timeout
              "Amusement park didn't reply in time."
            :unknown-error
              "An unknown error occured.")]]))

  (defn check-results
    [collapsible-id carousels]

    (cond
      (map? carousels)
        (for [[state colour]  (zip [:running :stopped :broken :destroyed]
                                   ["success" "info" "warning" "danger"])]
          (check-results-for state colour collapsible-id (get carousels state)))
      :else
        (display-error? carousels)))

  (defn historical-checks [checks]
    (hiccup/html
     [:h2 "Last 42 historical check results:"]
     (for [{:keys [check-time park-open? error? repair-requests] carousels-by-state :carousels} checks
           :let [collapsible-id (str "collapse-historical-check-results-" (time-utils/to-check-time-as-id-str check-time))]]
       [:div
         [:h3.collapsed {:data-toggle "collapse" :data-target (str "#" collapsible-id) :style "cursor: pointer;"}
           [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
           " "
           (time-utils/to-check-time-str check-time)
           [:span.small
             [:span.text-muted " ("]
               (if error?
                 [:b.text-muted "UNKNOWN"]
                 (if park-open?
                   [:b.text-success "OPEN"]
                   [:b.text-danger "CLOSED"]))
             [:span.text-muted ")"]]]
         [:div.historical-check-results.indented.collapse {:id collapsible-id}
           (check-results collapsible-id (or carousels-by-state error?))
           (when-not (empty? repair-requests)
             (let [repair-requests-collapsible-id (str collapsible-id "-repair-requests")]
               [:div.repair-requests
                 [:h4 {:data-toggle "collapse" :data-target (str "#" repair-requests-collapsible-id) :style "cursor: pointer;"}
                   [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
                  " Repair requests issued:"]
                 [:div.repair-requests-list.indented.collapse.in {:id repair-requests-collapsible-id}
                   [:table.table.table-striped.table-hover
                     [:thead
                       [:th "Carousel id"]
                       [:th "Diagnostic number"]
                       [:th "Type"]
                       [:th "Scheduled time"]]
                     [:tbody
                       (for [{:keys [carousel-id diagnostic-number type scheduled-time]} repair-requests]
                         [:tr
                           [:td carousel-id]
                           [:td diagnostic-number]
                           [:td (name type)]
                           [:td (time-utils/to-check-time-str scheduled-time)]])]]]]))]])))

  (defn index
    "Generates HTML for the index view."
    [{:keys [check-time park-open? historical-updates team-info] fresh-carousels-by-state :carousels}]

    (let []
      (hiccup/html
        [:html
          [:head
            [:link {:rel "stylesheet"
                    :type "text/css"
                    :href "/assets/stylesheets/bootstrap.css"}]
            [:style ".collapsed .glyphicon.glyphicon-menu-right:before {
                       content: \"\\e259\";
                      }

                     .indented {
                       padding: 10px 20px;
                       font-size: 17.5px;
                       border-left: 5px solid #eee;
                       margin-left: 1.2em;
                      }"]
            [:script {:type "text/javascript"
                      :src "/assets/javascripts/jquery.js"}]
            [:script {:type "text/javascript"
                      :src "/assets/javascripts/bootstrap.js"}]]
            [:body
              [:div.container
                [:h1
                  "Team Panda-5 app status    "
                  [:span.small.text-muted
                    "Last state update at: " (time-utils/to-check-time-str check-time)]]
                [:hr]
                  [:h2.text-center
                    "The amusement park is now " (if (nil? park-open?)
                                                   [:b.text-muted "UNKNOWN"]
                                                   (if park-open?
                                                     [:b.text-success "OPEN"]
                                                     [:b.text-danger "CLOSED"])) "."]
                  [:h2.text-center
                    "Current balance: " (if-let [points (:points @team-info)]
                                          (str "$" points)
                                          [:b.text-muted "UNKNOWN"]) "."]
                [:hr]
                [:div
                  [:h2 "Fresh check results:"]
                  (check-results "fresh" @fresh-carousels-by-state)]
                [:hr]
                [:div
                  (let [checks (take 42 (reverse (sort-by :check-time historical-updates)))
                        cache-key (time-utils/to-str (:check-time (first checks)))]
                    (cached cache-key
                      (historical-checks checks)))]]]])))
