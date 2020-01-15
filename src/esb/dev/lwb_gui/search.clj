; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

(ns esb.dev.lwb-gui.search
  (:require [seesaw.core :refer :all])
  (:import (javax.swing JCheckBox JTextArea JViewport)
           (java.util.regex Pattern Matcher)
           (javax.swing.text DefaultHighlighter$DefaultHighlightPainter)
           (java.awt Color Point)))

;; Comment: There are similar functions in RTextArea class SearchEngine,
;; it may be smart to use them (?)

;; Atoms for searching
(def search-highlights (atom nil))
(def current-pos (atom 0))

;; highlighting
(defn highlight
  "Highlight region in edit area."
  ([^JTextArea text-comp start stop color]
   (when (and (<= 0 start) (<= stop (.getLength (.getDocument text-comp))))
     (let [hl (.getHighlighter text-comp)]
       (.addHighlight hl start stop
                      (DefaultHighlighter$DefaultHighlightPainter. color)))))
  ([text-comp pos color] (highlight text-comp pos (inc pos) color)))

(defn remove-highlight
  "Remove highlighting."
  [text-comp highlight-object]
  (when highlight-object
    (.removeHighlight (.getHighlighter text-comp) highlight-object)))

(defn remove-highlights
  "Remove all highlighting."
  [text-comp highlights]
  (dorun (map #(remove-highlight text-comp %) highlights)))

(defn configure-search
  "Configure saerching."
  [match-case use-regex]
  (bit-or Pattern/CANON_EQ
          Pattern/UNICODE_CASE
          (if match-case 0 Pattern/CASE_INSENSITIVE)
          (if use-regex 0 Pattern/LITERAL)))

(defn find-all-in-string
  "Find all occurences of search s in text t."
  [s t match-case use-regex]
  (try
    (when (pos? (.length t))
      (let [p ^Pattern (Pattern/compile t (configure-search match-case use-regex))
            m ^Matcher (re-matcher p s)]
        (loop [positions []]
          (if (.find m)
            (recur (conj positions [(.start m) (.end m)]))
            positions))))
    (catch Exception _ [])))

(defn highlight-found
  "Highlight found occurences."
  [text-comp posns]
  (doall
    (map #(highlight text-comp (first %) (second %) Color/YELLOW) posns)))

(defn next-item
  "Get next item."
  [cur-pos posns]
  (or (first (drop-while #(> cur-pos (first %)) posns)) (first posns)))

(defn prev-item
  "Get previous item."
  [cur-pos posns]
  (or (first (drop-while #(< cur-pos (first %)) (reverse posns))) (last posns)))

(defn scroll-to-pos 
  "Scroll to offset in text area, i.e. set view positiin accordingly."
  [^JTextArea text-area offset]
  (let [r (.modelToView text-area offset)
        ^JViewport v (.getParent text-area)
        l (.height (.getViewSize v))
        h (.height (.getViewRect v))]
    (when r
      (.setViewPosition v (Point. 0 (min (- l h) (max 0 (- (.y r) (/ h 2)))))))))

(defn update-find-highlight
  "Update highlighted items."
  [search-text root back]
  (let [edit-area (select root [:#edit-area])
        match-case (.isSelected ^JCheckBox (select root [:#search-case]))
        use-regex (.isSelected ^JCheckBox (select root [:#search-regex]))
        posns (find-all-in-string (text edit-area) (text search-text) match-case use-regex)]
    (remove-highlights edit-area @search-highlights)
    (if (pos? (count posns))
      (let [selected-pos
            (if back (prev-item (dec @current-pos) posns)
                     (next-item @current-pos posns))
            posns (remove #(= selected-pos %) posns)
            pos-start (first selected-pos)
            pos-end (second selected-pos)]
        (.setBackground edit-area Color/WHITE)
        (doto edit-area
          (.setSelectionStart pos-end)
          (.setSelectionEnd pos-end))
        (reset! current-pos pos-start)
        (reset! search-highlights
                (conj (highlight-found edit-area posns)
                      (highlight edit-area pos-start pos-end (.getSelectionColor edit-area))))
        (scroll-to-pos edit-area pos-start))
      (.setBackground search-text Color/PINK))))

(defn highlight-step
  "Highlight forward or backward (if back is true)."
  [e back]
  (let [root (to-root e)
        search-text (select root [:#search-text])]
    (when-not back
      (swap! current-pos inc))
    (update-find-highlight search-text root back)))
