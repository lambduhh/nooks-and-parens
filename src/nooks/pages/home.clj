(ns nooks.pages.home
  "Landing page /: dialog, main menu, primary button, Today panel with the
  REPL-as-data readout, and the next three sessions."
  (:require [nooks.i18n :as i18n :refer [t t-str]]
            [nooks.layout :as layout]
            [nooks.pages.common :as c]
            [nooks.state :as state]))

(defn- today-value
  "What `(today)` returns in the readout. Data first: this is the hero visual."
  [as-of days]
  (let [d    (state/today-day as-of days)
        live (state/session-today as-of days)]
    (cond
      (nil? d)
      {:puzzles-start (i18n/day-label (state/utc-date (:day/unlock (first days))))
       :first-session (i18n/day-label (state/utc-date (get-in (first days) [:day/session :date])))}

      (state/archive? as-of days)
      {:days (count days) :open (state/open-days as-of days)}

      :else
      {:day     (:day/n d)
       :session (when live {:covers (:day/n live) :format (get-in live [:day/session :format])})
       :open    (state/open-days as-of days)})))

(defn- today-line [ctx days]
  (let [{:keys [lang as-of]} ctx
        d    (state/today-day as-of days)
        live (state/session-today as-of days)]
    (cond
      (nil? d)
      (t lang :home/today-soon
         (i18n/day-label (state/utc-date (:day/unlock (first days))))
         (i18n/day-label (state/utc-date (get-in (first days) [:day/session :date]))))

      (state/archive? as-of days)
      (t lang :home/today-archive (count days))

      :else
      (list (t lang :home/today-day (i18n/pad2 (:day/n d)))
            " " (c/chip lang (state/status as-of d))
            (when live
              (list [:br]
                    (t lang :home/today-live (:day/n live)
                       (t-str lang (keyword "format" (name (get-in live [:day/session :format]))))
                       (i18n/pad2 (:day/n live)))))))))

(defn page [ctx]
  (let [{:keys [lang as-of]} ctx
        {:keys [site days]} (:data ctx)
        today (state/today-day as-of days)
        upcoming (state/next-sessions as-of days 3)
        framing (if (:site/framing-final? site)
                  (:site/framing site)
                  [:span.draft (:site/framing site)])]
    {:route :home
     :title (t-str lang :route/home)
     :main
     (list
      [:h1.sr-only (:site/name site)]
      [:div.dialog
       [:span.who (:site/name site)]
       [:p.line1 (:site/positioning site)]
       [:p framing]
       [:span.more {:aria-hidden "true"} "▼"]]

      [:div.cols
       [:section.panel {:aria-labelledby "mm"}
        [:h2#mm (t lang :home/menu)]
        [:ul.menu
         (for [[i route desc] [[1 :start (t lang :home/menu-start)]
                               [2 :today (if today
                                           (t lang :home/menu-today-day (i18n/pad2 (:day/n today)))
                                           (t lang :home/menu-today-soon
                                              (i18n/day-label (state/utc-date (:day/unlock (first days))))))]
                               [3 :calendar (t lang :home/menu-calendar)]
                               [4 :workbench (t lang :home/menu-workbench)]]]
           [:li [:a {:href (layout/route-href ctx route)}
                 [:span.key {:aria-hidden "true"} i]
                 (t lang (keyword "route" (name route)))
                 [:small desc]]])]
        (layout/primary-button ctx)
        [:span.pill-note (t lang :home/cta-note)]]

       [:section.panel {:aria-labelledby "td"}
        [:h2#td (t lang :home/today (i18n/day-label (state/utc-date as-of)))]
        [:p (today-line ctx days)]
        (c/readout (t-str lang :home/readout-label) "(today)" (today-value as-of days))]]

      [:section.panel {:aria-labelledby "sched"}
       [:h2#sched.shopsign (t lang :label/replin)]
       (if (seq upcoming)
         (c/session-list ctx upcoming)
         [:p (t lang :home/sessions-done (count days))])
       [:p [:a {:href (layout/route-href ctx :calendar)} (t lang :home/see-all (count days))]]])}))
