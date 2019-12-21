(defproject lwb-gui "0.9.0"
  :date "2019-12-19"
  :description "A simple GUI for the Logic Workbench"
  :url "https://guthub.com/esb-lwb/lwb-gui"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [rsyntaxtextarea/rsyntaxtextarea "3.0.5z"]
                 [lwb "2.1.2"]]
  :main lwb-gui.main
  :profiles {:uberjar  {:aot :all}}
  :uberjar-name "lwb-gui.jar")
