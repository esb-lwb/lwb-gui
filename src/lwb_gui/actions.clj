; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

; Menu actions

(ns lwb-gui.actions
  (:require [lwb-gui.consts :as consts]
            [lwb-gui.prefs :as prefs]
            [seesaw.core :as sc]
            [clojure.java.browse :as browse]
            [lwb-gui.repl.control :as repl]
            [lwb-gui.utils :as utils]
            [lwb-gui.indent :as indent])
  (:import (org.fife.ui.rsyntaxtextarea TextEditorPane FileLocation)
           (javax.swing JOptionPane JFileChooser)
           (javax.swing.filechooser FileNameExtensionFilter)
           (java.io File)
           (org.fife.ui.utils RecentFilesMenu)
           (javax.swing.text JTextComponent)))

;; Sessions ---------------------------------------------------------------------
(defn session-state [^TextEditorPane edit-area]
  (cond
    (not (.isDirty edit-area)) :clean
    (and (.isDirty edit-area) (.isLocalAndExists edit-area)) :save
    (and (.isDirty edit-area) (not (.isLocalAndExists edit-area))) :save-as))

(defn set-edit-label [app]
  (let [edit-area (:edit-area app)
        path (.getFileFullPath edit-area)
        fn (if (.endsWith path "Untitled.txt")
             "<unsaved session>"
             path)]
    (.setText (:edit-label app) (str "Editor - " fn))))

(defn overwrite?
  "Overwrite file fx if fx exists?
   Returns true if the file does not exists."
  [app fx]
  (if (.exists fx)
    (let [rc (JOptionPane/showConfirmDialog (:frame app)
                                            "File exists, overwrite?" nil JOptionPane/YES_NO_OPTION)]
      (if (= rc JOptionPane/YES_OPTION)
        true
        false))
    true))

(defn save-as-file
  "Save as... dialog, returns true if saved, false otherwise"
  [app]
  (let [fi (FileNameExtensionFilter. "Clojure Files" (into-array String ["clj"]))
        fc (doto (JFileChooser. ^String (prefs/pget "current-dir")) (.setFileFilter fi))
        rc (.showSaveDialog fc (:frame app))]
    (if (= rc JFileChooser/APPROVE_OPTION)
      (let [^File f (.getSelectedFile fc)
            fn (.getAbsolutePath f)
            fx (if (not (.endsWith fn ".clj")) (File. (str fn ".clj")) f)
            fp (.getParent fx)
            fl (FileLocation/create fx)]
        (prefs/pput "current-dir" fp)
        (if (overwrite? app fx)
          (try
            (.saveAs (:edit-area app) fl)
            (.addFileToFileHistory (:recent-menu app) (.getFileFullPath fl))
            (set-edit-label app)
            true
            (catch Exception _ false))
          false))
      false)))

(defn save-file
  "Clean or Save or Save as... dialog, 
   returns true if okay, false otherwise"
  [app]
  (let [^TextEditorPane edit-area (:edit-area app)
        state (session-state edit-area)]
    (case state
      :clean true
      :save (do (.save edit-area) true)
      :save-as (save-as-file app))))

(defn close-session [app]
  (let [^TextEditorPane edit-area (:edit-area app)
        state (session-state edit-area)
        closable? (if (not (= state :clean))
                    (let [rc (JOptionPane/showConfirmDialog (:frame app) "Save session?")]
                      (condp = rc
                        JOptionPane/OK_OPTION (if (save-file app) true false)
                        JOptionPane/NO_OPTION true
                        JOptionPane/CANCEL_OPTION false))
                    true)]
    (if closable?
      (do
        (.reInit edit-area)                                 ; A hack in TextEditorPane.java
        (.setText (:edit-label app) "Editor - <unsaved session>")
        true)
      false)))

(defn new-session [app text]
  (let [^TextEditorPane edit-area (:edit-area app)]
    (if (close-session app)
      (do
        (.setText edit-area text)
        (repl/send-to-repl app text true)
        (.requestFocus edit-area)))))


(defn open-file [app]
  (if (close-session app)
    (let [fi (FileNameExtensionFilter. "Clojure Files" (into-array String ["clj"]))
          fc (doto (JFileChooser. ^String (prefs/pget "current-dir")) (.setFileFilter fi))
          rc (.showOpenDialog fc (:frame app))]
      (if (= rc JFileChooser/APPROVE_OPTION)
        (let [^File f (.getSelectedFile fc)
              fp (.getParent f)
              fl (FileLocation/create f)]
          (prefs/pput "current-dir" fp)
          (.load (:edit-area app) fl "UTF-8")))
      (.addFileToFileHistory ^RecentFilesMenu (:recent-menu app) (.getFileFullPath (:edit-area app)))
      (set-edit-label app))))

(defn open-recent-file [app ^String path]
  (if (close-session app)
    (let [fl (FileLocation/create path)]
      (if (.isLocalAndExists fl)
        (do
          (.load (:edit-area app) fl "UTF-8")
          (.setText (:edit-label app) (str "Editor - " (.getFileName (:edit-area app)))))
        (JOptionPane/showMessageDialog (:frame app) (str path " doesn't exist anymore."))))))

; TODO: info is just for testing, delete later
(defn info [app]
  (JOptionPane/showMessageDialog nil (str "Info " (session-state (:edit-area app))))
  (JOptionPane/showMessageDialog nil (str "Info " (.getFileName (:edit-area app)))))

;; Edit -------------------------------------------------------------------------
(defn comment-out [text-comp]
  (utils/insert-in-selected-row-headers text-comp ";"))

(defn uncomment-out [text-comp]
  (utils/remove-from-selected-row-headers text-comp ";"))

(defn toggle-comment [^JTextComponent text-comp]
  (if (= (.getText (.getDocument text-comp)
                   (first (utils/get-selected-line-starts text-comp)) 1)
         ";")
    (uncomment-out text-comp)
    (comment-out text-comp)))

(defn fix-indent-selected-lines [text-comp]
  (utils/awt-event
    (dorun (map #(indent/fix-indent text-comp %)
                (utils/get-selected-lines text-comp)))))

(defn indent [text-comp]
  (when (.isFocusOwner text-comp)
    (utils/insert-in-selected-row-headers text-comp "  ")))

(defn unindent [text-comp]
  (when (.isFocusOwner text-comp)
    (utils/remove-from-selected-row-headers text-comp "  ")))

(defn setup-autoindent [text-comp]
  (utils/attach-action-keys text-comp
                            ["cmd1 shift I" #(fix-indent-selected-lines text-comp)]
                            ["cmd1 shift RIGHT" #(indent text-comp)]
                            ["cmd1 shift LEFT" #(unindent text-comp)]))

;; Actions for Search: see search.clj

;; REPL -------------------------------------------------------------------------

;; Actions for REPL: see repl/control.clj

;; Options ----------------------------------------------------------------------

;; Manual -----------------------------------------------------------------------
(defn man [topic]
  (browse/browse-url (str consts/lwb-wiki topic)))

(defn about-dlg []
  (-> (sc/dialog :title "About lwb-gui" :size [400 :by 360] :content consts/about)
      (sc/pack!)
      (sc/show!)))

;; Exit ------------------------------------------------------------------------
(defn exit [app]
  ; Close current session
  (let [^TextEditorPane edit-area (:edit-area app)
        state (session-state edit-area)]
    (if (not (= state :clean))
      (let [rc (JOptionPane/showConfirmDialog (:frame app) "Save session?"
                                              "Select Option" JOptionPane/YES_NO_OPTION)]
        (if (= rc JOptionPane/OK_OPTION)
          (save-file app)))))
  ; Save history of recent files
  (prefs/pput "recent-files" (.getFileHistory (:recent-menu app)))

  )
