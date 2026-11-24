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

;; ---- Mermaid diagrams ------------------------------------------------------
;;
;; GitHub draws Mermaid in the browser with JavaScript (~3 MB, and it injects
;; inline styles our CSP blocks). We render each diagram once to a static SVG
;; with `bb diagrams` (Kroki, https://kroki.io, sent only the diagram text,
;; which is public in the template repo) and commit it under
;; assets/diagrams/<key>.svg. Builds and visitors never call Kroki. A diagram
;; with no SVG yet falls back to its source in a collapsed <details>.

(def diagram-init
  "Mermaid config prepended before rendering: site colours (docs/design-system.md),
  black text on paper, and plain SVG text labels so the file works as an <img>."
  (str "%%{init: {\"theme\":\"base\",\"flowchart\":{\"htmlLabels\":false},"
       "\"themeVariables\":{\"primaryColor\":\"#fffaf0\",\"primaryBorderColor\":\"#000000\","
       "\"primaryTextColor\":\"#000000\",\"lineColor\":\"#6e4424\",\"secondaryColor\":\"#f3e6cc\","
       "\"tertiaryColor\":\"#ffffff\",\"fontFamily\":\"Trebuchet MS, Helvetica, sans-serif\"}}}%%\n"))

(defn diagram-key
  "Content address of a diagram: changes when its source or the config does."
  [source]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        bs (.digest md (.getBytes (str diagram-init source) "UTF-8"))]
    (subs (apply str (map #(format "%02x" %) bs)) 0 16)))

(defn round-coords
  "Round every decimal with 3+ fraction digits to 2 places. Mermaid's minified
  path data runs numbers together (1.234.567 is 1.234 then .567), which a
  pre-commit PII scanner reads as CPF/CNPJ numbers. Two places is far below a
  pixel at this size; integers and hex colours are untouched, and the path
  data still parses."
  [s]
  (str/replace s #"(\d*)\.(\d{3,})"
               (fn [[_ i f]]
                 (let [r (-> (format "%.2f" (Double/parseDouble (str (if (str/blank? i) "0" i) "." f)))
                             (str/replace #"\.?0+$" ""))
                       r (if (str/blank? i) (str/replace r #"^0" "") r)]
                   (if (str/blank? r) "0" r)))))

(defn diagram-file [k] (fs/path "assets" "diagrams" (str k ".svg")))

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

(defn- diagram-figure [lang href]
  [:figure.diagram
   [:a {:href href} [:img {:src href :alt (t-str lang :template/diagram-alt)}]]
   [:figcaption [:a {:href "#after-diagram"} (t lang :template/diagram-skip)]
    " · " [:a {:href href} (t lang :template/diagram-full)]]])

(defn- renderers [lang {:keys [diagram-href]}]
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
                     (let [src (md/node->text node)
                           k   (diagram-key src)]
                       (if (and diagram-href (fs/exists? (diagram-file k)))
                         (list (diagram-figure lang (diagram-href k)) [:span {:id "after-diagram"}])
                         (do (binding [*out* *err*]
                               (println "note: no SVG for a Mermaid diagram yet; run bb diagrams"))
                             [:details.diagram
                              [:summary (t lang :template/diagram)]
                              [:pre [:code src]]])))
                     ((:code mdt/default-hiccup-renderers) ctx node))))))

(defn- hoist-diagram
  "Move the first Mermaid block to just after the h1."
  [ast]
  (let [c    (:content ast)
        mer? #(and (= :code (:type %)) (= "mermaid" (:language %)))
        d    (first (filter mer? c))
        h1i  (first (keep-indexed (fn [i n] (when (and (= :heading (:type n)) (= 1 (:heading-level n))) i)) c))]
    (if (and d h1i)
      (let [rest-c (vec (remove #(identical? % d) c))]
        (assoc ast :content (vec (concat (subvec rest-c 0 (inc h1i)) [d] (subvec rest-c (inc h1i))))))
      ast)))

(defn render-markdown
  "{:hiccup … :title … :draft? …} for Markdown text from repo path `from`."
  [lang text rewrite & [{:keys [diagram-first?] :as opts}]]
  (let [text (strip-front-matter text)
        ast  (cond-> (transform (md/parse text) rewrite) diagram-first? hoist-diagram)
        h1   (first (filter #(and (= :heading (:type %)) (= 1 (:heading-level %))) (:content ast)))]
    {:hiccup (mdt/->hiccup (renderers lang opts) ast)
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
        (fn [{:keys [path hub kind diagram-first?]}]
          {:path hub :route (hub-route hub) :kind kind
           :title (-> (slurp (str (fs/path dir path))) strip-front-matter
                      (->> (re-find #"(?m)^# (.+)$")) second)
           :page
           (fn [ctx]
             (let [lang    (:lang ctx)
                   rewrite (link-rewriter {:site site :sha sha :dir dir :hub-for hub-for
                                           :hub-href #(layout/href ctx (str (layout/lang-prefix lang) %))}
                                          path)
                   {:keys [hiccup title]} (render-markdown lang (slurp (str (fs/path dir path))) rewrite
                                                           {:diagram-first? diagram-first?
                                                            :diagram-href #(layout/href ctx (str "/assets/diagrams/" % ".svg"))})
                   route (hub-route hub)]
               {:route route
                :title (or title path)
                :main  (list
                        (if (= route :start)
                          (layout/crumbs ctx (t lang :route/start))
                          (layout/crumbs ctx [:a {:href (layout/route-href ctx :learn)} (t lang :route/learn)]
                                         (or title path)))
                        [:article.panel.doc
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

(defn how-to-groups
  "The How-to drawer's items in data/learn.edn's groups and order:
  [{:group k :items [item …]}]. An item's key is its :url (outside resource)
  or its hub :path. Throws unless every how-to is in exactly one group and
  every listed key exists, so a template bump can't drop or invent an entry."
  [groups items]
  (let [k-of    #(or (:url %) (:path %))
        by-key  (into {} (map (juxt k-of identity)) items)
        listed  (mapcat :items groups)
        dupes   (keep (fn [[k n]] (when (> n 1) k)) (frequencies listed))
        unknown (remove by-key listed)
        missing (remove (set listed) (keys by-key))]
    (when (or (seq dupes) (seq unknown) (seq missing))
      (throw (ex-info (str "data/learn.edn doesn't match the How-to drawer."
                           (when (seq missing) (str " Not in any group: " (vec missing) "."))
                           (when (seq unknown) (str " No such how-to: " (vec unknown) "."))
                           (when (seq dupes)   (str " In more than one group: " (vec dupes) ".")))
                      {:missing (vec missing) :unknown (vec unknown) :duplicates (vec dupes)})))
    (for [{:keys [group items]} groups]
      {:group group :items (mapv by-key items)})))

(defn learn-page
  "/learn/:the template's guides and the blog's posts by kind, then the
  optional AI shelf."
  [site template-pages]
  (fn [ctx]
    (let [lang (:lang ctx)
          link (fn [{:keys [path url title shared?]}]
                 [:li [:a {:href (or url (layout/href ctx (if shared? path (str (layout/lang-prefix lang) path))))} title]])
          ;; Outside resources (data/resources.edn) join the drawers by :kind.
          resources (for [{:resource/keys [title url kind]} (get-in ctx [:data :resources])]
                      {:title title :url url :kind kind})
          by   (group-by :kind (concat template-pages resources))
          ;; The How-to drawer is grouped and ordered by data/learn.edn
          ;; (beginner first); the other drawers stay alphabetical.
          groups (how-to-groups (get-in ctx [:data :learn :learn/how-to]) (by :how-to))
          body (fn [kind]
                 (cond
                   (= kind :how-to) (for [{:keys [group items]} groups]
                                      (list [:h3 (t lang group)] [:ul (map link items)]))
                   (= kind :blog)   [:ul (map link (by kind))]
                   :else            [:ul (map link (sort-by :title (by kind)))]))
          drawer (fn [kind k]
                   ;; Empty drawers are left out until they have something in them.
                   (when (seq (by kind))
                   [:section.drawer {:class (str "drawer-" (name kind))}
                    [:h2 (layout/emoji kind) (t lang k) " " [:span.count (str "(" (count (by kind)) ")")]]
                    (body kind)]))]
      {:route :learn
       :title (t-str lang :route/learn)
       :main  (list
               (layout/crumbs ctx (t lang :route/learn))
               [:section.panel
                [:h1 (layout/emoji :learn) (t lang :route/learn)]
                [:p (t lang :learn/intro)]
                [:p [:a {:href (:template/url site)} (t lang :learn/get-template)]]
                (layout/primary-button ctx)]
               ;; Diátaxis compass as two columns: study (tutorials over
               ;; explanation) and work (how-to over reference), practical on
               ;; top. Each column stacks on its own, so a short tile keeps
               ;; its height instead of stretching to match the long How-to.
               ;; Blog isn't a quadrant, so it gets a full row below.
               [:div.drawers.diataxis
                [:div.diataxis-col
                 (drawer :tutorial :learn/tutorials)
                 (drawer :explanation :learn/explanation)]
                [:div.diataxis-col
                 (drawer :how-to :learn/how-to)
                 (drawer :reference :learn/reference)]
                (drawer :blog :learn/blog)]
               [:section.panel
                [:h2 (t lang :learn/ai)]
                [:p (t lang :learn/ai-note)]
                [:ul (map link (sort-by :title (by :ai)))]])})))

;; ---- bb diagrams -----------------------------------------------------------

(defn render-diagrams!
  "Render every Mermaid block in the template pages to assets/diagrams/ via
  Kroki, skipping ones already rendered. Commit the new SVGs."
  [& _]
  (let [site (edn/read-string (slurp "data/site.edn"))
        {:template/keys [sha pages]} (config)
        dir  (ensure-checkout! (:template/url site) sha)
        srcs (for [{:keys [path]} pages
                   node (:content (md/parse (strip-front-matter (slurp (str (fs/path dir path))))))
                   :when (and (= :code (:type node)) (= "mermaid" (:language node)))]
               [path (md/node->text node)])]
    (fs/create-dirs (fs/path "assets" "diagrams"))
    (doseq [[path src] srcs
            :let [k (diagram-key src) f (diagram-file k)]]
      (if (fs/exists? f)
        (println "up to date:" (str f) "(" path ")")
        (let [resp ((requiring-resolve 'babashka.http-client/post) "https://kroki.io/mermaid/svg"
                    {:headers {"Content-Type" "text/plain"} :body (str diagram-init src) :throw false :timeout 60000})
              svg  (:body resp)]
          (when-not (and (= 200 (:status resp)) (str/starts-with? (str/triml svg) "<svg")
                         (not (re-find #"(?i)<script|\s(?:href|src)=\"https?:" svg)))
            (throw (ex-info (str "Kroki didn't return a clean SVG for " path) {:status (:status resp)})))
          (spit (str f) (round-coords svg))
          (println "wrote:" (str f) "(" path ")"))))))

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
