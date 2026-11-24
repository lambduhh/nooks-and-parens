(ns nooks.pages.stubs
  "MVP placeholder pages. Each has one primary button and draft copy; each is
  replaced by the real page in its phase (docs/plan.md)."
  (:require [nooks.i18n :as i18n :refer [t t-str]]
            [nooks.layout :as layout]
            [nooks.pages.common :as c]
            [nooks.pages.home :as home]
            [nooks.state :as state]))

(defn workbench [ctx] (c/stub-page ctx :workbench :stub/workbench))
(defn videos [ctx]    (c/stub-page ctx :videos :stub/videos))

(defn about [ctx]
  (c/stub-page ctx :about :stub/about
               [:h2 (t (:lang ctx) :about/credits)]
               [:ul
                [:li [:a {:href "https://github.com/borkdude/quickblog"} "quickblog"] " · "
                 [:a {:href "https://github.com/borkdude"} "borkdude"]]
                [:li [:a {:href "https://github.com/babashka/scittle"} "Scittle"]]
                [:li [:a {:href "https://babashka.org"} "babashka"]]]))

(defn formats [ctx]
  (let [lang (:lang ctx)]
    (c/stub-page ctx :formats nil
                 [:ul
                  (for [d (get-in ctx [:data :days])
                        :let [{:keys [n format]} (:day/session d)]]
                    [:li [:b (t lang (keyword "format" (name format)))]
                     " · " (t lang :formats/session (i18n/pad2 n))])])))

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
              [:p.small (t lang :session/no-session)]])}))

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
   :workbench workbench
   :videos    videos
   :formats   formats})
