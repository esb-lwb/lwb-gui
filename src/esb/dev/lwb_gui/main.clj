(ns esb.dev.lwb-gui.main
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [seesaw.color :as color]
            [seesaw.reditpane :as reditpane]
            [seesaw.keymap :refer [map-key]]
            [seesaw.keystroke :refer [keystroke]]
            [esb.dev.lwb-gui.menu :as menu]
            [esb.dev.lwb-gui.prefs :as prefs]
            [esb.dev.lwb-gui.search :as search]
            [esb.dev.lwb-gui.actions :as actions]
            [esb.dev.lwb-gui.settings :as settings]
            [esb.dev.lwb-gui.consts :as consts])
  (:import (org.fife.ui.utils SmartScroller RecentFilesMenu)
           (javax.swing JFrame)
           (java.awt.event WindowAdapter)
           (java.awt Desktop)
           (java.awt.desktop QuitHandler QuitResponse AboutHandler)
           (org.fife.ui.rsyntaxtextarea TextEditorPane))
  (:gen-class))

; Searchbar
(def search-bar
  (horizontal-panel
    :id :search-bar
    :border (line-border :color :blue )
    :items [[:fill-h 4]
            (button :id :search-close :text "×" :size [18 :by 18] :halign :center :valign :center
                    :listen [:action (fn [e] (actions/stop-find (to-root e)))]) 
            (text :id :search-text :text "") 
            (checkbox :id :search-case :text "Match Case")
            (checkbox :id :search-regex :text "Regex")]))

(defn adapt-search-bar
  "Key mapping for the searchbar"
  [sb]
  (map-key sb "ENTER" (fn [e] (search/highlight-step e false)) :scope :descendants)
  (map-key sb "shift ENTER" (fn [e] (search/highlight-step e true)) :scope :descendants)
  (map-key sb "ESCAPE" (fn [e] (actions/escape-find (to-root e))) :scope :descendants))
   
; make-frame needs the reference to our rtextarea, because the edit actions in RTextArea
; are created _after_ initialising. I.e. the rtextarea must be created before the menu
(defn make-frame
  "Makes the main window of the app."
  [rtextarea]
  (frame
    :title "Logic WorkBench GUI"
    :width 800 :height 600
    :menubar (menu/make-menubar)
    :content
    (border-panel :vgap 5
                  :center (left-right-split
                            (border-panel
                              :vgap 5
                              :border 10
                              :north (label :id :edit-label :text "Unsaved session")
                              :center (rscrollable rtextarea
                                                   :id :edit-scroll)
                              :south search-bar)
                            (border-panel
                              :vgap 5
                              :border 10
                              :north (label :id :repl-label :text "REPL Output")
                              :center (scrollable
                                        (text :id :repl-area :multi-line? true :editable? false)
                                        :id :repl-scroll))
                            :divider-location (or (prefs/get-divider-location) 0.5)))))

(defn load-recent-files
  "Populates the recent files menu from the preferences store."
  [root]
  (let [hl @prefs/recent-files]
    (if (some? hl)
      (dorun (for [f (reverse hl)]
               (.addFileToFileHistory ^RecentFilesMenu (select root [:#recent-menu]) f))))))

(defn mac-menu
  "About and Quit handler for the L&F of a MacApp."
  [frame]
  (let [mac-app (Desktop/getDesktop)]
    (.setQuitHandler mac-app (proxy [QuitHandler] []
                               (handleQuitRequestWith [_ ^QuitResponse response]
                                 (actions/exit frame)
                                 (.performQuit response))))
    (.setAboutHandler mac-app (proxy [AboutHandler] []
                                (handleAbout [_]
                                  (actions/about-dlg frame))))))

(defn exit-if-closed
  "Exit when closing the main window."
  [^JFrame f]
  (.addWindowListener f
                      (proxy [WindowAdapter] []
                        (windowClosing [_]
                          (actions/exit' f)))))

(defn adapt-reditpane
  "Settings for the edit area."
  [r]
  (.setCurrentLineHighlightColor ^TextEditorPane r (color/color :whitesmoke)))

(defn map-keys
  "Key mapping for the main window."
  [f]
  (map-key f "menu PLUS" (fn [e] (actions/inc-font-size e) :global))
  (map-key f "menu MINUS" (fn [e] (actions/dec-font-size e) :global))
  (map-key f "menu COMMA" (fn [e] (actions/toggle-comment e))))

(defn make-app
  "Makes the map"
  []
  (invoke-now
    (let [r (reditpane/text-area :id :edit-area :syntax :clojure) ;; see comment above
          f (make-frame r)]
      (when (consts/is-mac) (mac-menu f))
      (prefs/set-frame-size f)
      (adapt-reditpane r)
      (adapt-search-bar (select f [:#search-bar]))
      (hide! (select f [:#search-bar]))
      (map-keys f)
      (SmartScroller. (select f [:#repl-scroll]))
      (settings/apply-settings f)
      (load-recent-files f)
      (actions/start-repl f)
      f)))

(defn -main
  "Entry point for the app."
  []
  (native!)
  (let [f (make-app)]
    (exit-if-closed f)
    (show! f)))

