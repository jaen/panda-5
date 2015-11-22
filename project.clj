(defproject
  panda-5
  "1.0.0"
  :dependencies
  [[org.clojure/clojure "1.7.0" :scope "provided"]
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
   [org.postgresql/postgresql
    "9.4-1205-jdbc42"
    :exclusions
    [org.slf4j/slf4j-simple]]
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
  :source-paths
  ["src"
   "db"
   "/home/jaen/.m2/repository/funcool/clojure.jdbc/0.6.2"
   "test"])