; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

(ns lwb-gui.repl.output
  (:import (java.util.concurrent.atomic AtomicInteger)
           (javax.swing JScrollPane JTextArea)
           (javax.swing.event DocumentEvent DocumentListener)
           (java.awt Graphics)))

(defn end-position
  "Finds the end position of an insert or change in a document
   as reported in a DocumentEvent instance."
  [^DocumentEvent document-event]
  (+ (.getOffset document-event)
     (.getLength document-event)))

(defn tailing-scroll-pane
  "Embeds the given JTextArea in a JScrollPane that scrolls
   to the bottom whenever text is inserted or appended."
  [^JTextArea text-area]
  (let [scroll-offset (AtomicInteger. -1)
        scroll-pane (proxy [JScrollPane] [text-area]
                      (paintComponent [^Graphics graphics]
                        (let [offset (.getAndSet scroll-offset -1)]
                          (when (not= -1 offset)
                            (let [vsb (.getVerticalScrollBar this)]
                              (.setValue vsb (.y (.modelToView text-area offset))))))
                        (proxy-super paintComponent graphics)))
        set-scroll-offset (fn [e]
                            (.set scroll-offset (end-position e))
                            (.repaint scroll-pane))]
    (let [doc (.getDocument text-area)]
      (.addDocumentListener doc
                            (proxy [DocumentListener] []
                              (changedUpdate [e] (set-scroll-offset e))
                              (insertUpdate [e] (set-scroll-offset e))
                              (removeUpdate [_]))))
    scroll-pane))
