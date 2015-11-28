(ns db.migrations.jdbc.20151120201907-create-events-table
  (:require [jdbc.core :as jdbc]))

(defn up [db]
  (let [db-spec (get-in db [:db-spec])]
    (with-open [conn (jdbc/connection db-spec)]
      (jdbc/execute conn "create table events (uuid uuid primary key, type varchar(64), payload jsonb);"))))

(defn down [db]
  (with-open [conn (jdbc/connection (get-in db [:db-spec]))]
    (jdbc/execute conn "drop table events;")))