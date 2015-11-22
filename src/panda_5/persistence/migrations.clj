(ns panda-5.persistence.migrations
  (:require [panda-5.persistence.core :as persistence]
            [panda-5.utils.joplin-jdbc]

            [joplin.core :as joplin]))

(defn get-config []
  (let [sql-dev-db {:type :jdbc, :data-source (persistence/get-data-source)}
        migrator   "db/migrations/jdbc"
        seeder     "seeds.jdbc.events/run"]
    [{:db sql-dev-db :migrator migrator :seed seeder}]))

(defn ensure-migrated! []
      (doseq [spec (get-config)]
        (joplin/migrate-db spec)))

(defn ensure-seeds! []
      (doseq [spec (get-config)]
        (joplin/seed-db spec)))

(defn ensure-database! []
  (ensure-migrated!)
  (ensure-seeds!))