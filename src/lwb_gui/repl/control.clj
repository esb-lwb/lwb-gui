; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

; Controls the access to the Clojure REPL

(ns lwb-gui.repl.control
  (:import (java.io PrintWriter Writer)
           (org.fife.ui.rsyntaxtextarea TextEditorPane))
  (:require
    [lwb-gui.brackets :as brackets]
    [lwb-gui.repl.clj-repl :as clj-repl]))

(defn send-to-repl
  "Sends command to REPL.
   If silent? the command is not shown in the REPL, just the result of the evaluation."
  [app cmd silent?]
  (let [cmd-ln (str cmd \newline)]
    (when-not silent?
      (.append (app :repl-area) cmd-ln))
    (when-let [repl-map @(app :repl-map)]
      (clj-repl/evaluate repl-map cmd))))

(defn parens-balanced?
  "Are parenthesis balanced in s?"
  [s]
  (loop [[first & coll] s level 0]
    (cond (neg? level) false
          (nil? first) (zero? level)
          (= first \() (recur coll (inc level))
          (= first \)) (recur coll (dec level))
          :else (recur coll level))))

(defn send-selected-to-repl
  "Sends selected text or current sexpression to the REPL."
  [app]
  (let [ta (app :edit-area)
        txt (if-let [text (.getSelectedText ta)]
              text
              (brackets/get-current-sexpr ta))]
    (if (or (nil? txt) (not (parens-balanced? txt)))
      (.append (app :repl-area) "Not a well-formed sexpression!\n") ;; error!
      (send-to-repl app txt false))))

(defn send-top-sexpr-to-repl
  "Sends top sexpression to the REPL."
  [app]
  (let [ta (app :edit-area)
        txt (brackets/get-top-sexpr ta)]
    (if (nil? txt)
      (.append (app :repl-area) "Not a well-formed sexpression!\n") ;; error!
      (send-to-repl app txt false))))

(defn send-doc-to-repl
  "Sends the whole content of the edit area to the REPL."
  [app]
  (let [text (.getText ^TextEditorPane (app :edit-area))]
    (.append (app :repl-area) "Evaluating file...\n")
    (send-to-repl app text true)))

(defn make-repl-writer
  "Constructor for special variant of PrintWriter, that writes to repl-area."
  [repl-area]
  (->
    (proxy [Writer] []
      (write
        ([char-array _ _]
         (.append repl-area (apply str char-array)))
        ([t]
         (if (= Integer (type t))
           (.append repl-area (str (char t)))
           (.append repl-area (apply str t)))))
      (flush [])
      (close [] nil))
    (PrintWriter. true)))

(defn start-repl
  "Starts REPL."
  [app]
  (let [repl-area (:repl-area app)]
    (.append repl-area (str "--- This is the Clojure REPL ---\n"))
    (let [repl-map (clj-repl/launch-repl (make-repl-writer repl-area))]
      (reset! (:repl-map app) repl-map))))

(defn stop-repl
  "Stops REPL."
  [app]
  (.append (:repl-area app) "\n--- Bye, shutdown of Clojure REPL  ---\n")
  (when-let [repl-map @(:repl-map app)] (clj-repl/close repl-map)))

(defn restart-repl
  "Restarts REPL."
  [app]
  (stop-repl app)
  (start-repl app))

