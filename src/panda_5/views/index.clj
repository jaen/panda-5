(ns panda-5.views.index
  (require [hiccup.core :as hiccup]
           [clj-time.core :as time]
           [clj-time.format :as time-format]
           [cuerdas.core :as str]))

;; Defines.

  (def pl-timezone
    "Timezone for Warsaw, Poland."

    (time/time-zone-for-id "Europe/Warsaw"))

  (def check-time-formatter
    "Formats the datetime for check time."

    (time-format/with-zone (time-format/formatter "HH:mm:ss dd.MM.YYYY") pl-timezone))

  (def check-time-as-id-formatter
    "Formats the datetime for check time."

    (time-format/with-zone (time-format/formatter "YYYY-MM-DD-'at'-HH-mm-ss") pl-timezone))

;; Clojure, y u no zip : |

  (defn zip
    "Zips collections."
    [& colls]

    (partition (count colls) (apply interleave colls)))

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
                "No carousels are currently " state "."]])]]))

  (defn display-error? [error]
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
            "An unknown error occured.")]])

  (defn check-results
    [collapsible-id carousels]

    (cond
      (seq? carousels)
        (for [[state colour]  (zip [:running :stopped :broken :destroyed]
                                   ["success" "info" "warning" "danger"])]
          (check-results-for state colour collapsible-id (get carousels state)))
      :else
        (display-error? carousels)))

  (defn index
    "Generates HTML for the index view."
    [{:keys [check-time park-open? historical-updates team-info] fresh-carousels-by-state :carousels}]

    (let []
      (hiccup/html
        [:html
          [:head
            [:link {:rel "stylesheet"
                    :type "text/css"
                    :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"}]
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
                      :src "https://code.jquery.com/jquery-2.1.4.min.js"}]
            [:script {:type "text/javascript"
                      :src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"}]]
            [:body
              [:div.container
                [:h1
                  "Team Panda-5 app status    "
                  [:span.small.text-muted
                    "Last state update at: " (time-format/unparse check-time-formatter check-time)]]
                [:hr]
                  [:h2.text-center
                    "The amusement park is now " (if (nil? park-open?)
                                                   [:b.text-muted "UNKNOWN"]
                                                   (if park-open?
                                                     [:b.text-success "OPEN"]
                                                     [:b.text-danger "CLOSED"])) "."]
                  [:h2.text-center
                    "Current balance: " (if-let [points (:points team-info)]
                                          (str "$" points)
                                          [:b.text-muted "UNKNOWN"]) "."]
                [:hr]
                [:div
                  [:h2 "Fresh check results:"]
                  (check-results "fresh" @fresh-carousels-by-state)]
                [:hr]
                [:div
                  [:h2 "Last 42 historical check results:"]
                  (for [{:keys [check-time park-open? error?] carousels-by-state :carousels} (take 42 (reverse (sort-by :check-time historical-updates)))
                        :let [collapsible-id (str "collapse-historical-check-results-" (time-format/unparse check-time-as-id-formatter check-time))]]
                    [:div
                      [:h3.collapsed {:data-toggle "collapse" :data-target (str "#" collapsible-id) :style "cursor: pointer;"}
                        [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
                        " "
                        (time-format/unparse check-time-formatter check-time)
                        [:span.small
                          [:span.text-muted " ("]
                          (if error?
                            [:b.text-muted "UNKNOWN"]
                            (if park-open?
                              [:b.text-success "OPEN"]
                              [:b.text-danger "CLOSED"]))
                          [:span.text-muted ")"]]]
                      [:div.historical-check-results.indented.collapse {:id collapsible-id}
                        (check-results collapsible-id (or carousels-by-state error?))]])]]]])))
