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
    [state colour carousels]

    (let [state (name state)
          collapsible-id (str "#collapse-" state)]
      [:div.check-results {:class state}
        [:h3 {:data-toggle "collapse" :data-target collapsible-id :style "cursor: pointer;"}
          [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
          [:span {:class (str "text-" colour)}
            " " (str/titleize state) " "]
          [:span.small
            "(" (count carousels) ")"]]
        [:div.collapse.in {:id collapsible-id}
          (if (not-empty carousels)
            [:table.table.table-striped.table-hover
              [:tbody
                (for [carousel carousels]
                  (carousel-info-row carousel))]]
            [:div.jumbotron.text-center {:style "background:none;"}
              [:h4.text-muted
                "No carousels are currently " state "."]])]]))

  (defn index
    "Generates HTML for the index view."
    [{:keys [check-time park-open?] carousels-by-state :carousels}]

    (let []
      (hiccup/html
        [:html
          [:head
            [:link {:rel "stylesheet"
                    :type "text/css"
                    :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"}]
            [:style ".collapsed .glyphicon.glyphicon-menu-right:before { content: \"\\e259\"; }"]
            [:script {:type "text/javascript"
                      :src "https://code.jquery.com/jquery-2.1.4.min.js"}]
            [:script {:type "text/javascript"
                      :src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"}]]
            [:body
              [:div.container
                [:h1
                  "Team Panda-5 app status    "
                  [:span.small.text-muted
                    "Last checked at: " (time-format/unparse check-time-formatter check-time)]]
                [:hr]
                  [:h2.text-center
                    "The amusement park is now " (if park-open?
                                                   [:b.text-success "OPEN"]
                                                   [:b.text-danger "CLOSED"]) "."]
                [:hr]
                [:div
                  [:h2 "Last check results were:"]
                  (for [[state colour]  (zip [:running :stopped :broken :destroyed]
                                             ["success" "info" "warning" "danger"])]
                    (check-results-for state colour (get carousels-by-state state)))]]]])))