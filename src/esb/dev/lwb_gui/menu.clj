; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0

(ns esb.dev.lwb-gui.menu
  (:require [seesaw.core :refer :all]
            [seesaw.border :refer :all]
            [esb.dev.lwb-gui.actions :as actions]
            [esb.dev.lwb-gui.consts :as consts])
  (:import (org.fife.ui.utils RecentFilesMenu)
           (javax.swing AbstractAction)
           (org.fife.ui.rtextarea RTextArea)))

;; Actions in menu Main
(def about-action (action
                    :handler (fn [e] (actions/about-dlg (to-root e)))
                    :name "About"))

(def exit-action (action
                    :handler (fn [e] (actions/exit' (to-root e)))
                    :key "alt F4"
                    :name "Exit"))

;; Actions in submenu New
(def prop-action (action
                     :handler (fn [e] (actions/new-session (to-root e) consts/new-prop))
                     :name "Propositional Logic"))
(def pred-action (action
                     :handler (fn [e] (actions/new-session (to-root e) consts/new-pred))
                     :name "Predicate Logic"))
(def ltl-action (action
                     :handler (fn [e] (actions/new-session (to-root e) consts/new-ltl))
                     :name "Linear Temporal Logic"))
(def nd-action (action
                     :handler (fn [e] (actions/new-session (to-root e) consts/new-nd))
                     :name "Natural Deduction"))
(def cl-action (action
                     :handler (fn [e] (actions/new-session (to-root e) consts/new-cl))
                     :name "Combinatory Logic"))

;; Items in menu Session
(def new-menu (menu  :text "New"
                     :items [prop-action pred-action ltl-action nd-action cl-action]))

(def open-action (action
                     :handler (fn [e] (actions/open-file (to-root e)))
                     :key "menu O"
                     :name "Open"))

(def save-action (action
                     :handler (fn [e] (actions/save-file (to-root e)))
                     :key "menu S"
                     :name "Save"))

(def save-as-action (action 
                     :handler (fn [e] (actions/save-as-file (to-root e)))
                     :key "shift menu S"
                     :name "Save as..."))

(def close-action (action
                     :handler (fn [e] (actions/close-session (to-root e)))
                     :name "Close"))

(def recent-menu
  (let [rm (doto (proxy [RecentFilesMenu] ["Open Recent"]
                   (createOpenAction [^String path]
                     (proxy [AbstractAction] [path]
                       (actionPerformed [e]
                         (actions/open-recent-file e path))))
                   (getShouldIgnoreFile [path]
                     (if (.endsWith path "Untitled.txt")
                       true
                       false)))
             (.setMaximumFileHistorySize 10))]
    (config! rm :id :recent-menu)))


;; Items in menu Edit
;  Actions from RTextArea see below
(def comment-action (action
                     :handler (fn [e] (actions/toggle-comment e))
                     :key "menu COMMA"
                     :name "Comment"))

(def indent-action (action
                     :handler (fn [e] (actions/indent e))
                     :key "alt menu I"
                     :name "Indent lines"))

(def unindent-action (action
                     :handler (fn [e] (actions/unindent e))
                     :key "alt menu U"
                     :name "Unindent lines"))

(def goto-action (action
                     :handler (fn [e] (actions/go-to-line e))
                     :key "menu L"
                     :name "Go to line..."))

(def find-action (action
                     :handler (fn [e] (actions/start-find (to-root e)))
                     :key "menu F"
                     :name "Find"))

(def next-action (action
                     :handler (fn [e] (actions/find-next e))
                     :key "menu G"
                     :name "Find next"))

(def prev-action (action
                     :handler (fn [e] (actions/find-prev e))
                     :key "alt menu G"
                     :name "Find previous"))

;; Items in menu REPL
(def curr-sexpr-action (action
                     :handler (fn [e] (actions/send-selected-to-repl (to-root e)))
                     :key "ctrl shift C"
                     :name "Evaluate current sexpression"))
(def top-sexpr-action (action
                     :handler (fn [e] (actions/send-top-sexpr-to-repl (to-root e)))
                     :key "ctrl shift T"
                     :name "Evaluate top sexpression"))
(def file-action (action
                     :handler (fn [e] (actions/send-file-to-repl (to-root e)))
                     :key "ctrl shift F"
                     :name "Evaluate entire file"))
(def clear-action (action
                     :handler (fn [e] (actions/clear-repl-area (to-root e)))
                     :key "ctrl shift O"
                     :name "Clear output"))
(def restart-action (action
                     :handler (fn [e] (actions/restart-repl (to-root e)))
                     :key "ctrl shift R"
                     :name "Restart REPL"))

;; Items in menu  Options
(def increase-action (action
                     :handler (fn [e] (actions/inc-font-size e))
                     :key "menu PLUS"
                     :name "Increase font size"))
(def decrease-action (action
                     :handler (fn [e] (actions/dec-font-size e))
                     :key "menu MINUS"
                     :name "Decrease font size"))
(def settings-action (action
                     :handler (fn [e] (actions/settings e))
                     :name "Settings"))

;; Items in menu Manual
(def man-prop-action (action
                     :handler (fn [_] (actions/man "prop"))
                     :name "Propositional Logic"))
(def man-pred-action (action
                     :handler (fn [_] (actions/man "pred"))
                     :name "Predicate Logic"))
(def man-ltl-action (action
                     :handler (fn [_] (actions/man "ltl"))
                     :name "Linear Temporal Logic"))
(def man-nd-action (action
                     :handler (fn [_] (actions/man "nd"))
                     :name "Natural Deduction"))
(def man-cl-action (action
                     :handler (fn [_] (actions/man "cl"))
                     :name "Combinatory Logic"))


(defn make-menubar []
  ;; actions from RTextArea must be defined here, to be sure that the rtextarea is already created!
  (let [undo-action  (RTextArea/getAction RTextArea/UNDO_ACTION)
        redo-action  (RTextArea/getAction RTextArea/REDO_ACTION)
        cut-action   (RTextArea/getAction RTextArea/CUT_ACTION)
        copy-action  (RTextArea/getAction RTextArea/COPY_ACTION)
        paste-action (RTextArea/getAction RTextArea/PASTE_ACTION)
        menu-items' [
                     (menu :text "Session" :items [new-menu open-action recent-menu save-action save-as-action close-action])
                     (menu :text "Edit" :items [undo-action redo-action :separator cut-action copy-action paste-action :separator
                                                comment-action indent-action unindent-action goto-action :separator
                                                find-action next-action prev-action])
                     (menu :text "REPL" :items [curr-sexpr-action top-sexpr-action file-action clear-action restart-action])
                     (menu :text "Options" :items [increase-action decrease-action settings-action])
                     (menu :text "Manual" :items [man-prop-action man-pred-action man-ltl-action man-nd-action man-cl-action])]
        menu-items (if true (not (consts/is-mac)) (into [(menu :text "lwb-gui" :items [about-action :separator exit-action])] menu-items')
                            menu-items')]
    (menubar :items (seq menu-items))))
