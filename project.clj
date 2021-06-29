(defproject lwb-gui "1.0.3"
  :date "2021-06-29"
  :description "A simple GUI for the Logic Workbench"
  :url "https://guthub.com/esb-lwb/lwb-gui"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 ; for seesaw
                 [com.miglayout/miglayout "3.7.4"]
                 [com.jgoodies/forms "1.3.0"]
                 [org.swinglabs.swingx/swingx-core "1.6.3"]
                 [j18n "1.0.2"]
                 [rsyntaxtextarea/rsyntaxtextarea "3.0.9-esb-dev"]
                 [seesaw "1.5.1-esb-dev"]
                 [lwb/lwb "2.2.4"]] ;; update consts/about too!
  :main esb.dev.lwb-gui.main
  :profiles {:uberjar  {:aot :all}}
  :uberjar-name "lwb-gui.jar")

; lwb is not available as a maven jar
; compile github/esb-lwb/lwb and put the lwb.jar into your local maven repo
; lein localrepo install lwb.jar lwb n.n.n
