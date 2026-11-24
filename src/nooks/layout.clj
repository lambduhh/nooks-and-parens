(ns nooks.layout
  "The page shell every page shares, hub and blog alike: sign header, language
  switch, toolbelt, footer, and the security <meta> tags.

  Every link goes through `href`, which prefixes :site/base-path, so the site
  works under /power-tools-and-parens/ on GitHub Pages and at / on any other host."
  (:require [clojure.string :as str]
            [hiccup2.core :as h]
            [nooks.i18n :as i18n :refer [t t-str]]))

;; ---- Routes ----------------------------------------------------------------

(def routes
  "Route key → path under the language root. Every built page is one of these
  (plus the blog, which quickblog writes)."
  {:home      "/"
   :start     "/start/"
   :today     "/today/"
   :calendar  "/days/"
   :workbench "/workbench/"
   :videos    "/videos/"
   :learn     "/learn/"
   :formats   "/formats/"
   :blog      "/blog/"})

(def toolbelt
  "Toolbelt slots, in key order, with their icon. Keys start at 0: slot i is
  number key i (Jordan, 2026-11-24). Slot 7 (placeholder) and About are out
  for the alpha."
  [[:today "sun"] [:calendar "cal"] [:workbench "bench"] [:learn "book"]
   [:videos "film"] [:start "map"]])

(def toolbelt-labels
  "Slots whose toolbelt label differs from the route name. Only the toolbelt
  says \"First time?\"; the page itself is still \"Start here\" (Jordan, 2026-11-24)."
  {:start :toolbelt/start})

(defn toolbelt-key
  "The number key for a route's toolbelt slot (0-based), or nil."
  [route]
  (first (keep-indexed (fn [i [r _]] (when (= r route) i)) toolbelt)))

(def lang-prefix {:en "" :pt-BR "/pt-br"})

(defn href
  "Absolute path for a site-relative path, under the base path."
  [ctx path]
  (str (get-in ctx [:data :site :site/base-path]) path))

(defn route-href
  ([ctx route] (route-href ctx (:lang ctx) route))
  ([ctx lang route]
   (let [path (or (routes route) (throw (ex-info "Unknown route" {:route route})))]
     ;; The blog is English only for now; every language links the English blog.
     (href ctx (if (= route :blog) path (str (lang-prefix lang) path))))))

;; ---- Pieces ----------------------------------------------------------------

(def csp
  "GitHub Pages can't set response headers, so the policy ships as a <meta>
  tag (which can't carry frame-ancestors). Same-origin only; YouTube frames
  only from the no-cookie domain."
  (str/join "; " ["default-src 'self'"
                  "script-src 'self'"
                  "style-src 'self'"
                  "font-src 'self'"
                  "img-src 'self' data:"
                  "connect-src 'self'"
                  "frame-src https://www.youtube-nocookie.com"
                  "object-src 'none'"
                  "base-uri 'self'"
                  "form-action 'self'"]))

(def sprite
  "Icon symbols, referenced by <use>. Every <svg> on the site is decorative and
  carries aria-hidden; text always says the same thing."
  [:svg {:class "sprite" :width "0" :height "0" :aria-hidden "true" :focusable "false"}
   [:symbol {:id "t-hammer" :viewBox "0 0 48 48"} [:path {:d "M8 6h26l6 5v6H8z"}] [:rect {:x "20" :y "17" :width "7" :height "27" :rx "3"}]]
   [:symbol {:id "t-screwdriver" :viewBox "0 0 48 48"} [:rect {:x "18" :y "3" :width "12" :height "18" :rx "5"}] [:rect {:x "22" :y "21" :width "4" :height "20"}] [:path {:d "M22 41h4l-2 4z"}]]
   [:symbol {:id "t-wrench" :viewBox "0 0 48 48"} [:path {:d "M30 4a10 10 0 0 0-9 14L5 34l9 9 16-16A10 10 0 0 0 44 18l-7 2-5-5 2-7a10 10 0 0 0-4-4z"}]]
   [:symbol {:id "t-saw" :viewBox "0 0 48 48"} [:path {:d "M16 12h28v14L16 38z"}] [:path {:d "M3 10h13v18H3z"}]]
   [:symbol {:id "t-level" :viewBox "0 0 48 48"} [:rect {:x "3" :y "17" :width "42" :height "14" :rx "4"}] [:rect {:x "19" :y "20" :width "10" :height "8" :rx "4"}]]
   [:symbol {:id "t-tape" :viewBox "0 0 48 48"} [:path {:d "M22 8a16 16 0 1 0 16 16V8z"}] [:rect {:x "34" :y "34" :width "12" :height "6"}] [:circle {:cx "22" :cy "24" :r "5"}]]
   [:symbol {:id "t-map" :viewBox "0 0 48 48"} [:path {:d "M4 10l13-5 14 5 13-5v33l-13 5-14-5-13 5z"}] [:path {:d "M17 5v33M31 10v33" :fill "none"}]]
   [:symbol {:id "t-cal" :viewBox "0 0 48 48"} [:rect {:x "5" :y "9" :width "38" :height "34" :rx "5"}] [:path {:d "M5 19h38M15 4v10M33 4v10" :fill "none"}]]
   [:symbol {:id "t-bench" :viewBox "0 0 48 48"} [:rect {:x "3" :y "14" :width "42" :height "8" :rx "2"}] [:path {:d "M8 22v20h5V22M35 22v20h5V22"}]]
   [:symbol {:id "t-film" :viewBox "0 0 48 48"} [:rect {:x "4" :y "10" :width "40" :height "28" :rx "5"}] [:path {:d "M20 18l10 6-10 6z"}]]
   [:symbol {:id "t-book" :viewBox "0 0 48 48"} [:path {:d "M6 8h15a5 5 0 0 1 5 5v29a4 4 0 0 0-4-4H6zM42 8H31a5 5 0 0 0-5 5v29a4 4 0 0 1 4-4h12z"}]]
   [:symbol {:id "t-shelf" :viewBox "0 0 48 48"} [:rect {:x "4" :y "8" :width "40" :height "34" :rx "3"}] [:path {:d "M4 25h40M12 8v17M20 8v17M32 25v17" :fill "none"}]]
   [:symbol {:id "t-sun" :viewBox "0 0 48 48"} [:circle {:cx "24" :cy "24" :r "10"}] [:path {:d "M24 3v7M24 38v7M3 24h7M38 24h7M9 9l5 5M34 34l5 5M39 9l-5 5M14 34l-5 5" :fill "none"}]]
   [:symbol {:id "t-info" :viewBox "0 0 48 48"} [:circle {:cx "24" :cy "24" :r "19"}] [:path {:d "M24 21v13M24 14v1" :fill "none"}]]])

(defn icon [id]
  [:svg {:aria-hidden "true" :focusable "false"} [:use {:href (str "#t-" id)}]])

(def tool-emoji
  "Power-tool emoji for headings, by route or Learn drawer. Decoration only:
  aria-hidden, so headings read as their words (Jordan, 2026-11-24)."
  {:start "🔌" :workbench "🪚" :learn "🧰" :formats "🔧" :videos "🪜"
   :today "🔦" :not-found "🔩"
   :tutorial "🔨" :how-to "🪛" :reference "📏" :explanation "⚙️" :blog "🪧"})

(defn emoji
  "The emoji for k as a decorative prefix, or nil."
  [k]
  (when-let [e (tool-emoji k)] (list [:span.emoji {:aria-hidden "true"} e] " ")))

(defn tool-icon
  "Icon for a day's :day/tool keyword."
  [tool]
  (icon (name tool)))

(defn primary-button
  "The page's one primary action. `bb check` fails a page with zero or two."
  ([ctx] (primary-button ctx :slack))
  ([ctx kind & [label]]
   (let [site (get-in ctx [:data :site])]
     (case kind
       :slack   [:a.pill {:href (or (:cta/slack-channel site) (get-in site [:cta/slack :url]))}
                 (or label (get-in site [:cta/slack :label]))]
       :discord [:a.pill {:href (get-in site [:cta/discord :url])} label]))))

(defn crumbs [ctx & items]
  [:nav.crumbs {:aria-label (t-str (:lang ctx) :crumbs/label)}
   [:a {:href (route-href ctx :home)} (t (:lang ctx) :crumbs/home)]
   (for [item items] (list " › " item))])

(defn- sign-header [ctx]
  (let [{:keys [lang route]} ctx
        site     (get-in ctx [:data :site])
        other    (if (= lang :en) :pt-BR :en)
        ;; The blog has one pt-BR page: a "coming soon" notice.
        twin     (cond
                   (#{:blog :blog-pt} route)
                   (if (= lang :en) (href ctx "/pt-br/blog/") (route-href ctx :en :blog))
                   ;; Pages at their own path (template guides): same path, other language.
                   (:path ctx) (href ctx (str (lang-prefix other) (:path ctx)))
                   :else (route-href ctx other (or route :home)))
        lang-link (fn [l]
                    (if (= l lang)
                      [:a {:aria-current "true" :lang (name l)} (t lang (keyword "lang" (name l)))]
                      [:a {:href twin :hreflang (name l) :lang (name l)} (t lang (keyword "lang" (name l)))]))]
    [:header.signpost
     [:div.ropes {:aria-hidden "true"} [:span] [:span]]
     [:a.sign {:href (route-href ctx :home)}
      [:span.name (:site/name site)]
      [:span.tagline (:site/tagline site)
       (when-let [e (:site/tagline-emoji site)]
         (list " " [:span.emoji {:aria-hidden "true"} e]))]]
     [:p.lang {:aria-label (t-str lang :lang/switch)}
      (lang-link :en) " · " (lang-link :pt-BR)]]))

(defn- hotbar [ctx]
  (let [{:keys [lang route]} ctx]
    [:nav.hotbar {:aria-label (t-str lang :toolbelt/label)}
     [:ul
      (map-indexed
       (fn [i [r ic]]
         [:li [:a (cond-> {:href (route-href ctx r) :data-k (str i)}
                    (= r route) (assoc :aria-current "page"))
               [:span.k {:aria-hidden "true"} i]
               (icon ic)
               (t lang (get toolbelt-labels r (keyword "route" (name r))))]])
       toolbelt)]
     [:div.opts
      [:span (t lang :toolbelt/keys-help 0 (dec (count toolbelt)))]
      ;; Hidden until site.js runs: without JS there are no shortcuts to switch.
      [:button {:type "button" :id "np-keys" :aria-pressed "true" :hidden true
                :data-on (t-str lang :toolbelt/keys-on) :data-off (t-str lang :toolbelt/keys-off)}
       (t-str lang :toolbelt/keys-on)]]]))

(defn- footer [ctx]
  (let [lang (:lang ctx)]
    [:footer.foot.wrap
     [:p (t lang :footer/line)]
     [:p [:a {:href (route-href ctx :blog)} (t lang :footer/blog)]]]))

;; ---- Page ------------------------------------------------------------------

(defn page
  "A whole HTML document.

  opts:
    :title       plain text, or (h/raw …) for templates
    :main        hiccup for <main>
    :head        extra hiccup for <head>
    :description optional meta description

  ctx carries :lang (the page's language) and :content-lang (the language the
  content is actually in). When they differ, e.g. an untranslated pt-BR twin,
  the page is lang=pt-BR, its content blocks are lang=en, and a notice says so."
  [ctx {:keys [title main head description]}]
  (let [{:keys [lang content-lang dev?]} ctx
        content-lang (or content-lang lang)
        site   (get-in ctx [:data :site])
        in-lang (when (not= lang content-lang) {:lang (name content-lang)})
        title  (if (string? title) (str title " · " (:site/name site)) title)]
    (str
     "<!doctype html>\n"
     (h/html
      [:html {:lang (name lang)}
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:http-equiv "Content-Security-Policy" :content csp}]
        [:meta {:name "referrer" :content "strict-origin-when-cross-origin"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:title title]
        (when description [:meta {:name "description" :content description}])
        [:link {:rel "icon" :type "image/svg+xml" :href (href ctx "/assets/favicon.svg")}]
        [:link {:rel "stylesheet" :href (href ctx "/assets/css/site.css")}]
        [:link {:rel "preload" :href (href ctx "/assets/fonts/nunito.woff2") :as "font" :type "font/woff2" :crossorigin "anonymous"}]
        head
        [:script {:src (href ctx "/assets/js/site.js") :defer true}]
        (when dev? [:script {:src (href ctx "/assets/js/dev-reload.js") :defer true}])]
       [:body.np
        [:a.skip {:href "#main"} (t lang :skip-link)]
        sprite
        (when in-lang
          [:p.notice {:role "note"} (t lang :i18n/coming-soon)])
        [:div in-lang
         (sign-header ctx)
         [:main.wrap {:id "main" :tabindex "-1"} main]
         (footer ctx)
         (hotbar ctx)]]]))))
