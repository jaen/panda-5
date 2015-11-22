(ns panda-5.persistence.events
  (:require [panda-5.persistence.schemas :refer [Event EventQuery EventType]]
            [panda-5.persistence.core :as core]

            [jdbc.core :as jdbc]
            [cuerdas.core :as str]
            [schema.core :as s]
            [schema-tools.experimental.walk]))

(s/defn get-events :- [Event] [query :- EventQuery]
  (let [{:strs [uuid type payload]} (core/map->db query EventQuery
                                      {:matcher {EventType (comp str/camelize name)}})
        sql (str "select *\n"
                 "from events\n"
                 (when-let [clauses (not-empty (filter some? [(when uuid "uuid = ?")
                                                              (when type "type = ?")
                                                              (when payload "payload::jsonb @> ?::jsonb")]))]
                   (str "where "
                        (str/join " and " clauses)))
                 ";")
        params (filterv some? [sql uuid type payload])]
    (when-let [results (with-open [conn (core/get-connection)]
                         (jdbc/fetch conn params))]
      (mapv #(core/db->map %
               Event
               {:matcher {EventType (comp keyword str/dasherize)}})
            results))))

(s/defn get-event :- (s/maybe Event) [query :- EventQuery]
  (first (get-events query)))

(s/defn create-event! [event :- Event]
  (let [{:strs [uuid type payload]} (core/map->db event Event
                                      {:matcher {EventType (comp str/camelize name)}})
        sql (str "insert into events (uuid, type, payload) values (?, ?, ?);")
        params [sql uuid type payload]]
    (with-open [conn (core/get-connection)]
      (jdbc/execute conn params))))