#!/usr/bin/env boot

(set-env!
 :source-paths   #{"src" "db"}
 :dependencies '[[org.clojure/clojure "1.7.0" :scope "provided"]
                 [bidi "1.21.1" :exclusions [org.clojure/clojurescript]]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [hiccup "1.0.5"]
                 [clj-time "0.11.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [org.immutant/web "2.1.1"]
                 [org.immutant/scheduling "2.1.1"]
                 [org.immutant/messaging "2.1.1"]
                 [ring "1.4.0"]
                 [ring/ring-headers "0.1.1"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [environ "1.0.1"]
                 [funcool/cuerdas "0.6.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [prismatic/schema "1.0.3"]
                 [metosin/schema-tools "0.7.0"]
                 [yesql "0.5.1"]
                 [org.postgresql/postgresql "9.4-1205-jdbc42" :exclusions [org.slf4j/slf4j-simple]]
                 [danlentz/clj-uuid "0.1.6"]
                 [hikari-cp "1.2.4"]
                 [joplin.core "0.3.4"]
                 [joplin.jdbc "0.3.4"]
                 [funcool/clojure.jdbc "0.6.2"]
                 [org.clojure/core.async "0.2.374"]
                 [slingshot "0.12.2"]

                 [boot-immutant "0.5.0" :scope "test"]
                 [adzerk/boot-test "1.0.5" :scope "test"]
                 [jeluard/boot-notify "0.2.0" :scope "test"]]
  :main-class 'panda-5.core)

(require '[boot.immutant :as immutant]
         '[adzerk.boot-test :as test]
         '[jeluard.boot-notify :as notify])

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
                                             (get-env :resource-paths)))]))
        proj (pp-str head)]
      (if-not keep-project (.deleteOnExit pfile))
      (spit pfile proj)))

(task-options! aot {:namespace #{(get-env :main-class)}}
               jar {:main (get-env :main-class)}
               pom {:project 'panda-5
                    :version "1.0.0"})

(deftask lein-generate
  "Generate a leiningen `project.clj` file.
   This task generates a leiningen `project.clj` file based on the boot
   environment configuration, including project name and version (generated
   if not present), dependencies, and source paths. Additional keys may be added
   to the generated `project.clj` file by specifying a `:lein` key in the boot
   environment whose value is a map of keys-value pairs to add to `project.clj`."
  []

  (fn [arg]
    (generate-lein-project-file! :keep-project true)
    arg))

(deftask dev
  "Runs development"
  []

  (set-env! :source-paths #(conj % "test"))
  (comp
    (lein-generate)
    ; (watch)
    (checkout :dependencies '[[funcool/clojure.jdbc "0.6.2"]])
    (notify/notify)
    (repl)
    #_(test/test)))

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

  (set-env! :resource-paths #{"src"})
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