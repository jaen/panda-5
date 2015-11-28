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
   [org.immutant/web "2.x.incremental.692"]
   [org.mortbay.jetty.alpn/alpn-boot "8.1.6.v20151105"]
   [org.immutant/scheduling "2.1.1"]
   [org.immutant/messaging "2.1.1"]
   [ring "1.4.0"]
   [ring/ring-devel "1.4.0" :scope "test"]
   [ring/ring-core "1.4.0"]
   [environ "1.0.1"]
   [funcool/cuerdas "0.6.0"]
   [org.clojure/core.match "0.3.0-alpha4"]
   [prismatic/schema "1.0.3"]
   [metosin/schema-tools "0.7.0"]
   [org.postgresql/postgresql
    "9.4-1205-jdbc42"
    :exclusions
    [org.slf4j/slf4j-simple]]
   [danlentz/clj-uuid "0.1.6"]
   [hikari-cp "1.2.4"]
   [joplin.core "0.3.4"]
   [joplin.jdbc "0.3.4"]
   [funcool/clojure.jdbc "0.6.1"]
   [org.clojure/core.async "0.2.374" :scope "test"]
   [slingshot "0.12.2"]
   [org.slf4j/log4j-over-slf4j "1.7.12"]
   [org.slf4j/jul-to-slf4j "1.7.12"]
   [org.slf4j/jcl-over-slf4j "1.7.12"]
   [com.palletops/log-config "0.1.4"]
   [org.jboss/jboss-vfs "3.2.9.Final" :scopte "test"]
   [boot-immutant "0.5.0" :scope "test"]
   [adzerk/boot-test "1.0.5" :scope "test"]
   [jeluard/boot-notify "0.2.0" :scope "test"]]
  :source-paths
  ["src" "test" "certs" "resources"]
  :repositories
  [["clojars" "https://clojars.org/repo/"]
   ["maven-central" "https://repo1.maven.org/maven2/"]
   ["Immutant incremental builds"
    "http://downloads.immutant.org/incremental/"]])