; Copyright (c) 2019 - by Burkhardt Renz 
; All rights reserved.
; Eclipse Public License 1.0
; based on the project clooj by Arthur Edelstein

; A Clojure REPL embedded in our program 
; with itsself on the classpath

(ns lwb-gui.repl.clj-repl
  (:import (java.io File PrintWriter InputStreamReader))
  (:require [clojure.java.io :as io]))

(defn java-binary
  "Returns the fully-qualified path of the java binary."
  []
  (str (System/getProperty "java.home")
       File/separator "bin" File/separator "java"))

(defn this-jar
  "Get the name of jar in which this function is invoked"
  []
  (-> *ns* class .getProtectionDomain .getCodeSource .getLocation .toURI .getPath))

(defn repl-process
  "Start an external repl process by running clojure.main."
  []
  (.start (doto (ProcessBuilder. [(java-binary) "-cp" (this-jar) "clojure.main"])
            (.redirectErrorStream true)
            (.directory (io/file ".")))))

(defn copy
  "Continuously copies all content from a java InputStream
   to a java Writer. Blocks until InputStream closes."
  [input-stream writer]
  (let [reader (InputStreamReader. input-stream)]
    (loop []
      (let [c (.read reader)]
        (when (not= c -1)
          (.write writer c)
          (recur))))))

(defn launch-repl
  "Launch an outside process with a clojure repl.
   Returns a map with keys :process, :input-writer and :result-writer,
   which references the corresponding objects."
  [result-writer]
  (let [process (repl-process)
        input-writer (-> process .getOutputStream (PrintWriter. true))
        is (.getInputStream process)]
    (future (copy is result-writer))                        ; 
    {:input-writer  input-writer
     :process       process
     :result-writer result-writer}))

(defn evaluate
  "Evaluate some code in the repl specified by repl-map."
  [repl-map code]
  (binding [*out* (:input-writer repl-map)]
    (println code)
    (.flush *out*)))

(defn close
  "Close the repl specified in the repl-map."
  [{:keys [input-writer result-writer process]}]
  (.flush input-writer)
  (.close input-writer)
  (.flush result-writer)
  (.destroy process))

  


