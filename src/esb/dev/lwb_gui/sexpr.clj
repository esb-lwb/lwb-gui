; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0

(ns esb.dev.lwb-gui.sexpr
  (:require [seesaw.core :refer [value]])
  (:import (javax.swing.text JTextComponent)))

(defn parens-balanced?
  "Are parenthesis balanced in s?"
  [s]
  (loop [[first & coll] s level 0]
    (cond (neg? level) false
          (nil? first) (zero? level)
          (= first \() (recur coll (inc level))
          (= first \)) (recur coll (dec level))
          :else (recur coll level))))

(defn get-sexpr
  "Gets the sexpression in string s.
   requires: s begins with an opening parenthesis.
   returns: sexpression (as string) or nil if there is no such expression."
  [s]
  (let [level (atom 0)
        test-level (fn [ch]
                     (cond
                       (= ch \() (do (swap! level inc) true)
                       (= ch \)) (do (swap! level dec) (if (zero? @level) false true))
                       :else true))
        chars (conj (into [] (take-while test-level s)) \))]
    ; error handling, if there is no right parenthesis, then return nil
    (if (<= (count chars) (count s))
      (apply str chars)
      nil)))

(defn get-begin-current-sexpr
  "Find the index of first opening parenthesis left to pos in s.
   Returns -1 if not found."
  [s pos]
  (let [level-check (fn [s pos plevel]
                      (let [ch (nth s pos)]
                        (cond
                          (= ch \() (dec plevel)
                          (= ch \)) (inc plevel)
                          :else plevel)))]
    (loop [curr-pos pos plevel 1]
      (cond
        (and (= 0 curr-pos) (> plevel 0)) -1
        (= 0 plevel) curr-pos
        :else (recur (dec curr-pos) (level-check s (dec curr-pos) plevel))))))

(defn parens-level
  "Global level of opening parenthesis in s at pos.
   Returns 0 if its the outer most level, else > 0."
  [s pos]
  (let [level (atom 0)
        adj-level (fn [ch]
                    (cond
                      (= ch \() (swap! level inc)
                      (= ch \)) (swap! level dec)))
        text (reverse (subs s 0 pos))]
    (dorun (map adj-level text))
    @level))

(defn get-begin-top-sexpr
  "Find the index of most outer opening parenthesis left to pos in s.
   Returns -1 if not found."
  [s pos]
  (loop [curr-pos (.lastIndexOf s "(" pos)]
    (cond
      (= -1 curr-pos) -1
      (= 0 (parens-level s curr-pos)) curr-pos
      :else (recur (.lastIndexOf s "(" (dec curr-pos))))))

(defn get-current-sexpr
  "Returns the current sexpression of text-comp at caret."
  [^JTextComponent text-comp]
  (let [text (value text-comp)
        pos (.getCaretPosition text-comp)
        begin (get-begin-current-sexpr text pos)]
    (if (< begin 0)
      nil
      (get-sexpr (subs text begin)))))

(defn get-top-sexpr
  "Returns the most outer sexpression of text-comp at caret."
  [^JTextComponent text-comp]
  (let [text (value text-comp)
        pos (.getCaretPosition text-comp)
        begin (get-begin-top-sexpr text pos)]
    (if (< begin 0)
      nil
      (get-sexpr (subs text begin)))))


