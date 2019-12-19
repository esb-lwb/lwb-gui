; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

(ns lwb-gui.brackets
  (:require [lwb-gui.utils :as utils])
  (:import (javax.swing.text JTextComponent)))

(defn mismatched-brackets [a b]
  (and (or (nil? a) (some #{a} [\( \[ \{]))
       (some #{b} [\) \] \}])
       (not (some #{[a b]} [[\( \)] [\[ \]] [\{ \}]]))))

(defn process-bracket-stack
  "Receiving a bracket stack s, deal with the next character c
   and datum dat."
  [s c dat]
  (let [l (ffirst s)                                        ;last char
        p (next s)                                          ;pop stack
        j (conj s [c dat])]                                 ;conj [char dat] to stack
    (condp = l
      \\ p
      \" (condp = c, \" p, \\ j, s)
      \; (if (= c \newline) p s)
      (condp = c
        \" j \\ j \; j                                      ;"
        \( j \[ j \{ j
        \) p \] p \} p
        s))))

(defn find-enclosing-brackets [text pos]
  (let [process #(process-bracket-stack %1 %2 nil)
        reckon-dist (fn [stacks]
                      (let [scores (map count stacks)]
                        (utils/count-while #(<= (first scores) %) scores)))
        before (.substring text 0 (Math/min (.length text) pos))
        stacks-before (reverse (reductions process nil before))
        left (- pos (reckon-dist stacks-before))
        after (.substring text (Math/min (.length text) pos))
        stacks-after (reductions process (first stacks-before) after)
        right (+ -1 pos (reckon-dist stacks-after))]
    [left right]))

(defn find-bad-brackets [text]
  (loop [t text pos 0 stack nil errs nil]
    (let [c (first t)                                       ;this char
          new-stack (process-bracket-stack stack c pos)
          e (if (mismatched-brackets (ffirst stack) c)
              (list (first stack) [c pos]))
          new-errs (if e (concat errs e) errs)]
      (if (next t)
        (recur (next t) (inc pos) new-stack new-errs)
        (filter identity
                (map second (concat new-stack errs)))))))

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
                          (= ch \( ) (dec plevel)
                          (= ch \) ) (inc plevel)
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
  (let [text (utils/get-text-str text-comp)
        pos (.getCaretPosition text-comp)
        begin (get-begin-current-sexpr text pos)]
    (if (< begin 0)
      nil
      (get-sexpr (subs text begin)))))

(defn get-top-sexpr
  "Returns the most outer sexpression of text-comp at caret."
  [^JTextComponent text-comp]
  (let [text (utils/get-text-str text-comp)
        pos (.getCaretPosition text-comp)
        begin (get-begin-top-sexpr text pos)]
    (if (< begin 0)
      nil
      (get-sexpr (subs text begin)))))
