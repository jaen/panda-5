(defproject
  panda-5
  "1.0.0"
  :dependencies
  [[org.clojure/clojure "1.7.0" :scope "provided"]
   [bidi
    "1.21.1"
    :exclusions
    [org.clojure/clojure org.clojure/clojurescript]]
   [cheshire "5.5.0" :exclusions [org.clojure/clojure]]
   [clj-http "2.0.0" :exclusions [org.clojure/clojure]]
   [hiccup "1.0.5" :exclusions [org.clojure/clojure]]
   [clj-time "0.11.0" :exclusions [org.clojure/clojure]]
   [com.taoensso/timbre "4.1.4" :exclusions [org.clojure/clojure]]
   [org.immutant/web
    "2.x.incremental.692"
    :exclusions
    [org.clojure/clojure]]
   [org.mortbay.jetty.alpn/alpn-boot
    "8.1.6.v20151105"
    :scope
    "test"
    :exclusions
    [org.clojure/clojure]]
   [org.immutant/scheduling "2.1.1" :exclusions [org.clojure/clojure]]
   [org.immutant/messaging "2.1.1" :exclusions [org.clojure/clojure]]
   [org.immutant/caching "2.1.1" :exclusions [org.clojure/clojure]]
   [ring "1.4.0" :exclusions [org.clojure/clojure]]
   [ring/ring-devel
    "1.4.0"
    :scope
    "test"
    :exclusions
    [org.clojure/clojure]]
   [ring/ring-core "1.4.0" :exclusions [org.clojure/clojure]]
   [environ "1.0.1" :exclusions [org.clojure/clojure]]
   [funcool/cuerdas "0.6.0" :exclusions [org.clojure/clojure]]
   [org.clojure/core.match
    "0.3.0-alpha4"
    :exclusions
    [org.clojure/clojure]]
   [prismatic/schema "1.0.3" :exclusions [org.clojure/clojure]]
   [metosin/schema-tools "0.7.0" :exclusions [org.clojure/clojure]]
   [org.postgresql/postgresql
    "9.4-1205-jdbc42"
    :exclusions
    [org.clojure/clojure org.slf4j/slf4j-simple]]
   [danlentz/clj-uuid "0.1.6" :exclusions [org.clojure/clojure]]
   [hikari-cp "1.2.4" :exclusions [org.clojure/clojure]]
   [joplin.core "0.3.4" :exclusions [org.clojure/clojure]]
   [joplin.jdbc "0.3.4" :exclusions [org.clojure/clojure]]
   [funcool/clojure.jdbc "0.6.1" :exclusions [org.clojure/clojure]]
   [org.clojure/core.async
    "0.2.374"
    :scope
    "test"
    :exclusions
    [org.clojure/clojure]]
   [slingshot "0.12.2" :exclusions [org.clojure/clojure]]
   [org.slf4j/log4j-over-slf4j
    "1.7.12"
    :exclusions
    [org.clojure/clojure]]
   [org.slf4j/jul-to-slf4j "1.7.12" :exclusions [org.clojure/clojure]]
   [org.slf4j/jcl-over-slf4j
    "1.7.12"
    :exclusions
    [org.clojure/clojure]]
   [com.palletops/log-config "0.1.4" :exclusions [org.clojure/clojure]]
   [org.immutant/immutant-transit
    "0.2.3"
    :exclusions
    [org.clojure/clojure]]
   [com.cognitect/transit-clj
    "0.8.285"
    :exclusions
    [org.clojure/clojure]]
   [org.jboss/jboss-vfs
    "3.2.9.Final"
    :scopte
    "test"
    :exclusions
    [org.clojure/clojure]]
   [boot-immutant
    "0.5.0"
    :scope
    "test"
    :exclusions
    [org.clojure/clojure]]
   [adzerk/boot-test
    "1.0.5"
    :scope
    "test"
    :exclusions
    [org.clojure/clojure]]
   [jeluard/boot-notify
    "0.2.0"
    :scope
    "test"
    :exclusions
    [org.clojure/clojure]]]
  :source-paths
  ["src" "test" "certs" "resources"]
  :repositories
  [["clojars" "https://clojars.org/repo/"]
   ["maven-central" "https://repo1.maven.org/maven2/"]
   ["Immutant incremental builds"
    "http://downloads.immutant.org/incremental/"]])