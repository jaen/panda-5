#!/usr/bin/env boot

(set-env!
 :resource-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.7.0" :scope "provided"]
                 [bidi "1.21.1" :exclusions [org.clojure/clojurescript]]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [hiccup "1.0.5"]
                 [clj-time "0.11.0"]
                 [org.immutant/web "2.1.1"]
                 [org.immutant/scheduling "2.1.1"]
                 [ring "1.4.0"]
                 [ring/ring-headers "0.1.1"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-core "1.4.0"]
                 [environ "1.0.1"]]
  :main-class 'panda-5.core)

(defn- generate-lein-project-file! [& {:keys [keep-project] :or {:keep-project true}}]
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

(deftask lein-generate
  "Generate a leiningen `project.clj` file.
   This task generates a leiningen `project.clj` file based on the boot
   environment configuration, including project name and version (generated
   if not present), dependencies, and source paths. Additional keys may be added
   to the generated `project.clj` file by specifying a `:lein` key in the boot
   environment whose value is a map of keys-value pairs to add to `project.clj`."
 []
 (generate-lein-project-file! :keep-project true))

(deftask dev
  "Runs development"
  []
  (lein-generate)
  (repl))

(deftask build
  "Builds an uberjar of this project that can be run with java -jar"
  []
  (comp
   (aot :namespace #{(get-env :main-class)})
   (pom :project 'panda-5
        :version "1.0.0")
   (uber)
   (jar :main (get-env :main-class))))

(deftask heroku
  "Prepare project.clj and Procfile for Heroku deployment."
  [& [main-class]]
  (let [jar-name   "panda-5-standalone.jar"
        jar-path   (format "target/%s" jar-name)
        main-class (or main-class (get-env :main-class))]
    (assert main-class "missing :main-class entry in env")
    (set-env!
      :src-paths #{"resources"}
      :lein      {:min-lein-version "2.0.0" :uberjar-name jar-name})
    (comp
      (lein-generate)
      (with-pre-wrap
        (println "Writing project.clj...")
        (-> "project.clj" slurp
          (.replaceAll "(:min-lein-version)\\s+(\"[0-9.]+\")" "$1 $2")
          ((partial spit "project.clj")))
        (println "Writing Procfile...")
        (-> "web: java $JVM_OPTS -cp %s clojure.main -m %s"
          (format jar-path main-class)
          ((partial spit "Procfile")))))))

; (defn -main [& args]
;   (require 'my-namespace)
;   (apply (resolve 'my-namespace/-main) args))