; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

(ns lwb-gui.main
  (:import (javax.swing BorderFactory JFrame JLabel JMenuBar JPanel JTextField SpringLayout JCheckBox JButton
                        UIManager JMenu JTextArea AbstractAction JWindow JSplitPane JComponent JScrollPane ImageIcon JOptionPane)
           (java.awt.event MouseAdapter WindowAdapter ActionListener MouseEvent KeyEvent)
           (java.awt Color Font Desktop)
           (org.fife.ui.rsyntaxtextarea SyntaxConstants TextEditorPane)
           (org.fife.ui.rtextarea RTextScrollPane)
           (org.fife.ui.utils RecentFilesMenu)
           (java.awt.desktop AboutHandler QuitHandler QuitResponse))
  (:require [clojure.set]
            [lwb-gui.actions :as actions]
            [lwb-gui.consts :as consts]
            [lwb-gui.repl.control :as repl]
            [lwb-gui.repl.output :as repl-output]
            [lwb-gui.utils :as utils]
            [lwb-gui.navigate :as navigate]
            [lwb-gui.brackets :as brackets]
            [lwb-gui.highlighting :as highlighting]
            [lwb-gui.search :as search]
            [lwb-gui.prefs :as prefs]
            [lwb-gui.settings :as settings]
            [clojure.java.io :as io])
  (:gen-class))

(def gap 5)

;; Settings ------------------------------------------------------------------
(def default-settings
  (merge
    (zipmap [:font-name :font-size]
            (cond (utils/is-mac) ["Monaco" 12]
                  (utils/is-win) ["Courier New" 12]
                  :else ["Monospaced" 12]))
    {:line-wrap-doc              false
     :line-wrap-repl-out         false
     :show-only-monospaced-fonts true}))

(defn load-settings []
  (atom (merge default-settings (prefs/pget "settings"))))

(defn save-settings [settings]
  (prefs/pput "settings" settings))

(defn apply-settings [app settings]
  (let [set-line-wrapping (fn [text-area mode] (.setLineWrap text-area mode))
        set-font (fn [app font-name size]
                   (let [f (Font. font-name Font/PLAIN size)]
                     (utils/awt-event
                       (dorun (map #(.setFont (app %) f)
                                   [:edit-area
                                    :repl-area
                                    :search-text-area])))))]
    (set-line-wrapping (:edit-area app) (:line-wrap-doc settings))
    (set-line-wrapping (:repl-area app) (:line-wrap-repl-out settings))
    (set-font app (:font-name settings) (:font-size settings))
    (reset! (:settings app) settings)
    (save-settings settings)))

;; Font ----------------------------------------------------------------------
(defn resize-font [app fun]
  (apply-settings app (update-in @(:settings app)
                                 [:font-size]
                                 fun)))
(defn grow-font [app] (resize-font app inc))
(defn shrink-font [app] (resize-font app dec))

;; Caret finding --------------------------------------------------------------
(def highlight-agent (agent nil))

(defn display-caret-position [edit-area app]
  (let [{:keys [row col]} (utils/get-caret-coords edit-area)]
    (.setText (:pos-label app) (str " " (inc row) "|" (inc col)))))

(defn handle-caret-move [text-comp]
  (let [text (utils/get-text-str text-comp)]
    (send-off highlight-agent
              (fn [old-pos]
                (try
                  (let [pos (.getCaretPosition text-comp)]
                    (when-not (= pos old-pos)
                      (let [enclosing-brackets (brackets/find-enclosing-brackets text pos)
                            bad-brackets (brackets/find-bad-brackets text)
                            good-enclosures (clojure.set/difference
                                              (set enclosing-brackets) (set bad-brackets))]
                        (utils/awt-event
                          (highlighting/highlight-brackets text-comp good-enclosures bad-brackets)))))
                  (catch Throwable t (utils/awt-event (.printStackTrace t))))))))

;; Highlighting -----------------------------------------------------------------------------
(defn activate-caret-highlighter [app]
  (when-let [text-comp (app :edit-area)]
    (let [f #(handle-caret-move %)]
      (utils/add-caret-listener text-comp f)
      (utils/add-text-change-listener text-comp f))))

;; Double-click paren to select form --------------------------------------------------------
(defn double-click-selector [^JTextArea text-comp]
  (.addMouseListener text-comp
                     (proxy [MouseAdapter] []
                       (mouseClicked [^MouseEvent e]
                         (when (== 2 (.getClickCount e))
                           (utils/when-lets [pos (.viewToModel text-comp (.getPoint e))
                                             c (.. text-comp getDocument (getText pos 1) (charAt 0))
                                             pos (cond (#{\( \[ \{ \"} c) (inc pos)
                                                       (#{\) \] \} \"} c) pos)
                                             [a b] (brackets/find-enclosing-brackets (utils/get-text-str text-comp) pos)]
                                            (utils/set-selection text-comp a (inc b))))))))

;; build GUI ---------------------------------------------------------------------------------
(defn setup-search-elements [app]
  (.setVisible (:search-match-case-checkbox app) false)
  (.setVisible (:search-regex-checkbox app) false)
  (doto (:search-close-button app)
    (.setVisible false)
    (.setBorder nil)
    (.addActionListener
      (reify ActionListener
        (actionPerformed [_ _] (search/stop-find app)))))
  (let [sta (doto (app :search-text-area)
              (.setVisible false)
              (.setBorder (BorderFactory/createLineBorder Color/DARK_GRAY)))]
    (utils/add-text-change-listener sta #(search/update-find-highlight % app false))
    (utils/attach-action-keys sta ["ENTER" #(search/highlight-step app false)]
                              ["shift ENTER" #(search/highlight-step app true)]
                              ["ESCAPE" #(search/escape-find app)])))

(defn exit-if-closed [^JWindow f app]
  (.addWindowListener f
                      (proxy [WindowAdapter] []
                        (windowClosing [_]
                          (actions/exit app)
                          (System/exit 0)))))

(defn move-caret-to-line
  "Move caret to choosen line"
  [textarea]
  (let [current-line (fn [] (inc (.getLineOfOffset textarea (.getCaretPosition textarea))))
        line-str (JOptionPane/showInputDialog nil "Line number:" "Go to Line" JOptionPane/QUESTION_MESSAGE)
        line-num (Integer.
                   ^int (if (or (nil? line-str) (nil? (re-find #"\d+" line-str)))
                     (current-line)
                     (re-find #"\d+" line-str)))]
    (utils/scroll-to-line textarea line-num)
    (.requestFocus textarea)))

(defn attach-global-action-keys [comp app]
  (utils/attach-action-keys comp
                            ["cmd1 PLUS" #(grow-font app)]
                            ["cmd1 MINUS" #(shrink-font app)]
                            ["cmd1 K" #(.setText (app :repl-area) "")]))

(defn make-edit-area []
  (doto (TextEditorPane.)
    (.setAnimateBracketMatching false)
    (.setBracketMatchingEnabled true)
    (.setAutoIndentEnabled true)
    (.setAntiAliasingEnabled true)
    (.setTabSize 2)
    (.setLineWrap false)
    navigate/attach--keys
    double-click-selector
    actions/setup-autoindent
    ))

(defn load-recent-files [app]
  (let [hl (prefs/pget "recent-files")]
    (if (some? hl)
      (dorun (for [f hl]
               (.addFileToFileHistory (:recent-menu app) f))))
    ))


(defn make-recent-menu [app]
  (doto (proxy [RecentFilesMenu] ["Open Recent"]
          (createOpenAction [^String path]
            (proxy [AbstractAction] [path]
              (actionPerformed [_]
                (actions/open-recent-file app path))))
          (getShouldIgnoreFile [path]
            (if (.endsWith path "Untitled.txt")
              true
              false)))
    (.setMaximumFileHistorySize 10)))

(defn create-app []
  (let [frame (JFrame.)
        icon-128 (ImageIcon. (io/resource "lwb-128.png"))
        icon-64 (ImageIcon. (io/resource "lwb-64.png"))
        layout (SpringLayout.)
        edit-panel (JPanel.)
        edit-label (JLabel. "Editor - <unsaved session>")
        edit-area (make-edit-area)
        repl-area (JTextArea.)
        ^JScrollPane repl-pane (repl-output/tailing-scroll-pane repl-area)
        search-text-area (JTextField.)
        search-match-case-checkbox (JCheckBox. "Match case")
        search-regex-checkbox (JCheckBox. "Regex")
        search-close-button (JButton. "X")
        pos-label (JLabel.)
        repl-panel (JPanel.)
        repl-label (JLabel. "Clojure REPL output")
        ^JSplitPane split-pane (utils/make-split-pane edit-panel repl-panel true gap 0.5)
        app (merge {:repl-map (atom nil)}
                   (utils/gen-map
                     edit-label
                     edit-area
                     repl-area
                     repl-label
                     frame
                     icon-128
                     icon-64
                     repl-pane
                     search-text-area
                     search-match-case-checkbox
                     search-regex-checkbox
                     search-close-button
                     pos-label
                     split-pane
                     ))
        doc-scroll-pane (RTextScrollPane. ^TextEditorPane edit-area)
        recent-menu (make-recent-menu app)
        app (assoc app :repl (atom nil)
                       :recent-menu recent-menu
                       :settings (load-settings))]
    (load-recent-files app)
    (doto (:edit-area app)
      (utils/add-caret-listener #(display-caret-position % app)))
    (doto frame
      (.setBounds 25 50 950 700)
      (.setLayout layout)
      (.add split-pane)
      (.setTitle (str "Logic Workbench GUI - Rev " consts/rev)))
    (doto edit-panel
      (.setLayout (SpringLayout.))
      (.add doc-scroll-pane)
      (.add edit-label)
      (.add pos-label)
      (.add search-text-area)
      (.add search-match-case-checkbox)
      (.add search-regex-checkbox)
      (.add search-close-button))
    (doto repl-panel
      (.setLayout (SpringLayout.))
      (.add repl-label)
      (.add ^JComponent repl-pane))
    (utils/constrain-to-parent repl-label :n 0 :w 0 :n 15 :e 0)
    (utils/constrain-to-parent repl-pane :n 16 :w 0 :s -16 :e 0)
    (doto pos-label
      (.setFont (Font. "Courier" Font/PLAIN 13)))
    #_(.setSyntaxEditingStyle repl-area
                              SyntaxConstants/SYNTAX_STYLE_CLOJURE)
    (.setSyntaxEditingStyle edit-area
                            SyntaxConstants/SYNTAX_STYLE_CLOJURE)
    (utils/constrain-to-parent split-pane :n gap :w gap :s (- gap) :e (- gap))
    (utils/constrain-to-parent edit-label :n 0 :w 0 :n 15 :e 0)
    (utils/constrain-to-parent doc-scroll-pane :n 16 :w 0 :s -16 :e 0)
    (utils/constrain-to-parent pos-label :s -14 :w 0 :s 0 :w 100)
    (utils/constrain-to-parent search-text-area :s -15 :w 100 :s 0 :w 350)
    (utils/constrain-to-parent search-match-case-checkbox :s -15 :w 355 :s 0 :w 470)
    (utils/constrain-to-parent search-regex-checkbox :s -15 :w 475 :s 0 :w 550)
    (utils/constrain-to-parent search-close-button :s -15 :w 65 :s 0 :w 95)
    (.layoutContainer layout frame)
    (exit-if-closed frame app)
    (setup-search-elements app)
    (activate-caret-highlighter app)
    (utils/attach-action-keys edit-area
                              ["cmd1 ENTER" #(repl/send-selected-to-repl app)])
    (doto repl-area (.setEditable false))
    (dorun (map #(attach-global-action-keys % app)
                [edit-area repl-area (.getContentPane frame)]))
    app))

; Menu ------------------------------------------------------------------------



(defn make-menus [app]
  (when (utils/is-mac)
    (System/setProperty "apple.laf.useScreenMenuBar" "true")
    )
  (let [menu-bar (JMenuBar.)]
    (.setJMenuBar (:frame app) menu-bar)
    ;; Session
    (let [new-menu (doto (JMenu. "New")
                     (.setMnemonic KeyEvent/VK_N)
                     (utils/add-menu-item "Propositional Logic" "" nil #(actions/new-session app consts/new-prop))
                     (utils/add-menu-item "Predicate Logic" "" nil #(actions/new-session app consts/new-pred))
                     (utils/add-menu-item "Linear Temporal Logic" "" nil #(actions/new-session app consts/new-ltl))
                     (utils/add-menu-item "Natural Deduction" "" nil #(actions/new-session app consts/new-nd))
                     (utils/add-menu-item "Combinatory Logic" "" nil #(actions/new-session app consts/new-cl)))
          session-menu (doto (JMenu. "Session")
                         (.setMnemonic KeyEvent/VK_S)
                         (.add new-menu)
                         (utils/add-menu-item "Open" "O" "cmd1 O" #(actions/open-file app))
                         (.add ^RecentFilesMenu (:recent-menu app))
                         ;(utils/add-menu-item "Open Recent..." "R" "cmd1 R" #(actions/open-recent-file app))
                         (utils/add-menu-item "Save" "S" "cmd1 S" #(actions/save-file app))
                         (utils/add-menu-item "Save As.." "A" "cmd1 shift S" #(actions/save-as-file app))
                         (utils/add-menu-item "Close" "C" "cmd1 C" #(actions/close-session app))
                         )]
      (when-not (utils/is-mac)
        (utils/add-menu-item session-menu "Exit" "X" nil #(actions/exit' app)))
      (.add menu-bar session-menu))
    ;; Edit
    (let [edit-menu (doto (JMenu. "Edit")
                      (.setMnemonic KeyEvent/VK_E)
                      (utils/add-menu-item "Comment" "C" "cmd1 SEMICOLON" #(actions/toggle-comment (:edit-area app)))
                      (utils/add-menu-item "Fix indentation" "F" "cmd1 I" #(actions/fix-indent-selected-lines (:edit-area app)))
                      (utils/add-menu-item "Indent lines" "I" "cmd1 shift RIGHT" #(actions/indent (:edit-area app)))
                      (utils/add-menu-item "Unindent lines" "D" "cmd1 shift LEFT" #(actions/unindent (:edit-area app)))
                      (utils/add-menu-item "Go to line..." "G" "cmd1 L" #(move-caret-to-line (:edit-area app)))
                      (utils/add-menu-item :sep)
                      (utils/add-menu-item "Find" "F" "cmd1 F" #(search/start-find app))
                      (utils/add-menu-item "Find next" "N" "cmd1 G" #(search/highlight-step app false))
                      (utils/add-menu-item "Find prev" "P" "cmd1 shift G" #(search/highlight-step app true)))]
      (.add menu-bar edit-menu))
    ;; REPL
    (let [repl-menu (doto (JMenu. "REPL")
                      (.setMnemonic KeyEvent/VK_R)
                      (utils/add-menu-item "Evaluate current sexpression" "C" "cmd2 shift C" #(repl/send-selected-to-repl app))
                      (utils/add-menu-item "Evaluate top sexpression" "T" "cmd2 shift T" #(repl/send-top-sexpr-to-repl app))
                      (utils/add-menu-item "Evaluate entire file" "F" "cmd2 shift F" #(repl/send-doc-to-repl app))
                      (utils/add-menu-item "Clear output" "C" "cmd1 shift C" #(.setText (app :repl-area) ""))
                      (utils/add-menu-item "Restart REPL" "R" "cmd1 shift R" #(repl/restart-repl app)))]
      (.add menu-bar repl-menu))
    ;; Options
    (let [options-menu (doto (JMenu. "Options")
                         (.setMnemonic KeyEvent/VK_O)
                         (utils/add-menu-item "Increase font size" nil "cmd1 PLUS" #(grow-font app))
                         (utils/add-menu-item "Decrease font size" nil "cmd1 MINUS" #(shrink-font app))
                         (utils/add-menu-item "Settings" nil nil #(settings/show-settings-window app apply-settings)))]
      (.add menu-bar options-menu))
    ;; Manual
    (let [man-menu (doto (JMenu. "Manual")
                     (.setMnemonic KeyEvent/VK_M)
                     (utils/add-menu-item "Propositional Logic" "" nil #(actions/man "prop"))
                     (utils/add-menu-item "Predicate Logic" "" nil #(actions/man "pred"))
                     (utils/add-menu-item "Linear Temporal Logic" "" nil #(actions/man "ltl"))
                     (utils/add-menu-item "Natural Deduction" "" nil #(actions/man "nd"))
                     (utils/add-menu-item "Combinatory Logic" "" nil #(actions/man "cl")))]
      (when-not (utils/is-mac)
        (.addSeparator man-menu)
        (utils/add-menu-item man-menu "About" "A" nil #(actions/about-dlg app)))
      (.add menu-bar man-menu))))

;; Startup

(defonce current-app (atom nil))

(defn startup []
  (Thread/setDefaultUncaughtExceptionHandler
    (proxy [Thread$UncaughtExceptionHandler] []
      (uncaughtException [thread exception]
        (println thread) (.printStackTrace exception))))
  (if utils/is-mac
    (System/setProperty "apple.laf.useScreenMenuBar" "true") )
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName))
  (let [app (create-app)]
    (reset! current-app app)
    (make-menus app)
    (let [frame (app :frame)]
      (utils/persist-window-shape "main-window" frame)
      (.setVisible frame true))
    (repl/start-repl app)
    (apply-settings app @(:settings app))
    ; special handlers for Mac OSX
    (if utils/is-mac
      (let [mac-app #_(Application/getApplication) (Desktop/getDesktop)]
        (.setQuitHandler mac-app (proxy [QuitHandler] []
                                   (handleQuitRequestWith [_ ^QuitResponse response]
                                     (actions/exit app)
                                     (.performQuit response))))
        (.setAboutHandler mac-app (proxy [AboutHandler] []
                                    (handleAbout [_]
                                      (actions/about-dlg app))
                                    ))))))

(defn -main []
  (startup))

