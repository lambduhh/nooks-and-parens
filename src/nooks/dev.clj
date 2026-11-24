(ns nooks.dev
  "bb dev: build, serve, rebuild on change, reload the browser.

  The site's links all start with :site/base-path (e.g. /power-tools-and-parens/,
  a GitHub Pages project site). To serve that locally, .work/dev-root/<base> is
  a symlink to public/, and .work/dev-root is what's served. With an empty
  base path, public/ is served directly. The server listens on 127.0.0.1 only."
  (:require [babashka.fs :as fs]
            [babashka.http-server :as server]
            [clojure.string :as str]
            [nooks.build :as build]
            [nooks.data :as data])
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
        base     (get-in (data/load-all) [:site :site/base-path])   ; "" or "/segment"
        seg      (str/replace base #"^/" "")
        root     (if (str/blank? seg) (fs/path "public") (fs/path ".work" "dev-root"))
        home     (str base "/")]
    (rebuild! opts)
    (when-not (str/blank? seg)
      (fs/create-dirs root)
      ;; A link left from an earlier base path (the site was renamed) is harmless.
      (let [link (fs/path root seg)]
        (when-not (fs/exists? link {:nofollow-links true})
          (fs/create-sym-link link (fs/absolutize "public"))))
      (spit (str (fs/path root "index.html"))
            (str "<!doctype html><meta http-equiv=\"refresh\" content=\"0; url=" home "\"><a href=\"" home "\">" home "</a>")))
    (server/serve {:port port :dir (str root) :ip "127.0.0.1"})
    (println (str "\nServing http://localhost:" port home
                  "\nBlog draft: http://localhost:" port home "blog/announcement.html"
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
