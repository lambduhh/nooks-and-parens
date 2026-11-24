(ns nooks.build
  "bb build [--as-of DATE] [--out DIR]: the whole site into public/.

  The build is a function of (data, as-of). `as-of` defaults to the real clock,
  read once here; everything downstream takes it as an argument, which is what
  lets `bb check` build the site as of any date."
  (:require [babashka.fs :as fs]
            [nooks.blog :as blog]
            [nooks.data :as data]
            [nooks.i18n :as i18n :refer [t t-str]]
            [nooks.layout :as layout]
            [nooks.pages.stubs :as stubs]
            [nooks.state :as state]
            [nooks.template :as template]
            [nooks.vendor :as vendor])
  (:import [java.time Instant]))

(def languages [:en :pt-BR])

(def dev-reload-js
  "Dev only (bb dev): reload the page when the build stamp changes. Same-origin,
  so the CSP allows it; never written by a production build."
  "(function(){var u=document.currentScript.src.replace(/assets\\/js\\/dev-reload\\.js.*/,'__build.txt'),last=null;
setInterval(function(){fetch(u,{cache:'no-store'}).then(function(r){return r.text()}).then(function(s){if(last!==null&&s!==last)location.reload();last=s}).catch(function(){})},800);})();\n")

(defn- write! [path s]
  (fs/create-dirs (fs/parent path))
  (spit (str path) s))

(defn- page-file [out-dir lang path]
  (fs/path out-dir (str (subs (str (layout/lang-prefix lang) path) 1) "index.html")))

(defn- ctx-for [base lang]
  (assoc base :lang lang
         ;; Nothing is translated yet: pt-BR pages carry English content.
         :content-lang :en))

(defn- all-pages
  "Every non-blog page: [{:route :path :page}]. Start here and the Learn
  guides come from the template repo (nooks.template)."
  [site]
  (let [tpl (template/pages site)]
    (concat
     (for [[route f] stubs/pages] {:route route :path (layout/routes route) :page f})
     [{:route :learn :path (layout/routes :learn) :page (template/learn-page site (concat tpl (blog/post-list)))}]
     tpl)))

(defn- render-page! [out-dir ctx {:keys [route path page]}]
  (let [ctx (assoc ctx :route route :path path)]
    (write! (page-file out-dir (:lang ctx) path)
            (layout/page ctx (page ctx)))))

(defn- blog-pt-notice! [out-dir ctx]
  (let [ctx (assoc ctx :lang :pt-BR :content-lang :en :route :blog-pt)
        lang :pt-BR]
    (write! (fs/path out-dir "pt-br" "blog" "index.html")
            (layout/page ctx {:title (t-str lang :route/blog)
                              :main  [:section.panel
                                      [:h1 (t lang :route/blog)]
                                      [:p [:a {:href (layout/route-href ctx :en :blog)}
                                           (t lang :route/blog) " (EN)"]]
                                      (layout/primary-button ctx)]}))))

(defn build!
  "Build into out-dir. opts: :as-of Instant, :out-dir, :work-dir, :dev?, :quiet?"
  [{:keys [as-of out-dir work-dir dev? quiet?]
    :or   {out-dir "public" work-dir ".work/build"}}]
  (let [d    (data/load-all)
        base {:data d :as-of as-of :dev? dev?}]
    (binding [i18n/*strings* (:i18n d)]
      (fs/delete-tree out-dir)
      (fs/create-dirs out-dir)
      (fs/copy-tree "assets" (fs/path out-dir "assets"))
      (vendor/install! out-dir)
      (when dev? (write! (fs/path out-dir "assets" "js" "dev-reload.js") dev-reload-js))
      (let [pages (all-pages (:site d))]
        (doseq [lang languages, pg pages]
          (render-page! out-dir (ctx-for base lang) pg)))
      (write! (fs/path out-dir "404.html")
              (layout/page (assoc base :lang :en :route nil) (stubs/not-found (assoc base :lang :en))))
      (blog-pt-notice! out-dir base)
      (let [render #(blog/render! (assoc base :lang :en :content-lang :en) out-dir work-dir)]
        (if quiet?
          (binding [*out* (java.io.StringWriter.)] (render))
          (render)))
      (when dev? (write! (fs/path out-dir "__build.txt") (str (System/nanoTime))))
      {:out-dir out-dir :as-of as-of})))

(defn parse-args [args]
  (loop [[a b & more :as args] args, m {}]
    (cond
      (empty? args)      m
      (= a "--as-of")    (recur more (assoc m :as-of (state/parse-as-of b)))
      (= a "--out")      (recur more (assoc m :out-dir b))
      :else (throw (ex-info (str "Unknown argument " a ". Flags: --as-of DATE, --out DIR") {})))))

(defn -main [& args]
  (let [opts  (parse-args args)
        as-of (or (:as-of opts) (Instant/now))
        {:keys [out-dir]} (build! (assoc opts :as-of as-of :quiet? true))]
    (println (str "Built " out-dir "/ as of " as-of
                  " (" (count (fs/glob out-dir "**.html")) " pages)"))))

