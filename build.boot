#!/usr/bin/env boot

(defn read-dependencies! []
  (let [deps (read-string (slurp "resources/dependencies.edn"))]
    deps))

(set-env!
  :source-paths   #{"src"}
  :resource-paths #{"resources" "certs"}
  :dependencies (read-dependencies!)
  :exclusions '[org.clojure/clojure]
  :repositories #(conj % ["Immutant incremental builds"
                          "http://downloads.immutant.org/incremental/"])
  :main-class 'panda-5.core)

(task-options! aot {:namespace #{(get-env :main-class)}}
               jar {:main (get-env :main-class)}
               pom {:project 'panda-5
                    :version "1.0.0"})

(require '[boot.immutant :as immutant]
         '[adzerk.boot-test :as test]
         '[jeluard.boot-notify :as notify]
         '[boot.util :as util])

;; === Dependencies tasks

  (defn- generate-lein-project-file!
    "Generates leiningen project file."
    [& {:keys [keep-project] :or {:keep-project true}}]

    (require 'clojure.java.io)
    (let [pfile ((resolve 'clojure.java.io/file) "project.clj")
          ; Only works when pom options are set using task-options!
          {:keys [project version]} (:task-options (meta #'boot.task.built-in/pom))
          prop #(when-let [x (get-env %2)] [%1 x])
          head (list* 'defproject (or project 'boot-project) (or version "0.0.0-SNAPSHOT")
                 (concat
                   (prop :url :url)
                   (prop :license :license)
                   (prop :description :description)
                   [:dependencies (get-env :dependencies)
                    :source-paths (vec (concat (get-env :source-paths)
                                               (get-env :resource-paths)))
                    :repositories (get-env :repositories)]))
          proj (pp-str head)]
        (if-not keep-project (.deleteOnExit pfile))
        (spit pfile proj)))

  (defn- modified-files? [before-fileset after-fileset files]
    (->> (fileset-diff @before-fileset after-fileset)
         input-files
         (by-name files)
         not-empty))

  (deftask lein-generate
    "Generate a leiningen `project.clj` file.
     This task generates a leiningen `project.clj` file based on the boot
     environment configuration, including project name and version (generated
     if not present), dependencies, and source paths. Additional keys may be added
     to the generated `project.clj` file by specifying a `:lein` key in the boot
     environment whose value is a map of keys-value pairs to add to `project.clj`."
    []

    (let [fs-prev-state (atom nil)]
      (with-pre-wrap fileset
        (when true ; (modified-files? fs-prev-state fileset #{"resources/dependencies.edn"})
          (util/info "Regenerating project clj...\n")
          (generate-lein-project-file! :keep-project true)
          (reset! fs-prev-state fileset))
        fileset)))

  (deftask update-deps
    "Updates dependencies from `resources/dependencies.edn` if they changed.
     Might save you a restart, might not."
    []

    (let [fs-prev-state (atom nil)]
      (with-pre-wrap fileset
        (when true ; (modified-files? fs-prev-state fileset #{"resources/dependencies.edn"})
          (util/info "Dependencies changed, updating...\n")
          (set-env! :dependencies (fn [deps] (read-dependencies!)))
          (reset! fs-prev-state fileset))
        fileset)))

; === App tasks

(deftask dev
  "Runs development"
  []

  (set-env! :source-paths #(conj % "test"))
  (comp
    (watch)
    (update-deps)
    (lein-generate)
    (notify/notify)
    (repl :server true)))

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []

  (comp
    (aot)
    (pom)
    (uber)
    (jar)))

(deftask build-immutant
  "Build a WidFly-compatible war."
  []

  (set-env! :resource-paths #(conj % "src"))
  (comp
    (immutant/immutant-war :context-path "/"
                           :init-fn 'panda-5.core/start!
                           :name "ROOT"
                           :nrepl-host "0.0.0.0"
                           :nrepl-port 12132
                           :nrepl-start true)))

(defn start! []
  (require 'panda-5.core)
  (apply (resolve 'panda-5.core/start!) []))

(defn stop! []
  (require 'panda-5.core)
  (apply (resolve 'panda-5.core/stop!) []))
