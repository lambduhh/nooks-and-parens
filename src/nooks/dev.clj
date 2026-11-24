(ns nooks.dev
  "bb dev: build, serve, rebuild on change, reload the browser.

  The site's links all start with /nooks-and-parens/ (GitHub Pages project
  site). To serve that locally, .work/dev-root/nooks-and-parens is a symlink to
  public/, and .work/dev-root is what's served. The server listens on
  127.0.0.1 only."
  (:require [babashka.fs :as fs]
            [babashka.http-server :as server]
            [nooks.build :as build])
  (:import [java.time Instant]))

(def watched ["data" "src" "assets" "blog" "templates"])

(defn- stamp
  "A fingerprint of every watched file's path and mtime."
  []
  (hash (for [dir watched
              f (fs/glob dir "**")
              :when (fs/regular-file? f)]
          [(str f) (.toMillis (fs/last-modified-time f))])))

(defn- src-stamp []
  (hash (for [f (fs/glob "src" "**.clj")] [(str f) (.toMillis (fs/last-modified-time f))])))

(defn- rebuild! [opts]
  (try
    (let [t0 (System/currentTimeMillis)
          as-of (or (:as-of opts) (Instant/now))]
      (build/build! {:as-of as-of :dev? true :quiet? true})
      (println (str "Built as of " as-of " in " (- (System/currentTimeMillis) t0) " ms")))
    (catch Exception e
      (binding [*out* *err*]
        (println "Build failed:" (ex-message e))
        (when-let [d (ex-data e)] (println "  " (pr-str d)))))))

(defn -main [& args]
  (let [port-arg (second (drop-while #(not= "--port" %) args))
        port     (if port-arg (parse-long port-arg) 1888)
        opts     (build/parse-args (remove #{"--port" port-arg} args))
        root     (fs/path ".work" "dev-root")
        link     (fs/path root "nooks-and-parens")]
    (rebuild! opts)
    (fs/create-dirs root)
    (when-not (fs/exists? link {:nofollow-links true})
      (fs/create-sym-link link (fs/absolutize "public")))
    (spit (str (fs/path root "index.html"))
          "<!doctype html><meta http-equiv=\"refresh\" content=\"0; url=/nooks-and-parens/\"><a href=\"/nooks-and-parens/\">/nooks-and-parens/</a>")
    (server/serve {:port port :dir (str root) :ip "127.0.0.1"})
    (println (str "\nServing http://localhost:" port "/nooks-and-parens/"
                  "\nBlog draft: http://localhost:" port "/nooks-and-parens/blog/announcement.html"
                  "\nWatching " (pr-str watched) ". Ctrl-C to stop."))
    (loop [s (stamp), ss (src-stamp)]
      (Thread/sleep 500)
      (let [s2 (stamp)]
        (if (= s s2)
          (recur s ss)
          (let [ss2 (src-stamp)]
            (when (not= ss ss2)
              ;; Code changed: reload every nooks namespace before building.
              (try (require 'nooks.build :reload-all)
                   (catch Exception e
                     (binding [*out* *err*] (println "Reload failed:" (ex-message e))))))
            (rebuild! opts)
            (recur (stamp) ss2)))))))
