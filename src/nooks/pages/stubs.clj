(ns nooks.pages.stubs
  "MVP placeholder pages. Each has one primary button and draft copy; each is
  replaced by the real page in its phase (docs/plan.md)."
  (:require [nooks.i18n :as i18n :refer [t t-str]]
            [nooks.layout :as layout]
            [nooks.pages.common :as c]
            [nooks.pages.home :as home]
            [nooks.pages.workbench :as workbench]
            [nooks.state :as state]))

(defn videos [ctx]    (c/stub-page ctx :videos :stub/videos))

(defn about [ctx]
  (c/stub-page ctx :about :stub/about
               [:h2 (t (:lang ctx) :about/credits)]
               [:ul
                [:li [:a {:href "https://github.com/borkdude/quickblog"} "quickblog"] " · "
                 [:a {:href "https://github.com/borkdude"} "borkdude"]]
                [:li [:a {:href "https://github.com/babashka/scittle"} "Scittle"]]
                [:li [:a {:href "https://babashka.org"} "babashka"]]]))

(defn- format-anchor [id] (str "format-" (name id)))

(defn- pool-facts
  "Length, people, what to prepare for a pool format; drafts until written."
  [lang {:format/keys [length people prepare example-video]}]
  (let [field (fn [label-k v] (list [:dt (t lang label-k)] [:dd (or v (t lang :formats/tbd))]))]
    (list
     [:dl.facts
      (field :formats/length length)
      (field :formats/people people)
      (field :formats/prepare prepare)]
     (when example-video
       [:p [:a {:href example-video} (t lang :formats/example)]]))))

(defn- guide-entry
  "One format in the guide: name, \"Favorite\" in words, Jordan's text,
  and the practical facts when it's in this December's pool."
  [lang pool-by-id {:format/keys [id name text pool favorite?]}]
  [:li {:id (format-anchor id)}
   [:h4 name (when favorite? (list " " [:span.chip.fav (t lang :formats/favorite)]))]
   [:p text]
   (when pool (pool-facts lang (pool-by-id pool)))])

(defn formats
  "/formats/: Jordan's session formats guide (data/formats.edn :formats/guide),
  then this campaign's additions to the pool and the managers note. The
  favorites table is built from the pool's :favorite formats, so its names are
  the same labels the schedule uses."
  [ctx]
  (let [lang (:lang ctx)
        {:formats/keys [pool guide sources]} (get-in ctx [:data :formats])
        pool-by-id (into {} (map (juxt :format/id identity)) pool)
        entries    (mapcat :group/formats (:guide/groups guide))
        ;; Where a pool format's row in the table links: its first guide entry
        ;; (:pairing-mob has two, pairing and mob).
        entry-for  (fn [pool-id] (some #(when (= pool-id (:format/pool %)) (:format/id %)) entries))]
    (c/stub-page ctx :formats :formats/intro
                 [:div.prose.guide
                  [:h2 (:guide/title guide)]
                  [:p (:guide/lede guide)]
                  [:ul
                   (for [{:audience/keys [who text]} (:guide/audiences guide)]
                     [:li [:b who] " — " text])]
                  [:p (:guide/variety guide)]

                  [:h3#favorites (:guide/favorites-title guide)]
                  [:p (:guide/favorites-lede guide)]
                  [:table
                   [:thead [:tr [:th {:scope "col"} (t lang :formats/col-format)]
                            [:th {:scope "col"} (t lang :formats/col-good-for)]]]
                   [:tbody
                    (for [{:format/keys [id source good-for]} pool
                          :when (= :favorite source)]
                      [:tr
                       [:th {:scope "row"}
                        [:a {:href (str "#" (format-anchor (entry-for id)))}
                         (t lang (keyword "format" (name id)))]]
                       [:td good-for]])]]
                  [:p (:guide/menu-lede guide)]

                  (for [{:group/keys [id title blurb formats]} (:guide/groups guide)]
                    [:section {:aria-labelledby (str "group-" (name id))}
                     [:h3 {:id (str "group-" (name id))} title]
                     [:p.blurb blurb]
                     [:ul.format-list (for [e formats] (guide-entry lang pool-by-id e))]])

                  [:h3#heuristics (:guide/heuristics-title guide)]
                  [:ul
                   (for [{:heuristic/keys [q a]} (:guide/heuristics guide)]
                     [:li [:b q] " " a])]]

                 [:section {:aria-labelledby "extras"}
                  [:h2#extras (t lang :formats/extras-heading)]
                  [:ul.format-list
                   (for [{:format/keys [id source] :as f} pool
                         :when (= :extra source)]
                     [:li {:id (format-anchor id)}
                      [:h3 (t lang (keyword "format" (name id)))]
                      [:p (t lang :formats/tbd)]
                      (pool-facts lang f)])]]

                 [:h2 (t lang :formats/managers-heading)]
                 [:p (t lang :formats/managers)]
                 [:ul
                  (for [{:source/keys [term title author date url caveat-url]} sources]
                    [:li [:b term] ": " [:a {:href url} title] ", " author ", " date
                     (when caveat-url (list " (" [:a {:href caveat-url} (t lang :formats/caveat)] ")"))])])))

(defn calendar
  "Stub until the tool wall lands: the decided heading and the full schedule."
  [ctx]
  (let [lang (:lang ctx)]
    {:route :calendar
     :title (t-str lang :route/calendar)
     :main  (list
             (layout/crumbs ctx (t lang :route/calendar))
             [:section.panel
              [:h1.shopsign (t lang :label/replin)]
              [:p (t lang :stub/calendar)]
              (layout/primary-button ctx)]
             [:section.panel
              (c/session-list ctx (get-in ctx [:data :days]))
              [:p.small (t lang :session/no-session) " "
               [:a {:href (layout/route-href ctx :formats)} (t lang :calendar/formats-link)]]])}))

(defn today-target
  "Where /today/ points as of build time. Day pages aren't built yet (next
  PR), so in December this is the calendar; before 1 Dec, the home page."
  [ctx]
  (if (state/today-day (:as-of ctx) (get-in ctx [:data :days]))
    (layout/route-href ctx :calendar)
    (layout/route-href ctx :home)))

(defn today [ctx]
  (let [lang (:lang ctx)
        target (today-target ctx)]
    {:route :today
     :title (t-str lang :route/today)
     :head  [:meta {:http-equiv "refresh" :content (str "0; url=" target)}]
     :main  (list
             (layout/crumbs ctx (t lang :route/today))
             [:section.panel
              [:h1 (layout/emoji :today) (t lang :route/today)]
              [:p [:a {:href target} (t lang :stub/today-link)]]
              (layout/primary-button ctx)])}))

(defn not-found [ctx]
  (let [lang (:lang ctx)]
    {:route nil
     :title (t-str lang :route/not-found)
     :main  [:section.panel
             [:h1 (layout/emoji :not-found) (t lang :route/not-found)]
             [:p (t lang :stub/not-found)]
             [:p [:a {:href (layout/route-href ctx :home)} (t lang :route/home)] " · "
              [:a {:href (layout/route-href ctx :calendar)} (t lang :route/calendar)]]
             (layout/primary-button ctx)]}))

(def pages
  "Route → page fn for the hub's own pages. :start and :learn come from the
  template repo; see nooks.build/all-pages."
  {:home      home/page
   :today     today
   :calendar  calendar
   :workbench workbench/page
   :videos    videos
   :formats   formats})
