(ns panda-5.persistence.migrations
  (:require [panda-5.persistence.core :as persistence]
            [panda-5.utils.joplin-jdbc :as joplin-jdbc]

            [clojure.java
             [classpath :as classpath]
             [io :as io]]
            [clojure.set :as set]
            [clojure.string :as string]

            [joplin.core :as joplin]
            [taoensso.timbre :as log])
  (:import [org.jboss.vfs VirtualFile]))

(defn get-config []
  (let [sql-dev-db {:type :jdbc, :data-source (persistence/get-data-source)}
        migrator   "src/db/migrations/jdbc" ;; this is relative to the classpath, not relative to the run root.
        seeder     "db.seeds.jdbc.events/run"]
    [{:db sql-dev-db :migrator migrator :seed seeder}]))

(defn drop-first-part [path delimiter]
  (->> (string/split path #"/")
       rest
       (interpose delimiter)
       (apply str)))

(defn fixed-get-files
  "Get migrations files given a folder path.
Will try to locate files on the local filesystem, folder on the classpath
or resource folders inside a jar on the classpath"
  [path]
  (let [local-folder          (io/file path)
        classpath-folder-name (drop-first-part path "/")
        local-resource        (when-let [resource (io/resource classpath-folder-name)]
                                (if (= (.getProtocol resource) "vfs")
                                  (.getContent resource)
                                  (io/file resource)))
        folder-on-classpath   (->> (classpath/classpath-directories)
                                   (map #(str (.getPath %) "/" classpath-folder-name))
                                   (map io/file)
                                   (filter #(.isDirectory %))
                                   first)]

    (cond
      ;; If it's a local folder just read the file from there
      (.isDirectory local-folder)
        (->> (.listFiles local-folder)
             (map #(vector % (.getName %))))

      (and local-resource (.isDirectory local-resource))
        (if (instance? VirtualFile local-resource)
          (let [children (.getChildren local-resource)]
            (->> (map #(.getPhysicalFile %) children)
                 (map #(vector % (.getName %)))))
          (->> (.listFiles local-resource)
               (map #(vector % (.getName %)))))

      ;; If it's a folder on the classpath use that
      folder-on-classpath
        (->> (.listFiles folder-on-classpath)
             (map #(vector % (.getName %))))

      ;; Try finding this path inside a jar on the classpath
      :else
        (->> (classpath/classpath-jarfiles)
             (mapcat classpath/filenames-in-jar)
             (filter #(.startsWith % classpath-folder-name))
             (map #(vector (io/resource %) (.getName (io/file %))))))))

(alter-var-root #'joplin.core/get-files (constantly fixed-get-files))

;(defn get-migration-namespaces
;  "Get a sequence on namespaces containing the migrations on a given folder path"
;  [path]
;  (when path
;    (let [ns (drop-first-part path ".")]
;      (->> (get-files path)
;           (map second)
;           (map #(re-matches #"(.*)(\.clj)$" %))
;           (keep second)
;           (map #(string/replace % "_" "-"))
;           sort
;           (mapv #(vector % (symbol (str ns "." %))))))))
;
;(defrecord JoplinMigration [id up down]
;  ragtime.protocols/Migration
;  (id [_] id)
;  (run-up! [_ db] (up db))
;  (run-down! [_ db] (down db)))

(defn pending-migrations? []
  #_(doseq [spec (get-config)]
    (let [test (#'joplin/get-migration-namespaces (:migrator spec))
          migrations (joplin/get-migrations (:migrator spec))
          db (#'ragtime.jdbc/map->SqlDatabase (#'joplin-jdbc/append-db-spec spec))
          applied-migrations (ragtime.protocols/applied-migration-ids db)
          welp #'joplin/get-pending-migrations]
      (log/debug "DEM TEST: " (vec test))
      (log/debug "DEM MIGRATIONS: " (vec migrations))
      (log/debug "DEM MIGRATION IDSS: " (set (map :id migrations)) )
      (log/debug "DEM APPLIED MIGRATION IDS: " (set applied-migrations))
      (log/debug "UNAPPLIED: " (clojure.set/difference (set (map :id migrations)) (set applied-migrations)))
    (welp db migrations))))

(defn ensure-migrated! []
  (doseq [spec (get-config)]
    (log/info "Ensuring database schema is up to date.")
    (pending-migrations?)
    (joplin/migrate-db spec)))

; (defn pending-migrations2? []
;   (doseq [spec (get-config)]
;     (joplin/pending-migrations spec)))

(defn ensure-seeds! []
  (doseq [spec (get-config)]
    (log/info "Ensuring database seeds are up to date.")
    (pending-migrations?)
    (joplin/seed-db spec)))

(defn ensure-database! []
  (log/info "Ensuring database is up to date.")
  (ensure-migrated!)
  (ensure-seeds!))
