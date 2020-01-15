(defproject lwb-gui "1.0.0-BETA"
  :date "2020-01-15"
  :description "A simple GUI for the Logic Workbench"
  :url "https://guthub.com/esb-lwb/lwb-gui"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ; for seesaw
                 [com.miglayout/miglayout "3.7.4"]
                 [com.jgoodies/forms "1.2.1"]
                 [org.swinglabs.swingx/swingx-core "1.6.3"]
                 [j18n "1.0.2"]
                 [rsyntaxtextarea/rsyntaxtextarea "3.0.9-esb-dev"]
                 [seesaw "1.5.1-esb-dev"]
                 [lwb "2.1.5"]] ;; update consts/about too!
  :main esb.dev.lwb-gui.main
  :profiles {:uberjar  {:aot :all}}
  :uberjar-name "lwb-gui.jar")
