(defproject lwb-gui "0.1.0"
  :date "2019-12-03"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [rsyntaxtextarea/rsyntaxtextarea "3.0.5z"]
                 [seesaw "1.5.0"]
                 [lwb "2.1.2"]]
  :main lwb-gui.main
  :profiles {:uberjar  {:aot :all}}
  :uberjar-name "lwb-gui.jar")
