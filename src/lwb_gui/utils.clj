; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

(ns lwb-gui.utils
  (:require [clojure.string :as string :only (join split)]
            [lwb-gui.prefs :as prefs])
  (:import (java.util UUID)
           (java.awt Point Window Component Container)
           (java.awt.event ActionListener ComponentAdapter)
           (javax.swing AbstractAction JButton JMenuItem BorderFactory
                        JSplitPane KeyStroke SpringLayout SwingUtilities JTextArea JViewport JMenu)
           (javax.swing.event CaretListener DocumentListener)
           (java.beans PropertyChangeListener)
           (javax.swing.text JTextComponent)))

;; general
(defmacro when-lets [bindings & body]
  (assert (vector? bindings))
  (let [n (count bindings)]
    (assert (zero? (mod n 2)))
    (assert (<= 2 n))
    (if (= 2 n)
      `(when-let ~bindings ~@body)
      (let [[a b] (map vec (split-at 2 bindings))]
        `(when-let ~a (when-lets ~b ~@body))))))

(defn count-while [pred coll]
  (count (take-while pred coll)))

(defmacro awt-event [& body]
  `(SwingUtilities/invokeLater
     (fn [] (try ~@body
                 (catch Throwable t#
                   (.printStackTrace t#))))))

(defmacro gen-map [& args]
  (let [kw (map keyword args)]
    (zipmap kw args)))

;; identify OS

(defn get-os []
  (.toLowerCase (System/getProperty "os.name")))

(def is-win
  (memoize #(not (neg? (.indexOf (get-os) "win")))))

(def is-mac
  (memoize #(not (neg? (.indexOf (get-os) "mac")))))

(def is-unix
  (memoize #(not (and (neg? (.indexOf (get-os) "nix"))
                      (neg? (.indexOf (get-os) "nux"))))))

;; swing layout

(defn put-constraint [comp1 edge1 comp2 edge2 dist]
  (let [edges {:n SpringLayout/NORTH
               :w SpringLayout/WEST
               :s SpringLayout/SOUTH
               :e SpringLayout/EAST}
        ^SpringLayout layout (.getLayout (.getParent comp1))]
    (.putConstraint layout ^String (edge1 edges) ^JTextComponent comp1
                    ^int dist ^String (edge2 edges) ^JTextComponent comp2)))

(defn put-constraints [comp & args]
  (let [args (partition 3 args)
        edges [:n :w :s :e]]
    (dorun (map #(apply put-constraint comp %1 %2) edges args))))

(defn constrain-to-parent
  "Distance from edges of parent comp args"
  [comp & args]
  (apply put-constraints comp
         (flatten (map #(cons (.getParent comp) %) (partition 2 args)))))

;; text components

(defn get-coords [^JTextArea text-comp offset]
  (let [row (.getLineOfOffset text-comp offset)
        col (- offset (.getLineStartOffset text-comp row))]
    {:row row :col col}))

(defn get-caret-coords [text-comp]
  (get-coords text-comp (.getCaretPosition text-comp)))

(defn add-text-change-listener [text-comp f]
  "Executes f whenever text is changed in text component."
  (.addDocumentListener
    (.getDocument text-comp)
    (reify DocumentListener
      (insertUpdate [_ _] (f text-comp))
      (removeUpdate [_ _] (f text-comp))
      (changedUpdate [_ _]))))

(defn get-text-str
  "Text from JTextComponent."
  [text-comp]
  (let [doc (.getDocument text-comp)]
    (.getText doc 0 (.getLength doc))))

(defn add-caret-listener [text-comp f]
  (.addCaretListener text-comp
                     (reify CaretListener (caretUpdate [_ _]
                                            (f text-comp)))))

(defn set-selection [text-comp start end]
  (doto text-comp (.setSelectionStart start) (.setSelectionEnd end)))

(defn scroll-to-pos [^JTextArea text-area offset]
  (let [r (.modelToView text-area offset)
        ^JViewport v (.getParent text-area)
        l (.height (.getViewSize v))
        h (.height (.getViewRect v))]
    (when r
      (.setViewPosition v
                        (Point. 0 (min (- l h) (max 0 (- (.y r) (/ h 2)))))))))

(defn scroll-to-line [text-comp line]
  (let [text (.getText text-comp)
        pos (inc (.length (string/join "\n" (take (dec line) (string/split text #"\n")))))]
    (.setCaretPosition text-comp pos)
    (scroll-to-pos text-comp pos)))

(defn get-selected-lines [text-comp]
  (let [row1 (.getLineOfOffset text-comp (.getSelectionStart text-comp))
        row2 (inc (.getLineOfOffset text-comp (.getSelectionEnd text-comp)))]
    (doall (range row1 row2))))

(defn get-selected-line-starts [text-comp]
  (map #(.getLineStartOffset text-comp %)
       (reverse (get-selected-lines text-comp))))

(defn insert-in-selected-row-headers [text-comp txt]
  (awt-event
    (let [starts (get-selected-line-starts text-comp)
          document (.getDocument text-comp)]
      (dorun (map #(.insertString document % txt nil) starts)))))

(defn remove-from-selected-row-headers [text-comp txt]
  (awt-event
    (let [len (count txt)
          document (.getDocument text-comp)]
      (doseq [start (get-selected-line-starts text-comp)]
        (when (= (.getText (.getDocument text-comp) start len) txt)
          (.remove document start len))))))

;; other gui

(defn make-split-pane [comp1 comp2 horizontal divider-size resize-weight]
  (doto (JSplitPane. (if horizontal JSplitPane/HORIZONTAL_SPLIT
                                    JSplitPane/VERTICAL_SPLIT)
                     true comp1 comp2)
    (.setResizeWeight resize-weight)
    (.setOneTouchExpandable false)
    (.setBorder (BorderFactory/createEmptyBorder))
    (.setDividerSize divider-size)))

;; keys

(defn get-keystroke [^String key-shortcut]
  (KeyStroke/getKeyStroke
    (-> key-shortcut
        (.replace "cmd1" (if (is-mac) "meta" "ctrl"))
        (.replace "cmd2" (if (is-mac) "ctrl" "alt")))))

;; actions

(defn attach-child-action-key
  "Maps an input-key on a swing component to an action,
  such that action-fn is executed when pred function is
  true, but the parent (default) action when pred returns
  false."
  [component input-key pred action-fn]
  (let [im (.getInputMap component)
        am (.getActionMap component)
        input-event (get-keystroke input-key)
        parent-action (if-let [tag (.get im input-event)]
                        (.get am tag))
        child-action
        (proxy [AbstractAction] []
          (actionPerformed [e]
            (if (pred)
              (action-fn)
              (when parent-action
                (.actionPerformed parent-action e)))))
        uuid (.toString (UUID/randomUUID))]
    (.put im input-event uuid)
    (.put am uuid child-action)))


(defn attach-action-key
  "Maps an input-key on a swing component to an action-fn."
  [component input-key action-fn]
  (attach-child-action-key component input-key
                           (constantly true) action-fn))

(defn attach-action-keys
  "Maps input keys to action-fns."
  [comp & items]
  (doall (map #(apply attach-action-key comp %) items)))

;; buttons

(defn create-button [^String text fn]
  (doto (JButton. text)
    (.addActionListener
      (reify ActionListener
        (actionPerformed [_ _] (fn))))))

;; menus

(defn add-menu-item
  ([^JMenu menu ^String item-name key-mnemonic key-accelerator response-fn]
   (let [menu-item (JMenuItem. item-name)]
     (when key-accelerator
       (.setAccelerator menu-item (get-keystroke key-accelerator)))
     (when (and (is-win) key-mnemonic)
       (.setMnemonic menu-item (.getKeyCode (get-keystroke key-mnemonic))))
     (.addActionListener menu-item
                         (reify ActionListener
                           (actionPerformed [_ _]
                             (response-fn))))
     (.add menu menu-item)))
  ([^JMenu menu item]
   (condp = item
     :sep (.addSeparator menu))))

;; saving and restoring window shape in preferences -----------------------------------
(defn get-shape [components]
  (for [comp components]
    (condp instance? comp
      Window
      [:window {:x (.getX comp) :y (.getY comp)
                :w (.getWidth comp) :h (.getHeight comp)}]
      JSplitPane
      [:split-pane {:location (.getDividerLocation comp)}]
      nil)))

(defn watch-shape [components fun]
  (doseq [comp components]
    (condp instance? comp
      Window
      (.addComponentListener comp
                             (proxy [ComponentAdapter] []
                               (componentMoved [_] (fun))
                               (componentResized [_] (fun))))
      JSplitPane
      (.addPropertyChangeListener comp JSplitPane/DIVIDER_LOCATION_PROPERTY
                                  (proxy [PropertyChangeListener] []
                                    (propertyChange [_] (fun))))
      nil)))

(defn set-shape [components shape-data]
  (loop [comps components shapes shape-data]
    (let [comp (first comps)
          shape (first shapes)]
      (try
        (when shape
          (condp = (first shape)
            :window
            (let [{:keys [x y w h]} (second shape)]
              (.setBounds comp x y w h))
            :split-pane
            (.setDividerLocation comp (:location (second shape)))
            nil))
        (catch Exception _ nil)))
    (when (next comps)
      (recur (next comps) (next shapes)))))

(defn restore-shape [name components]
  (try
    (set-shape components (prefs/pget name))
    (catch Exception _)))

(defn widget-seq [^Component comp]
  (tree-seq #(instance? Container %)
            #(seq (.getComponents %))
            comp))

(defn persist-window-shape [name ^Window window]
  (let [components (widget-seq window)
        shape-persister (agent nil)]
    (restore-shape name components)
    (watch-shape components
                 #(send-off shape-persister
                            (fn [old-shape]
                              (let [shape (get-shape components)]
                                (when (not= old-shape shape)
                                  (prefs/pput name shape))
                                shape))))))

