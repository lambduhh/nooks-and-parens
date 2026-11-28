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
       ;; Only the session's number: its format and day stay private until it's
       ;; :announced? (see nooks.pages.common/session-list).
       :session (when live (get-in live [:day/session :n]))
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
                    (t lang :home/today-session (i18n/pad2 (get-in live [:day/session :n])))))))))

(defn- battery
  "Puzzles out so far as a cordless-tool battery (design-system power-tool
  backlog; Jordan picked it in QA, 2026-11-28). The words carry the state;
  the bars are decoration."
  [lang as-of days]
  (let [total (count days)
        out   (count (filter #(state/unlocked? as-of %) days))]
    [:div.battery
     [:span.cells {:aria-hidden "true"}
      (for [i (range total)]
        [:span {:class (if (< i out) "cell on" "cell")}])]
     [:span.battery-text (t lang :home/battery out total)]]))

(defn- puzzle-card
  "Today's puzzle as a link (never embedded: puzzles are linked, not copied),
  and the day's DIY recipes, each opening in the Workbench."
  [ctx day]
  (let [lang    (:lang ctx)
        ;; recipes.edn is a vector in learning order (since the Workbench
        ;; exercises); index it by :id to look up a day's recipe ids.
        recipes (into {} (map (juxt :id identity)) (get-in ctx [:data :recipes]))]
    [:div.puzzle-card
     [:p [:a {:href (:day/puzzle-url day)}
          (t lang :home/puzzle-link (i18n/pad2 (:day/n day)))]]
     (when-let [ks (seq (filter recipes (:day/recipes day)))]
       [:p (t lang :label/diy-recipes) ": "
        (interpose ", "
                   (for [k ks]
                     [:a {:href (str (layout/route-href ctx :workbench) "#" (name k))}
                      (get-in recipes [k :title])]))])]))

(defn page [ctx]
  (let [{:keys [lang as-of]} ctx
        {:keys [site days]} (:data ctx)
        today (state/today-day as-of days)
        upcoming (state/next-sessions as-of days 3)]
    {:route :home
     :title (t-str lang :route/home)
     :main
     (list
      [:h1.sr-only (:site/name site)]
      [:div.dialog
       [:p.line1 (:site/positioning site)
        ;; Power-tool icons, decoration only: the line reads as its words.
        (when-let [ids (seq (:site/positioning-icons site))]
          (list " " [:span.ptools (for [id ids] (layout/icon (name id)))]))]
       ;; What the site is, for someone who just landed (Jordan's QA, 2026-11-28).
       [:p.intro (t lang :home/intro)
        (when (:template/public? site)
          (list " " [:a {:href (:template/url site)} (t lang :home/intro-template)]))]
       (when-let [post (:site/intro-post site)]
         [:p.intro [:a {:href (layout/href ctx post)} (t lang :home/intro-blog)]])
       [:span.more {:aria-hidden "true"} "▼"]]

      [:div.cols
       [:section.panel {:aria-labelledby "mm"}
        [:h2#mm (t lang :home/menu)]
        [:ul.menu
         ;; Toolbelt order; each row's number is its toolbelt shortcut key.
         (for [[route desc] (sort-by (comp layout/toolbelt-key first)
                                     [[:today (if today
                                                (t lang :home/menu-today-day (i18n/pad2 (:day/n today)))
                                                (t lang :home/menu-today-soon
                                                   (i18n/day-label (state/utc-date (:day/unlock (first days))))))]
                                      [:calendar (t lang :home/menu-calendar)]
                                      [:workbench (t lang :home/menu-workbench)]
                                      [:start (t lang :home/menu-start)]])]
           [:li [:a {:href (layout/route-href ctx route)}
                 [:span.key {:aria-hidden "true"} (layout/toolbelt-key route)]
                 (t lang (keyword "route" (name route)))
                 [:small desc]]])]
        (layout/primary-button ctx)]

       [:section.panel {:aria-labelledby "td"}
        [:h2#td (t lang :home/today (i18n/day-label (state/utc-date as-of)))]
        [:p (today-line ctx days)]
        ;; Only while the event runs: after it, "All 12 days are out." says it.
        (when (and today (not (state/archive? as-of days)))
          (list (puzzle-card ctx today) (battery lang as-of days)))
        (c/readout (t-str lang :home/readout-label) "(today)" (today-value as-of days))]]

      [:section.panel {:aria-labelledby "sched"}
       [:h2#sched.shopsign (t lang :label/shop-hours)]
       [:p.sign-sub (t lang :label/shop-hours-sub)]
       (if (seq upcoming)
         (c/session-list ctx upcoming)
         [:p (t lang :home/sessions-done (count days))])
       [:p [:a {:href (layout/route-href ctx :calendar)} (t lang :home/see-all (count days))]]])}))
