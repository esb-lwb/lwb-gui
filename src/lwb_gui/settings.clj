; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

; Settings dialog

(ns lwb-gui.settings
  (:import (javax.swing JFrame JTabbedPane JLabel
                        JPanel JComboBox Box
                        JTextArea
                        BoxLayout SpringLayout
                        JCheckBox JButton)
           (java.awt Font GraphicsEnvironment Dimension FontMetrics Graphics)
           (java.awt.image BufferedImage)
           (java.awt.event ActionListener ItemListener ItemEvent))
  (:require [lwb-gui.utils :as utils]))

(def settings (atom nil))

(defn combo-box [items default-item change-fun]
  (doto (JComboBox. (into-array items))
    (.setSelectedItem default-item)
    (.addActionListener
      (reify ActionListener
        (actionPerformed [_ e]
          (change-fun (.getSelectedItem (.getSource e))))))))

(defn check-box [text checked? change-fun]
  (doto (JCheckBox. ^String text ^boolean checked?)
    (.addItemListener
      (reify ItemListener
        (itemStateChanged [_ e]
          (change-fun
            (=
              (.getStateChange e)
              ItemEvent/SELECTED)))))))

(defn font-panel []
  (let [^Graphics graphics-object (memoize (fn [] (.createGraphics
                                                    (BufferedImage. 1 1 BufferedImage/TYPE_INT_ARGB))))
        monospaced? (fn [font] (let [g (graphics-object)
                                     ^FontMetrics m (.getFontMetrics ^Graphics g ^Font font)]
                                 (apply == (map #(.charWidth m ^char %) [\m \n \. \M \-]))))
        get-all-font-names (fn [] (.. GraphicsEnvironment
                                      getLocalGraphicsEnvironment
                                      getAvailableFontFamilyNames))
        get-all-fonts-12 (fn [] (map #(Font. % Font/PLAIN 12) (get-all-font-names)))
        get-monospaced-font-names (fn [] (map #(.getName %) (filter monospaced? (get-all-fonts-12))))
        get-necessary-fonts (fn [] (if (:show-only-monospaced-fonts @settings)
                                     (get-monospaced-font-names)
                                     (get-all-font-names)))
        example-text-area (doto (JTextArea. "abcdefghijklmnopqrstuvwxyz 0123456789 (){}[]\nABCDEFGHIJKLMNOPQRSTUVWXYZ +-*/= .,;:!? #&$%@|^")
                            (.setFont (Font. (:font-name @settings) Font/PLAIN (:font-size @settings))))
        example-pane (doto (JPanel. (SpringLayout.)) (.add example-text-area))
        ^JComboBox font-box (combo-box
                              (get-necessary-fonts)
                              (:font-name @settings)
                              #(do
                                 (swap! settings assoc :font-name %)
                                 (.setFont
                                   example-text-area
                                   (Font. % Font/PLAIN (:font-size @settings)))))
        ^JComboBox size-box (combo-box
                              (range 5 49)
                              (:font-size @settings)
                              #(do
                                 (swap! settings assoc :font-size %)
                                 (.setFont
                                   example-text-area
                                   (Font. (:font-name @settings) Font/PLAIN %))))
        ^JCheckBox monospaced-check-box (check-box
                                          "Show only monospaced fonts"
                                          (:show-only-monospaced-fonts @settings)
                                          #(do
                                             (swap! settings
                                                    assoc :show-only-monospaced-fonts %)
                                             (doto font-box
                                               (.setModel
                                                 (.getModel
                                                   (JComboBox.
                                                     (into-array
                                                       (get-necessary-fonts)))))
                                               (.setSelectedItem (:font-name @settings)))))
        controls-pane (JPanel.)
        font-pane (JPanel.)]
    (utils/constrain-to-parent example-text-area :n 20 :w 15 :s -15 :e -15)

    (doto controls-pane
      (.setLayout (BoxLayout. controls-pane BoxLayout/X_AXIS))
      (.add (Box/createRigidArea (Dimension. 20 0)))
      (.add (JLabel. "Font:"))
      (.add (Box/createRigidArea (Dimension. 25 0)))
      (.add font-box)
      (.add (Box/createRigidArea (Dimension. 25 0)))
      (.add (JLabel. "Size:"))
      (.add (Box/createRigidArea (Dimension. 25 0)))
      (.add size-box)
      (.add (Box/createHorizontalGlue)))

    (doto font-pane
      (.setLayout (BoxLayout. font-pane BoxLayout/Y_AXIS))
      (.add controls-pane)
      (.add monospaced-check-box)
      (.add example-pane))))

(defn editor-options-panel []
  (let [options-pane (JPanel.)]
    (doto options-pane
      (.setLayout (BoxLayout. options-pane BoxLayout/Y_AXIS))
      (.add ^JCheckBox (check-box
                         "Wrap lines in source editor"
                         (:line-wrap-doc @settings)
                         #(swap! settings assoc :line-wrap-doc %)))
      (.add ^JCheckBox (check-box
                         "Wrap lines in repl output"
                         (:line-wrap-repl-out @settings)
                         #(swap! settings assoc :line-wrap-repl-out %))))))

(defmacro tabs [& elements]
  `(doto (JTabbedPane.)
     ~@(map #(list '.addTab (first %) (second %)) elements)))

(defn make-settings-window [app apply-fn]
  (let [bounds (.getBounds (:frame app))
        x (+ (.x bounds) (/ (.width bounds) 2))
        y (+ (.y bounds) (/ (.height bounds) 2))
        settings-frame (JFrame. "Settings")
        button-pane (JPanel.)]

    (doto button-pane
      (.setLayout (BoxLayout. button-pane BoxLayout/X_AXIS))
      (.add ^JButton (utils/create-button "OK" #(do
                                                  (apply-fn app @settings)
                                                  (.dispose settings-frame))))
      (.add ^JButton (utils/create-button "Apply" #(apply-fn app @settings)))
      (.add ^JButton (utils/create-button "Cancel" #(.dispose settings-frame))))

    (doto settings-frame
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
      (.setLayout (BoxLayout. (.getContentPane settings-frame) BoxLayout/Y_AXIS))
      (.setBounds (- x 250) (- y 250) 500 300)

      (.add (tabs
              ["Font" (font-panel)]
              ["Editor options" (editor-options-panel)]))
      (.add (Box/createRigidArea (Dimension. 0 25)))
      (.add button-pane))))

(defn show-settings-window [app apply-fn]
  (reset! settings @(:settings app))
  (.show (make-settings-window app apply-fn)))
