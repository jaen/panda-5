(ns panda-5.views.index
  (require [hiccup.core :as hiccup]
           [clj-time.core :as time]
           [clj-time.format :as time-format]))

(defn carousel-info-row [{:keys [id name state price]}]
  (hiccup/html
    [:tr
      [:td {:style "width:150px;"}
        [:span.text-muted "#" id]]
      [:td
        name]
      [:td {:style "width:100px;"}
        [:span.small.pull-right "$" price]]]))

(defn index [check-result]
  (let [check-time (:check-time check-result)
        park-open? (:park-open? check-result)
        {:keys [running stopped broken destroyed]} (:carousels check-result)]
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
                  "Last checked at: " (time-format/unparse (time-format/formatter "HH:mm:ss dd MM YYYY") check-time)]]
              [:hr]
                [:h2.text-center
                "The amusement park is now " (if park-open?
                                               [:b.text-success "OPEN"]
                                               [:b.text-danger "CLOSED"]) "."]
              [:hr]
              [:div
                [:h2 "Last check results were:"]
                [:div
                  [:h3 {:data-toggle "collapse" :data-target "#collapse-running" :style "cursor: pointer;"}
                    [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
                    [:span.text-success " Running "]
                    [:span.small "(" (count running) ")"]]
                  [:div#collapse-running.collapse.in
                    (if (not-empty running)
                      [:table.table.table-striped.table-hover
                        [:tbody
                          (for [carousel running]
                            (carousel-info-row carousel))]]
                      [:div.jumbotron.text-center {:style "background:none;"}
                        [:h4.text-muted "No carousels are currently running."]])]]
                [:div
                  [:h3 {:data-toggle "collapse" :data-target "#collapse-stopped" :style "cursor: pointer;"}
                    [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
                    [:span.text-info " Stopped "]
                    [:span.small "(" (count stopped) ")"]]
                  [:div#collapse-stopped.collapse.in
                    (if (not-empty stopped)
                      [:table.table.table-striped.table-hover
                        [:tbody
                          (for [carousel stopped]
                            (carousel-info-row carousel))]]
                      [:div.jumbotron.text-center {:style "background:none;"}
                        [:h4.text-muted "No carousels are currently stopped."]])]]
                [:div
                  [:h3 {:data-toggle "collapse" :data-target "#collapse-broken" :style "cursor: pointer;"}
                    [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
                    [:span.text-warning " Broken "]
                    [:span.small "(" (count broken) ")"]]
                  [:div#collapse-broken.collapse.in
                    (if (not-empty broken)
                      [:table.table.table-striped.table-hover
                        [:tbody
                          (for [carousel broken]
                            (carousel-info-row carousel))]]
                      [:div.jumbotron.text-center {:style "background:none;"}
                        [:h4.text-muted "No carousels are currently broken."]])]]
                [:div
                  [:h3 {:data-toggle "collapse" :data-target "#collapse-destroyed" :style "cursor: pointer;"}
                    [:span.glyphicon.glyphicon-menu-right.text-muted {:style "font-size: 0.8em;"}]
                    [:span.text-danger " Destroyed "]
                    [:span.small "(" (count destroyed) ")"]]
                  [:div#collapse-destroyed.collapse.in
                    (if (not-empty destroyed)
                      [:table.table.table-striped.table-hover
                        [:tbody
                          (for [carousel destroyed]
                            (carousel-info-row carousel))]]
                      [:div.jumbotron.text-center {:style "background:none;"}
                        [:h4.text-muted "No carousels are currently destroyed."]])]]]]]])))