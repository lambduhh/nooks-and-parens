(ns nooks.template
  "The template repo's guides, rendered into the hub.

  At build time we check out the template at the commit pinned in
  data/template.edn (cached under .work/template/<sha>/), parse each listed
  Markdown file with nextjournal-markdown, and transform the tree:

  - heading ids follow GitHub's rules, so the guides' own #anchors still work;
  - relative links go to the hub page for that file when there is one, or to
    the file on GitHub at the pinned commit;
  - raw HTML is limited to an allowlist (details, summary, kbd, br); anything
    else shows as text, and HTML comments (review markers) are dropped;
  - a Mermaid diagram (drawn by JavaScript on GitHub) shows as its source in a
    collapsed <details>: the guide's numbered steps carry the same route.

  The output is static HTML on our own origin, under our CSP."
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [hiccup2.core :as h]
            [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as mdt]
            [nooks.i18n :as i18n :refer [t t-str]]
            [nooks.layout :as layout]))

(defn config [] (edn/read-string (slurp "data/template.edn")))

;; ---- Checkout --------------------------------------------------------------

(defn checkout-dir [sha] (fs/path ".work" "template" sha))

(defn ensure-checkout!
  "The template at `sha`, fetched once and cached. Fails the build if the
  commit can't be fetched: a hub without its guides shouldn't deploy."
  [url sha]
  (when-not (re-matches #"[0-9a-f]{40}" (str sha))
    (throw (ex-info "data/template.edn :template/sha must be a full 40-character commit SHA" {:sha sha})))
  (let [dir  (checkout-dir sha)
        done (fs/path dir ".nooks-ok")]
    (when-not (fs/exists? done)
      (fs/delete-tree dir)
      (fs/create-dirs dir)
      (let [git (fn [& args] (apply p/shell {:dir (str dir) :out :string :err :string} "git" args))]
        (git "init" "-q")
        (git "fetch" "-q" "--depth" "1" (str url ".git") sha)
        (git "checkout" "-q" "FETCH_HEAD"))
      (spit (str done) sha))
    dir))

;; ---- Paths and links -------------------------------------------------------

(defn- normalize
  "Resolve `rel` against the directory of repo path `from`. Nil if it climbs
  out of the repo."
  [from rel]
  (loop [out [] [seg & more] (concat (butlast (str/split from #"/")) (str/split rel #"/"))]
    (cond
      (nil? seg)                 (str/join "/" out)
      (#{"" "."} seg)            (recur out more)
      (= ".." seg)               (when (seq out) (recur (pop out) more))
      :else                      (recur (conj out seg) more))))

(defn- canonical
  "Follow a symlink inside the checkout (AGENTS.md -> ai/AGENTS.md)."
  [dir path]
  (let [f (fs/path dir path)]
    (if (fs/sym-link? f)
      (let [target (fs/read-link f)]
        (or (normalize path (str target)) path))
      path)))

(defn- github-url [site sha dir path]
  (let [kind (if (fs/directory? (fs/path dir path)) "tree" "blob")]
    (str (:template/url site) "/" kind "/" sha "/" path)))

(defn link-rewriter
  "A fn from an href in the guide at `from` to the href the hub should use.
  `hub-href` turns a hub path (/learn/tutorial/) into a full link for the page's
  language."
  [{:keys [site sha dir hub-for hub-href]} from]
  (fn [href]
    (cond
      (re-find #"^(?i)([a-z][a-z0-9+.-]*:|//|#)" href) href
      :else
      (let [[path frag] (str/split href #"#" 2)
            frag   (when frag (str "#" frag))
            target (some->> (normalize from path) (canonical dir))]
        (cond
          (nil? target)          (str (:template/url site) "/blob/" sha "/" from)
          (str/blank? path)      (str frag)
          (hub-for target)       (str (hub-href (hub-for target)) frag)
          :else                  (str (github-url site sha dir target) frag))))))

;; ---- Markdown tree ---------------------------------------------------------

(defn github-slug
  "GitHub's heading anchor: lower-case, drop punctuation except - and _,
  spaces to hyphens."
  [text]
  (-> text str/lower-case str/trim
      (str/replace #"[^\p{L}\p{N}\s_-]" "")
      (str/replace #"\s" "-")))

(def ^:private allowed-tag #"(?i)</?(details|summary|kbd|br)\s*/?>")

(defn- allowed-html? [s]
  (let [tags (re-seq #"<[^>]*>" s)]
    (and (seq tags) (every? #(re-matches allowed-tag %) tags))))

(defn- comment? [s] (re-matches #"(?s)\s*<!--.*-->\s*" s))

(defn- strip-front-matter [s]
  (str/replace s #"(?s)\A---\n.*?\n---\n" ""))

(defn- draft-marker?
  "Template files mark unreviewed drafts with a leading <!-- DRAFT: … --> comment."
  [s]
  (boolean (re-find #"(?s)\A\s*<!--\s*DRAFT" s)))

(defn- walk-ast [f node]
  (let [node (f node)]
    (if (vector? (:content node))
      (update node :content (fn [c] (mapv #(walk-ast f %) c)))
      node)))

(defn- transform [ast rewrite]
  (let [seen (atom {})]
    (walk-ast
     (fn [node]
       (case (:type node)
         :heading (let [slug (github-slug (md/node->text node))
                        n    (get @seen slug 0)]
                    (swap! seen update slug (fnil inc 0))
                    (assoc-in node [:attrs :id] (if (zero? n) slug (str slug "-" n))))
         :link    (update-in node [:attrs :href] rewrite)
         :image   (update-in node [:attrs :src] rewrite)
         node))
     ast)))

(defn- renderers [lang]
  (let [html (fn [_ctx node]
               (let [s (md/node->text node)]
                 (cond (comment? s)       nil
                       (allowed-html? s)  (h/raw s)
                       :else              s)))]
    (assoc mdt/default-hiccup-renderers
           :html-inline html
           :html-block  html
           :code (fn [ctx node]
                   (if (= "mermaid" (:language node))
                     [:details.diagram
                      [:summary (t lang :template/diagram)]
                      [:pre [:code (md/node->text node)]]]
                     ((:code mdt/default-hiccup-renderers) ctx node))))))

(defn render-markdown
  "{:hiccup … :title … :draft? …} for Markdown text from repo path `from`."
  [lang text rewrite]
  (let [text (strip-front-matter text)
        ast  (transform (md/parse text) rewrite)
        h1   (first (filter #(and (= :heading (:type %)) (= 1 (:heading-level %))) (:content ast)))]
    {:hiccup (mdt/->hiccup (renderers lang) ast)
     :title  (some-> h1 md/node->text)
     :draft? (draft-marker? text)}))

;; ---- How-to namespaces -----------------------------------------------------

(defn howtos
  "Every src/howto/*.clj in the checkout: {:path :name :doc :source}."
  [dir]
  (for [f (sort (map str (fs/glob (fs/path dir "src" "howto") "*.clj")))
        :let [source (slurp f)
              nm     (second (re-find #"\(ns\s+([\w.\-]+)" source))
              doc    (some-> (re-find #"\(ns\s+[\w.\-]+\s+\"((?:[^\"\\]|\\.)*)\"" source)
                             second (str/replace #"\\\"" "\""))]]
    {:title  (or (some-> doc str/split-lines first str/trim (str/replace #"\.$" "")) nm)
     :path   (str (fs/relativize dir f))
     :name   (str/replace (fs/file-name f) #"\.clj$" "")
     :ns     nm
     :doc    doc
     :source source}))

;; ---- Pages -----------------------------------------------------------------

(defn- source-line [ctx url path sha]
  [:p.post-meta (t (:lang ctx) :template/source) " "
   [:a {:href url} (str path " @ " (subs sha 0 7))]])

(defn- hub-route
  "Which toolbelt slot a template page belongs to."
  [hub] (if (= hub "/start/") :start :learn))

(defn pages
  "Hub pages built from the template: [{:path :route :page (fn [ctx])}].
  Also returns :learn-index data for the Learn page."
  [site]
  (let [{:template/keys [sha pages]} (config)
        dir     (ensure-checkout! (:template/url site) sha)
        hub-for (into {} (map (juxt :path :hub) pages))
        hows    (howtos dir)
        hub-for (into hub-for (map (fn [x] [(:path x) (str "/learn/howto/" (:name x) "/")]) hows))
        doc-page
        (fn [{:keys [path hub kind]}]
          {:path hub :route (hub-route hub) :kind kind
           :title (-> (slurp (str (fs/path dir path))) strip-front-matter
                      (->> (re-find #"(?m)^# (.+)$")) second)
           :page
           (fn [ctx]
             (let [lang    (:lang ctx)
                   rewrite (link-rewriter {:site site :sha sha :dir dir :hub-for hub-for
                                           :hub-href #(layout/href ctx (str (layout/lang-prefix lang) %))}
                                          path)
                   {:keys [hiccup title draft?]} (render-markdown lang (slurp (str (fs/path dir path))) rewrite)
                   route (hub-route hub)]
               {:route route
                :title (or title path)
                :main  (list
                        (if (= route :start)
                          (layout/crumbs ctx (t lang :route/start))
                          (layout/crumbs ctx [:a {:href (layout/route-href ctx :learn)} (t lang :route/learn)]
                                         (or title path)))
                        [:article.panel.doc
                         (when draft? [:p.notice {:role "note"} (t lang :template/draft-notice)])
                         [:div.prose hiccup]
                         (source-line ctx (github-url site sha dir path) path sha)
                         (layout/primary-button ctx)])}))})
        howto-page
        (fn [{:keys [path name ns doc source title]}]
          {:path (str "/learn/howto/" name "/") :route :learn :kind :how-to :title title
           :page
           (fn [ctx]
             (let [lang (:lang ctx)]
               {:route :learn
                :title title
                :main  (list
                        (layout/crumbs ctx [:a {:href (layout/route-href ctx :learn)} (t lang :route/learn)] title)
                        [:article.panel.doc
                         [:h1 title]
                         [:p.post-meta [:code ns]]
                         (when-let [more (some->> doc str/split-lines rest (map str/trim) (str/join " ") not-empty)]
                           [:p more])
                         [:pre.source [:code.language-clojure source]]
                         (source-line ctx (github-url site sha dir path) path sha)
                         (layout/primary-button ctx)])}))})]
    (concat (map doc-page pages) (map howto-page hows))))

(defn learn-page
  "/learn/: the template's guides by kind, then the optional AI shelf."
  [site template-pages]
  (fn [ctx]
    (let [lang (:lang ctx)
          link (fn [{:keys [path title]}]
                 [:li [:a {:href (layout/href ctx (str (layout/lang-prefix lang) path))} title]])
          by   (group-by :kind template-pages)
          drawer (fn [kind k]
                   [:section.drawer
                    [:h2 (t lang k) " " [:span.count (str "(" (count (by kind)) ")")]]
                    (if (seq (by kind))
                      [:ul (map link (sort-by :title (by kind)))]
                      [:p (t lang :learn/none)])])]
      {:route :learn
       :title (t-str lang :route/learn)
       :main  (list
               (layout/crumbs ctx (t lang :route/learn))
               [:section.panel
                [:h1 (t lang :route/learn)]
                [:p (t lang :learn/intro)]
                [:p [:a {:href (:template/url site)} (t lang :learn/get-template)]]
                (layout/primary-button ctx)]
               [:div.drawers
                (drawer :tutorial :learn/tutorials)
                (drawer :how-to :learn/how-to)
                (drawer :reference :learn/reference)
                (drawer :explanation :learn/explanation)]
               [:section.panel
                [:h2 (t lang :learn/ai)]
                [:p (t lang :learn/ai-note)]
                [:ul (map link (sort-by :title (by :ai)))]])})))

;; ---- bb template:bump ------------------------------------------------------

(defn bump!
  "Point data/template.edn at the template's latest main."
  [& _]
  (let [url  (:template/url (edn/read-string (slurp "data/site.edn")))
        line (:out (p/shell {:out :string} "git" "ls-remote" (str url ".git") "refs/heads/main"))
        sha  (first (str/split (str/trim line) #"\s+"))
        f    "data/template.edn"
        s    (slurp f)
        old  (second (re-find #":template/sha \"([0-9a-f]{40})\"" s))]
    (when-not (re-matches #"[0-9a-f]{40}" (str sha))
      (throw (ex-info "Couldn't read the template's main commit" {:out line})))
    (if (= old sha)
      (println "Already at" sha)
      (do (spit f (str/replace s old sha))
          (println "template.edn:" old "->" sha)
          (println "Next: bb check, then open a PR. Changes:" (str url "/compare/" old "..." sha))))))
