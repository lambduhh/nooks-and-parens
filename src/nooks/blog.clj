(ns nooks.blog
  "/blog/, rendered by quickblog inside the site chrome.

  How the chrome gets in: quickblog wraps every page in templates-dir/base.html.
  We don't track a base.html. Each build renders the site layout (header,
  toolbelt, footer, CSP) with `{{body|safe}}` where <main> goes, and writes it,
  plus the tracked templates/*.html, into a work dir that quickblog uses as its
  templates-dir. One layout, two renderers, no copy to drift.

  Tracked templates may contain [[t :ns/key]] tokens; they're replaced with the
  English UI string (draft markers included) before quickblog sees them."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [nooks.i18n :as i18n]
            [nooks.layout :as layout]
            [nooks.template :as template]
            [quickblog.api :as qb]))

(def posts-dir "blog/posts")
(def assets-dir "blog/assets")
(def templates-src "templates")
(def new-post-template "blog/new-post.md")

(defn- i18n-tokens
  "Replace [[t :ns/key]] with the rendered string for that key."
  [s]
  (str/replace s #"\[\[t (:[\w./-]+)\]\]"
               (fn [[_ k]]
                 (str (h/html (i18n/t :en (keyword (subs k 1))))))))

(defn base-template
  "The site layout as a Selmer template for quickblog."
  [ctx]
  (layout/page
   (assoc ctx :route :blog)
   {:title (h/raw "{{title}}")
    :head  (h/raw (str "<link rel=\"alternate\" type=\"application/atom+xml\" href=\""
                       (layout/href ctx "/blog/atom.xml") "\" title=\"{{blog-title}}\">"
                       "{% if sharing.description %}<meta name=\"description\" content=\"{{sharing.description}}\">{% endif %}"
                       "<meta property=\"og:title\" content=\"{{title}}\">"
                       "{% if sharing.url %}<meta property=\"og:url\" content=\"{{sharing.url}}\">{% endif %}"))
    :main  (list (h/raw "{{body|safe}}")
                 ;; Every article ends with one next step: the page's primary button.
                 [:p.next-step (layout/primary-button ctx)])}))

(defn- write-templates! [ctx dir]
  (fs/create-dirs dir)
  (doseq [f (fs/glob templates-src "*.{html,css}")]
    (spit (str (fs/path dir (fs/file-name f))) (i18n-tokens (slurp (str f)))))
  (spit (str (fs/path dir "base.html")) (base-template ctx)))

(defn- unescape [s]
  (-> s (str/replace #"<[^>]*>" "")
      (str/replace "&apos;" "'") (str/replace "&#39;" "'") (str/replace "&quot;" "\"")
      (str/replace "&lt;" "<") (str/replace "&gt;" ">") (str/replace "&amp;" "&")))

(defn github-heading-ids
  "Give every heading in a post the id GitHub would (\"## 1. Introduction\" ->
  #1-introduction), as the template guides on the hub have, so a post's table
  of contents uses the anchors people expect. Duplicates get -1, -2."
  [html]
  (let [seen (atom {})]
    (str/replace html #"(?s)<h([1-6]) id=\"[^\"]*\">(.*?)</h\1>"
                 (fn [[_ level inner]]
                   (let [slug (template/github-slug (unescape inner))
                         n    (get @seen slug 0)]
                     (swap! seen update slug (fnil inc 0))
                     (str "<h" level " id=\"" (if (zero? n) slug (str slug "-" n)) "\">" inner "</h" level ">"))))))

(defn quickblog-opts [ctx out-dir work-dir]
  (let [site (get-in ctx [:data :site])]
    {:blog-title       (str (i18n/t-str :en :blog/title) " · " (:site/name site))
     :blog-author      (:site/author site)
     :blog-description (:site/name site)
     :blog-root        (str (:site/url site) "/blog/")
     :posts-dir        posts-dir
     :assets-dir       assets-dir
     :templates-dir    (str (fs/path work-dir "templates"))
     :out-dir          (str (fs/path out-dir "blog"))
     :cache-dir        (str (fs/path work-dir "cache"))
     :force-render     true
     :post-process-html-fn github-heading-ids
     :num-index-posts  10
     :default-metadata {:tags #{}}}))

(defn render!
  "Render the blog into out-dir/blog. quickblog's progress output goes to
  *out*; callers that want quiet bind it."
  [ctx out-dir work-dir]
  (write-templates! ctx (fs/path work-dir "templates"))
  (qb/render (quickblog-opts ctx out-dir work-dir)))

;; ---- bb blog:new -----------------------------------------------------------

(defn slugify [title]
  (-> title str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-|-$" "")))

(defn new-post!
  "bb blog:new \"Title\" [--slug my-slug]. New posts start as Preview: true,
  reachable by URL and absent from the index, tags and feeds, until you delete
  that line."
  [& args]
  (let [[title & more] args
        opts  (apply hash-map more)
        slug-arg (get opts "--slug")]
    (when (str/blank? title)
      (binding [*out* *err*] (println "Usage: bb blog:new \"Title\" [--slug my-slug]"))
      (System/exit 2))
    (when (and slug-arg (not (re-matches #"[a-z0-9]+(-[a-z0-9]+)*" slug-arg)))
      (binding [*out* *err*] (println "--slug may only use a-z, 0-9 and single hyphens"))
      (System/exit 2))
    (let [slug (or slug-arg (slugify title))
          file (str slug ".md")]
      (when (fs/exists? (fs/path posts-dir file))
        (binding [*out* *err*] (println "Already exists:" (str (fs/path posts-dir file))))
        (System/exit 1))
      (qb/new {:posts-dir posts-dir :templates-dir templates-src
               :file file :title title :preview true :tags [] :default-metadata {:tags []}
               :template-file new-post-template})
      (println "Created" (str (fs/path posts-dir file)))
      (println (str "See it at http://localhost:1888/nooks-and-parens/blog/" slug ".html (run bb dev)")))))
