; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0

(ns esb.dev.lwb-gui.actions
  (:require [esb.dev.lwb-gui.consts :as consts]
            [esb.dev.lwb-gui.repl :as repl]
            [esb.dev.lwb-gui.sexpr :as sexpr]
            [esb.dev.lwb-gui.prefs :as prefs]
            [esb.dev.lwb-gui.settings :as settings]
            [clojure.java.browse :as browse]
            [seesaw.chooser :refer [choose-file]]
            [seesaw.core :refer :all]
            [esb.dev.lwb-gui.search :as search])
  (:import (org.fife.ui.rsyntaxtextarea TextEditorPane 
                                        FileLocation 
                                        RSyntaxTextAreaEditorKit$ToggleCommentAction 
                                        RSyntaxTextAreaEditorKit$InsertTabAction 
                                        RSyntaxTextAreaEditorKit$DecreaseIndentAction)
           (java.io File)
           (org.fife.ui.utils RecentFilesMenu Toast)
           (javax.swing.text Utilities)))

;; Session
(defn session-state 
  "In which state is the current session?"
  [^TextEditorPane edit-area]
  (cond
    (not (.isDirty edit-area)) :clean
    (and (.isDirty edit-area) (.isLocalAndExists edit-area)) :save
    (and (.isDirty edit-area) (not (.isLocalAndExists edit-area))) :save-as))

(defn set-edit-label 
  "Sets the labels of the current session."
  [root]
  (let [edit-area (select root [:#edit-area])
        path (.getFileFullPath edit-area)
        fn (if (.endsWith path "Untitled.txt") "<unsaved session>" path)
        edit-label (select root [:#edit-label])]
    (text! edit-label (str "Editor - " fn))))

(defn overwrite?
  "Overwrite file fx if fx exists?
   Returns true if the file does not exists."
  [root  ^File fx]
  (if (.exists fx)
    (confirm root "File exists, overwrite?" :option-type :yes-no :type :question :icon consts/icon-64)
    true))

(defn save-as-file
  "Save as... dialog, returns true if saved, false otherwise"
  [root]
  (let [f (choose-file :all-files? false?
                       :type :save
                       :filters [["Clojure Files" ["clj"]]]
                       :dir @prefs/current-dir)]
    (if (nil? f)  ;CANCEL
      false
      (let [fn (.getAbsolutePath f)
            fx ^File (if (not (.endsWith fn ".clj")) (File. ^String (str fn ".clj")) f)
            fp (.getParent fx)
            fl (FileLocation/create fx)]
        (reset! prefs/current-dir fp)
        (if (overwrite? root fx)
          (try
            (.saveAs (select root [:#edit-area]) fl)
            (.addFileToFileHistory ^RecentFilesMenu (select root [:#recent-menu]) (.getFileFullPath fl))
            (set-edit-label root)
            true
            (catch Exception _ false))
          false)))))

(defn save-file
  "Clean or Save or Save as... dialog, 
   returns true if okay, false otherwise"
  [root]
  (let [^TextEditorPane edit-area (select root [:#edit-area])
        state (session-state edit-area)]
    (case state
      :clean true
      :save (do (.save edit-area) 
                (Toast/displayToast root "Saved")
                true)
      :save-as (save-as-file root))))

(defn close-session 
  "Close session, without closing the app."
  [root]
  (let [^TextEditorPane edit-area (select root [:#edit-area])
        edit-label (select root [:#edit-label])
        state (session-state edit-area)
        closable? (if (not (= state :clean))
                    (let [rc (confirm root "Save session?" :option-type :yes-no-cancel :type :warning :icon consts/icon-64)]
                      (condp = rc
                        true (if (save-file root) true false)
                        false true
                        nil false))
                    true)]
    (if closable?
      (do
        (.reInit edit-area)                                 ; A hack in TextEditorPane.java
        (text! edit-label "Editor - <unsaved session>")
        true)
      false)))

(declare restart-repl)
(declare send-to-repl)

(defn new-session 
  "A new session with the given text."
  [root text]
  (let [edit-area (select root [:#edit-area])]
    (if (close-session root)
      (do
        (restart-repl root true)
        (text! edit-area text)
        (send-to-repl root text true)
        (.requestFocus edit-area)))))

(defn open-file 
  "Opens file chooser."
  [root]
  (if (close-session root)
    (let [f (choose-file :all-files? false?
                         :filters [["Clojure Files" ["clj"]]]
                         :dir @prefs/current-dir)]
      (if f
        (let [fp (.getParent ^File f)
              fl (FileLocation/create ^File f)]
          (reset! prefs/current-dir fp)
          (.load (select root [:#edit-area]) fl "UTF-8")
          (.addFileToFileHistory ^RecentFilesMenu (select root [:#recent-menu]) (.getFileFullPath fl))
          (restart-repl root true)))
      (set-edit-label root))))

(defn open-recent-file 
  "Opens the file with the given filepath."
  [e ^String path]
  (let [root (to-root e)
        edit-area (select root [:#edit-area])]
  (if (close-session root)
    (let [fl (FileLocation/create path)]
      (if (.isLocalAndExists fl)
        (do
          (.load edit-area fl "UTF-8")
          (restart-repl root true)
          (set-edit-label root))
        (alert root (str path " doesn't exist anymore.") :type :info :icon consts/icon-64))))))

;; Edit
(defn toggle-comment 
  "Toggles comment"
  [e]
  (let [root (to-root e)
        edit-area (select root [:#edit-area])]
  (.actionPerformedImpl (RSyntaxTextAreaEditorKit$ToggleCommentAction.) e edit-area)))

(defn indent 
  "Indents selected lines."
  [e]
  (let [root (to-root e)
        edit-area (select root [:#edit-area])]
    (let [dot (.getSelectionStart edit-area)
          mrk (.getSelectionEnd edit-area)] 
      (if (= dot mrk)
        (.setCaretPosition edit-area (Utilities/getRowStart edit-area dot)))
    (.actionPerformedImpl (RSyntaxTextAreaEditorKit$InsertTabAction.) e edit-area))))

(defn unindent
  "Unindents selected lines."
  [e]
  (let [root (to-root e)
        edit-area (select root [:#edit-area])]
    (.actionPerformedImpl (RSyntaxTextAreaEditorKit$DecreaseIndentAction.) e edit-area)))

(defn go-to-line 
  "Shows dialog for line numbers and goes to the given line number."
  [e]
  (let [root (to-root e)
        edit-area (select root [:#edit-area])
        current-line (fn [] (inc (.getLineOfOffset edit-area (.getCaretPosition edit-area))))
        line-str ^String (input root "Line number:" :title "Go to Line" :type :question :icon consts/icon-64)
        line-num (Integer. ^int (if (or (nil? line-str) (nil? (re-find #"\d+" line-str)))
                                  (current-line)
                                  (re-find #"\d+" line-str)))]
    (try
      (.setCaretPosition edit-area (.getLineStartOffset edit-area (dec line-num)))
      (.requestFocus edit-area)
      (catch Exception _ nil))))  ; do nothing when line number points to bad location

(defn start-find 
  "Starts find by showing the searchbar and putting selected text into the searchfield."
  [root]
  (let [search-bar (select root [:#search-bar])
        search-text (select root [:#search-text])
        edit-area (select root [:#edit-area])
        sel-text (.getSelectedText edit-area)]
    (show! search-bar)
    (.requestFocus search-text)
    (if (not (empty? sel-text))
      (text! search-text sel-text))))

(defn find-next 
  "Starts searching and/or goes to next found position."
  [e]
    (start-find (to-root e))
    (search/highlight-step e false))

(defn find-prev
  "Starts searching and/or goes to previous found position."
  [e]
  (start-find (to-root e))
  (search/highlight-step e true))

(defn stop-find 
  "Stops searching and hides searchbar."
  [root]
  (let [search-bar (select root [:#search-bar])
        edit-area (select root [:#edit-area])]
    (hide! search-bar)
    (search/remove-highlights edit-area @search/search-highlights)
    (reset! search/search-highlights nil)
    (reset! search/current-pos 0)))

(defn escape-find
  "Stops searching and hides searchbar."
  [root]
  (stop-find root)
  (.requestFocus (select root [:#edit-area])))

;; REPL
(defn send-to-repl
  "Sends command to REPL.
   If silent? the command is not shown in the REPL, just the result of the evaluation."
  [root cmd silent?]
  (let [repl-area (select root [:#repl-area])
        cmd-ln (str cmd \newline)]
    (when-not silent?
      (.append repl-area cmd-ln))
    (when-let [repl-map @repl/repl-state]
      (repl/evaluate repl-map cmd))))

(defn send-selected-to-repl
  "Sends selected text or current sexpression to the REPL."
  [root]
  (let [edit-area (select root [:#edit-area])
        repl-area (select root [:#repl-area])
        txt (if-let [text (.getSelectedText edit-area)]
              text
              (sexpr/get-current-sexpr edit-area))]
    (if (or (nil? txt) (not (sexpr/parens-balanced? txt)))
      (.append repl-area "Not a well-formed sexpression!\n") ;; error!
      (send-to-repl root txt false))))

(defn send-top-sexpr-to-repl
  "Sends top sexpression to the REPL."
  [root]
  (let [edit-area (select root [:#edit-area])
        repl-area (select root [:#repl-area])
        txt (sexpr/get-top-sexpr edit-area)]
    (if (nil? txt)
      (.append repl-area "Not a well-formed sexpression!\n") ;; error!
      (send-to-repl root txt false))))

(defn send-file-to-repl
  "Sends the whole content of the edit area to the REPL."
  [root]
  (let [edit-area (select root [:#edit-area])
        repl-area (select root [:#repl-area])
        text (.getText ^TextEditorPane edit-area)]
    (.append repl-area "Evaluating file...\n")
    (send-to-repl root text true)))

(defn start-repl
  "Starts REPL."
  [root]
  (let [repl-area (select root [:#repl-area])]
    (.append repl-area (str "--- This is the Clojure REPL ---\n"))
    (let [repl-map (repl/launch-repl repl-area)]
      (reset! repl/repl-state repl-map))))

(defn stop-repl
  "Stops REPL."
  [root]
  (let [repl-area (select root [:#repl-area])]
    (.append repl-area "\n--- Bye, shutdown of Clojure REPL  ---\n")
    (when-let [repl-map @repl/repl-state] 
      (repl/close repl-map))))

(defn restart-repl
  "Restarts REPL. If clear? clears output."
  ([root]
   (restart-repl root false))
  ([root clear?]
   (stop-repl root)
   (when clear?
     (text! (select root [:#repl-area]) ""))
   (start-repl root)))

(defn clear-repl-area 
  "Clears output area."
  [root]
  (text! (select root [:#repl-area]) ""))

;; Options

(defn inc-font-size 
  "Increase font size."
  [e]
  (settings/font-size (to-root e) inc))

(defn dec-font-size
  "Decrease font size."
  [e]
  (settings/font-size (to-root e) dec))

(defn settings 
  "Starts and handles settings dialog."
  [e]
  (settings/display-settings (to-root e)))

;; Manual
(defn man 
  "Show topic in wiki of the Logic Workbench on github."
  [topic]
  (browse/browse-url (str consts/lwb-wiki topic)))

;; About
(defn about-dlg 
  "Show About dialog."
  [root]
  (alert root consts/about :title "About lwb-gui" :type :info :icon consts/icon-128))

;; Exit
(defn exit 
  "Exit app."
  [root]
  ; Close current session
  (let [^TextEditorPane edit-area (select root [:#edit-area])
        state (session-state edit-area)]
    (if (not (= state :clean))
      (if (confirm root "Save session?" :option-type :yes-no :type :question :icon consts/icon-64)
        (save-file root)))
    (reset! prefs/recent-files (.getFileHistory (select root [:#recent-menu])))
    (prefs/store-shapes root)))
      
(defn exit'
  "Exit app."
  [root]
  ; exit with System.exit
  (exit root)
  (System/exit 0))