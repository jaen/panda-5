(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.7.0" :scope "provided"]
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
   [environ "1.0.1"]
   [funcool/cuerdas "0.6.0"]]
  :source-paths
  ["src"])