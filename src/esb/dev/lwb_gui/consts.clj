; Copyright (c) 2019 - 2020 by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0

; Logic Workbench GUI - Constants 

(ns esb.dev.lwb-gui.consts
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (javax.swing ImageIcon)))

; version from project file
(defmacro const-from-projectfile
  "Get version and date from project file."
  []
  (let [p (edn/read-string (slurp "project.clj"))]
    {:proj-version (nth p 2) :proj-date (nth p 4)}))

(def rev
  (let [p (const-from-projectfile)]
    (str (:proj-version p) " [" (:proj-date p) "]")))

(def lwb-gui "Logic Workbench GUI")
(def lwb-wiki "https://github.com/esb-lwb/lwb/wiki/")

;; Platform
(defn get-os
  "os.name from platform."
  []
  (.toLowerCase (System/getProperty "os.name")))
(def is-mac
  (memoize #(not (neg? (.indexOf (get-os) "mac")))))

; Icons
(def icon-128 (ImageIcon. (io/resource "lwb-128.png")))
(def icon-64 (ImageIcon. (io/resource "lwb-64.png")))

; Namespaces for new sessions
(def new-prop
  "(ns prop
    (:require [lwb.prop :refer :all]
              [lwb.prop.nf :refer [literal? nnf cnf cnf? dnf dnf?]]  
              [lwb.prop.sat :refer [tseitin sat sat? valid?]]
              [lwb.prop.cardinality :refer [min-kof max-kof kof oneof]]
              [lwb.prop.bdd :as bdd]))\n")

(def new-pred
  "(ns pred
    (:require [lwb.pred :refer :all]
              [lwb.pred.sat :refer [sat sat? valid?]]
              [lwb.pred.substitution :refer [freefor? substitution]]))\n")

(def new-ltl
  "(ns ltl
    (:require [lwb.ltl :refer :all]
              [lwb.ltl.eval :refer [eval-phi]]
              [lwb.ltl.buechi :refer [ba ks->ba]]
              [lwb.ltl.sat :refer [sat sat? valid?]]
              [lwb.ltl.kripke :as kripke]))\n")

(def new-nd
  "(ns nd 
    (:require [lwb.nd.repl :refer :all]))\n
    ; Load one of the following logic rules:
    ; (load-logic :prop)
    ; (load-logic :pred)
    ; (load-logic :ltl)\n")

(def new-cl
  "(ns cl 
    (:require [lwb.cl :refer :all]
              [lwb.cl.repl :refer :all]))\n")

(def
  about
  (clojure.string/join
    \newline
    ["<html><strong>Logic Workbench GUI</strong>"
     (str "Rev " rev)
     "uses Logic Workbench lwb Revision 2.2.4"
     "Project home: https:/github.com/esb-lwb/lwb-gui"
     "Copyright © 2014 - 2021, Burkhardt Renz"
     ""
     "<html>Licensed under Eclipse Public License 1.0<br />"
     "<html>Written in <strong>Clojure</strong> (clojure.org)<br />"
     "<html>inspired by Clooj (github.com/arthuredelstein/clooj)<br />"
     ""
     "<html>Libraries used:"
     "RSyntaxTextArea (github.com/bobbylight/RSyntaxTextArea)"
     "licensed under a BSD 3-Clause license"
     "Copyright © 2019, Robert Futrell"
     "seesaw (https://github.com/daveray/seesaw)"
     "Copyright © 2012, Dave Ray"]))

    