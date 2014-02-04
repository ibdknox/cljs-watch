(use '[clojure.java.io :only [file]])
(require '[cljs.closure :as cljsc])

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

  (def default-opts {:optimizations :simple
                     :pretty-print true
                     :output-dir "resources/public/cljs/"
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

  (defn generic? [ns-form]
    (-> ns-form (nth 2) :generic))

  (def extract-ns
    (partial some #(when (= (first %) 'ns) %)))

  (defn structurize [source-file]
    (->> (slurp source-file)
         (format "(%s)")
         (read-string)))

  (defn find-cljs [dir]
    (let [dir-files (-> dir file file-seq)]
      (->> (ext-filter dir-files "clj")
           (filter (comp generic? extract-ns structurize))
           (concat (ext-filter dir-files "cljs")))))

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

  (defn transform-cl-args
    [args]
    (let [source (first args)
          opts-string (apply str (interpose " " (rest args)))
          options (when (> (count opts-string) 1)
                    (try (read-string opts-string)
                         (catch Exception e (println e))))]
      {:source source :options options}))
  
  (let [{:keys [source options]} (transform-cl-args *command-line-args*)
        src-dir (or source "src/")
        opts (merge default-opts options)]
    (.mkdirs (file (:output-dir opts)))
    (watcher-print "Building ClojureScript files in ::" src-dir)
    (compile-cljs src-dir opts)
    (status-print "[done]")
    (watcher-print "Waiting for changes\n")
    (while true
      (Thread/sleep 1000)
      (when (files-updated? src-dir)
        (watcher-print "Compiling updated files...")
        (compile-cljs src-dir opts)
        (status-print "[done]")))))
