(use '[clojure.java.io :only [file]])
(require '[cljs.closure :as cljsc])
(require '[clojure.tools.cli :as cli])

;;wrap everything in a do to prevent the ouput, as a file
;;this is unnecessary, but from clojure.main -e it makes a 
;;difference
(do
  (import '[java.util Calendar])
  (import '[java.text SimpleDateFormat])

  (defn text-timestamp []
    (let [c (Calendar/getInstance)
          f (SimpleDateFormat. "HH:mm:ss")]
      (.format f (.getTime c))))

  (def default-opts {:output-dir "resources/public/cljs/"
                     :output-to "resources/public/cljs/bootstrap.js"})

  (def ANSI-CODES
    {:reset "[0m"
     :default "[39m"
     :white   "[37m"
     :black   "[30m"
     :red     "[31m"
     :green   "[32m"
     :blue    "[34m"
     :yellow  "[33m"
     :magenta "[35m"
     :cyan    "[36m"
     })

  (defn ansi
    [code]
    (str \u001b (get ANSI-CODES code (:reset ANSI-CODES))))

  (defn style
    [s & codes]
    (str (apply str (map ansi codes)) s (ansi :reset)))

  (def last-compiled (atom 0))

  (defn ext-filter [coll ext]
    (filter (fn [f]
              (let [fname (.getName f)
                    fext (subs fname (inc (.lastIndexOf fname ".")))]
                (and (not= \. (first fname)) (.isFile f) (= fext ext))))
            coll))

  (defn find-cljs [dir]
    (let [dir-files (-> dir file file-seq)]
      (ext-filter dir-files "cljs")))

  (defn compile-cljs [src-dir opts]
    (try
      (cljsc/build src-dir opts)
      (catch Throwable e
        (.printStackTrace e)))
    (reset! last-compiled (System/currentTimeMillis)))

  (defn newer? [f]
    (let [last-modified (.lastModified f)]
      (> last-modified @last-compiled)))

  (defn files-updated? [dir]
    (some newer? (find-cljs dir)))

  (defn watcher-print [& text]
    (print (style (str (text-timestamp) " :: watcher :: ") :magenta))
    (apply print text)
    (flush))

  (defn status-print [text]
    (print "    " (style text :green) "\n")
    (flush))

  (defn- parse-options [extra]
    (let [opts-string (apply str (interpose " " extra))]
      (if (empty? opts-string)
        {}
        (try (let [opts (read-string opts-string)]
                (if (map? opts)
                  opts
                  (do
                    (println "cljs options must be in map syntax")
                    {})))
          (catch Exception e (println e))))))

  (defn nop [])
  (defn- create-done-fn [argsmap]
    (if (:bell argsmap)
      #(do (print \u0007) (flush))
      (if-let [cmd (:bell-cmd argsmap)]
        #(.exec (Runtime/getRuntime) cmd)
        nop
      )))

  (defn transform-cl-args [args]
    (let [[argmap extra] (cli/cli args
            ["-s" "--source"    "Source file or directory" :default "src/"]
            ["-b" "--bell"      "Uses system beep to indicate a finished compile" :flag true]
            ["-c" "--bell-cmd"  "Use this to customize the beep command e.g. growlnotify -m compile-done cljs-watch"])
          options (parse-options extra)]
        (assoc argmap :options options)))

  (let [
    argsmap (transform-cl-args *command-line-args*)
    {src-dir :source, :keys [options]} argsmap
    donefn (create-done-fn argsmap)
    opts (merge default-opts options)]
      (.mkdirs (file (:output-dir opts)))
      (watcher-print "Building ClojureScript files in ::" src-dir)
      (compile-cljs src-dir opts)
      (status-print "[done]")
      (donefn)
      (watcher-print "Waiting for changes\n")
      (while true
        (Thread/sleep 1000)
        (when (files-updated? src-dir)
          (watcher-print "Compiling updated files...")
          (compile-cljs src-dir opts)
          (status-print "[done]")
          (donefn)))))
