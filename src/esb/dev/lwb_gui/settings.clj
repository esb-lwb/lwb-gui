; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0

(ns esb.dev.lwb-gui.settings
  (:require [seesaw.core :refer :all]
            [seesaw.font :as font]
            [seesaw.bind :as bind]
            [seesaw.mig :refer [mig-panel]]
            [esb.dev.lwb-gui.prefs :as prefs])
  (:import (javax.swing JDialog JTextArea)
           (java.awt.font FontRenderContext)
           (java.awt.geom Rectangle2D)
           (java.awt Font RenderingHints Rectangle)
           (org.fife.ui.rtextarea RTextScrollPane)
           (org.fife.ui.rsyntaxtextarea TextEditorPane)))

; Consts
(def min-font-size 6)
(def max-font-size 24)

(defn apply-settings
  "Apply settings to the app."
  [f]
  (let [^RTextScrollPane edit-scroll (select f [:#edit-scroll])
        ^TextEditorPane edit-area (select f [:#edit-area])
        ^JTextArea repl-area (select f [:#repl-area])
        settings @prefs/settings
        font (font/font :name (:font-name settings) :size (:font-size settings))]
    (.setLineNumbersEnabled edit-scroll (:edit-lino settings))
    (.setLineWrap edit-area (:edit-wrap settings))
    (.setTabSize edit-area (:edit-tabsize settings))
    (.setFont edit-area font)
    (.setLineWrap repl-area (:repl-wrap settings))
    (.setFont repl-area font)))

(defn persist-settings
  "Apply and persist the settings in the preference store."
  [frame p]
  (reset! prefs/settings {:font-name    (selection (select p [:#font-name]))
                          :font-size    (selection (select p [:#font-size]))
                          :edit-wrap    (selection (select p [:#edit-wrap]))
                          :edit-lino    (selection (select p [:#edit-lino]))
                          :edit-tabsize (selection (select p [:#edit-tabsize]))
                          :repl-wrap    (selection (select p [:#repl-wrap]))})
  (apply-settings frame))

(defn monospaced?
  "Is the font with font-name monospaced?"
  [font-name]
  (let [^Font font (font/font font-name)
        fctx (FontRenderContext. nil
                                 RenderingHints/VALUE_TEXT_ANTIALIAS_DEFAULT
                                 RenderingHints/VALUE_FRACTIONALMETRICS_DEFAULT)
        ^Rectangle2D i-bounds (.getStringBounds font "ij" fctx)
        ^Rectangle2D m-bounds (.getStringBounds font "mw" fctx)]
    (= (.getWidth i-bounds) (.getWidth m-bounds))))

(defn font-names
  "Seq of the names of monospaced fonts."
  []
  (filter monospaced? (font/font-families)))

(defn make-settings-dlg
  "Constructs the settings dialog."
  [frame]
  (let [d (dialog frame
                  :id :settings
                  ; :parent frame It is not possible to set the parent like in (alert ...)
                  ; i.e. the Apple menu disappears, when the dialog is shown!
                  ; A fix is not easy, needs a patch of the macro construct in seesaw
                  ; made a horrible hack!! used JDialog. instead of (construct JDialog)
                  ; See remarks in the patched seesaw project
                  :modal? true
                  :title "Settings"
                  :resizable? false
                  :option-type :ok-cancel
                  :success-fn (fn [p] (persist-settings frame p))
                  :cancel-fn (fn [_] nil)
                  :content (mig-panel :constraints ["", "[300!][300!]", "[200!][140!]"]
                                      :items [[(mig-panel :id :font-opts :border "Font options" :constraints ["", "[38!][360!][50!][98!]", "[36!][50!][50!]"]
                                                          :items [[(label "Font") "cell 0 0, align right, gapright 4"]
                                                                  [(combobox :id :font-name :model (font-names) :selected-item (:font-name @prefs/settings))
                                                                   "cell 1 0"]
                                                                  [(label "Fontsize") "cell 2 0, align right, gapright 12"]
                                                                  [(combobox :id :font-size :model (range min-font-size (inc max-font-size)) :selected-item (:font-size @prefs/settings)) "cell 3 0, gapleft 8"]
                                                                  ; Two text boxes, because a multi-line text results in an exception. Seems to be a problem with mig layout, but I#m not sure.
                                                                  [(text :id :text-ex1 :size [540 :by 48] :editable? false :text "The quick brown fox jumps over the lazy dog"
                                                                         :font (font/font :name (:font-name @prefs/settings) :size (:font-size @prefs/settings))) "cell 0 1 4 1"]
                                                                  [(text :id :text-ex2 :size [540 :by 48] :editable? false :text "0123456789 (){}[])]+-*/= .,;:!? #&$%@|^\""
                                                                         :font (font/font :name (:font-name @prefs/settings) :size (:font-size @prefs/settings))) "cell 0 2 4 1"]
                                                                  ]) "cell 0 0 2 1"]
                                              [(mig-panel :id :edit-opts :border "Editor options" :size [296 :by 140]
                                                          :items [[(label "Wrap Lines?") "align right"]
                                                                  [(checkbox :id :edit-wrap :selected? (:edit-wrap @prefs/settings)) "wrap"]
                                                                  [(label "Show Line Numbers?") "align right"]
                                                                  [(checkbox :id :edit-lino :selected? (:edit-lino @prefs/settings)) "wrap"]
                                                                  [(label "Tabsize") "align right"]
                                                                  [(combobox :id :edit-tabsize :model (range 2 9) :selected-item (:edit-tabsize @prefs/settings))]
                                                                  ]) "cell 0 1"]
                                              [(mig-panel :id :repl-opts :border "REPL output options" :size [296 :by 140]
                                                          :items [[(label "       Wrap Lines?") "align right"]
                                                                  [(checkbox :id :repl-wrap :selected? (:repl-wrap @prefs/settings))]
                                                                  ]) "cell 1 1"]]))]
    (let [fn (select d [:#font-name])
          fs (select d [:#font-size])
          t1 (select d [:#text-ex1])
          t2 (select d [:#text-ex2])]
      (bind/bind (bind/selection fn) (bind/transform #(font/font :name % :size (value fs))) (bind/property t1 :font))
      (bind/bind (bind/selection fn) (bind/transform #(font/font :name % :size (value fs))) (bind/property t2 :font))
      (bind/bind (bind/selection fs) (bind/transform #(font/font :name (value fn) :size %)) (bind/property t1 :font))
      (bind/bind (bind/selection fs) (bind/transform #(font/font :name (value fn) :size %)) (bind/property t2 :font)))
    d))

(defn display-settings
  "Displays the settings dialog."
  [f]
  (let [^JDialog dlg (make-settings-dlg f)
        ^Rectangle fbounds (.getBounds f)]
    (.setLocation dlg (+ (.x fbounds) (/ (- (.width fbounds) 600) 2)) (+ (.y fbounds) (/ (- (.height fbounds) 440) 2)))
    (pack! dlg)
    (show! dlg)))

(defn update-font-size
  "Update the font-size in edit and repl area by function inc or dec."
  [f func]
  (let [edit-area (select f [:#edit-area])
        repl-area (select f [:#repl-area])
        settings (update-in @prefs/settings [:font-size] func)
        font (font/font :name (:font-name settings) :size (:font-size settings))]
    (reset! prefs/settings settings)
    (.setFont edit-area font)
    (.setFont repl-area font)))

(defn font-size
  "Update the font-size in edit and repl area by function inc or dec,
   after check of current font size."
  [f func]
  (let [current-font-size (:font-size @prefs/settings)]
    (if (or (and (= func dec) (= current-font-size min-font-size)) 
            (and (= func inc) (= current-font-size max-font-size)))
      nil                                                   ;do nothing
      (update-font-size f func))))
