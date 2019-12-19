; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

; Preferences 
; values are Clojure data structures
; keys should be string with less than 80 characters

(ns lwb-gui.prefs
  (:import (java.util.prefs Preferences)))

; Node to store the preference data
(def lwb-gui-prefs (let [^Preferences p (Preferences/userRoot)]
                     (.node p "lwb-gui")))

; used for long values
(defn partition-str [^Integer n s]
  (let [^Integer l (.length s)]
    (for [i (range 0 l n)]
      (.substring s i (Math/min l (+ (int i) (int n)))))))

; Maximum length of string allowed as a value is 8192 characters.
(def pref-max-bytes (* 3/4 Preferences/MAX_VALUE_LENGTH))

(defn pput
  "Writes a Clojure data structure to the preferences store."
  [key value]
  (let [chunks (partition-str pref-max-bytes (with-out-str (pr value)))
        node (.node lwb-gui-prefs key)]
    (.clear node)
    (doseq [i (range (count chunks))]
      (. node put (str i) (nth chunks i)))))

(defn pget
  "Reads a Clojure data structure from the preferences store."
  [key]
  (let [node (.node lwb-gui-prefs key)]
    (let [s (apply str
                   (for [i (range (count (. node keys)))]
                     (.get node (str i) nil)))]
      (when (and s (pos? (.length s))) (read-string s)))))