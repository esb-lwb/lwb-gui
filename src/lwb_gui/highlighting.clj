; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

(ns lwb-gui.highlighting
  (:import (javax.swing.text DefaultHighlighter$DefaultHighlightPainter)
           (java.awt Color)
           (javax.swing JTextArea))
  (:require [lwb-gui.utils :as utils]))

; TODO: Mimik verbessern
(defn highlight
  ([^JTextArea text-comp start stop color]
   (when (and (<= 0 start) (<= stop (.. text-comp getDocument getLength)))
     (.. text-comp getHighlighter
         (addHighlight start stop
                       (DefaultHighlighter$DefaultHighlightPainter. color)))))
  ([text-comp pos color] (highlight text-comp pos (inc pos) color)))

(defn remove-highlight
  ([text-comp highlight-object]
   (when highlight-object
     (.removeHighlight (.getHighlighter text-comp)
                       highlight-object))))

(defn remove-highlights [text-comp highlights]
  (dorun (map #(remove-highlight text-comp %) highlights)))

(def highlights (atom {}))

(defn highlight-brackets [text-comp good-enclosures bad-brackets]
  (utils/awt-event
    (remove-highlights text-comp (get @highlights text-comp))
    (swap! highlights assoc text-comp
           (doall (concat
                    (map #(highlight text-comp % Color/LIGHT_GRAY) good-enclosures)
                    (map #(highlight text-comp % Color/PINK) bad-brackets))))))

