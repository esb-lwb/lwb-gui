; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

; Caret movement

(ns lwb-gui.navigate
  (:require [lwb-gui.utils :as utils])
  (:import (javax.swing JTextArea)
           (javax.swing.text Utilities)))

(defn to-start [comp]
  (.setCaretPosition comp 0))

(defn to-end [comp]
  (let [len (-> (.getDocument comp) (.getLength))]
    (.setCaretPosition comp len)))

(defn to-line-start [comp]
  (let [lstart (Utilities/getRowStart comp (.getCaretPosition comp))]
    (.setCaretPosition comp lstart)))

(defn to-line-end [comp]
  (let [lend (Utilities/getRowStart comp (.getCaretPosition comp))]
    (.setCaretPosition comp lend)))

(defn attach--keys [^JTextArea comp]
  (utils/attach-action-keys comp
                            ["cmd1 LEFT" #(to-line-start comp)]
                            ["cmd1 RIGHT" #(to-line-end comp)]
                            ["cmd1 UP" #(to-start comp)]
                            ["cmd1 DOWN" #(to-end comp)]))