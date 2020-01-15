(ns esb.dev.lwb-gui.prefs
  (:require [seesaw.pref :refer [preference-atom]])
  (:import (java.awt Window Container Component)
           (javax.swing JSplitPane JFrame)))

; Atoms synced with preferences storage

(def current-dir  (preference-atom :current-dir))
(def recent-files (preference-atom :recent-files))
(def window-shape (preference-atom :window-shape))
(def settings     (preference-atom :settings {:font-name    "Monospaced"
                                              :font-size    12
                                              :edit-wrap    false
                                              :edit-lino    false
                                              :edit-tabsize 2
                                              :repl-wrap    false}))

;; saving and restoring window shape in preferences -----------------------------------
;; Comment: The location of the divider of the split pane must be set in the
;; definition of the split pane, so that seesaw can use this value
;; That's the reason to split the handling of the frame position and the
;; location of the divider.

(defn get-shapes [components]
  (for [comp components]
    (condp instance? comp
      Window [:window {:x (.getX comp) :y (.getY comp)
                       :w (.getWidth comp) :h (.getHeight comp)}]
      JSplitPane [:split-pane {:location (.getDividerLocation comp)}]
      nil)))

(defn widget-seq [^Component comp]
  (filter #(or (instance? Window %) (instance? JSplitPane %))
          (tree-seq #(instance? Container %)
                    #(seq (.getComponents %))
                    comp)))

; we know the exact structure of @window-shape
(defn get-divider-location []
  (let [shape (second @window-shape)]
    (:location (second shape))))

(defn set-frame-size [^JFrame f]
  (let [shape (first @window-shape)]
    (if (some? shape)
      (let [{:keys [x y w h]} (second shape)]
        (.setBounds f x y w h)))))

(defn store-shapes [^JFrame f]
  (let [components (widget-seq f)]
    (reset! window-shape (get-shapes components))))
