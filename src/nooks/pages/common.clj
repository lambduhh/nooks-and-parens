(ns nooks.pages.common
  "Components shared by several pages."
  (:require [clojure.string :as str]
            [nooks.i18n :as i18n :refer [t]]
            [nooks.layout :as layout]
            [nooks.state :as state]))

(defn chip [lang status]
  [:span {:class (str "chip " (name status))} (t lang (keyword "chip" (name status)))])

(defn guest-line
  "Confirmed guests by name; anyone else is never named."
  [lang guests]
  (let [confirmed (filter :confirmed? guests)]
    (if (seq confirmed)
      (interpose ", " (for [{:keys [name url]} confirmed]
                        (if url [:a {:href url} name] name)))
      (t lang :session/guest-tba))))

(defn session-list
  "The shop list under \"What're ya REPLin'?\": one row per session."
  [ctx days]
  (let [{:keys [lang as-of]} ctx
        videos (get-in ctx [:data :videos])]
    [:ul.shop
     (for [d days
           :let [{:keys [n date format time guests]} (:day/session d)
                 sess (state/session-state as-of d)]]
       [:li
        (layout/tool-icon (:day/tool d))
        [:div
         [:b (t lang :session/title (i18n/pad2 n) (i18n/t-str lang (keyword "format" (name format))))]
         [:div.meta
          (t lang :session/covers (i18n/pad2 (:day/n d)))
          " · " (t lang :session/where)
          " · " (or time (t lang :session/time-tba))
          " · " (guest-line lang guests)]]
        [:div.price
         (cond
           (state/replay? as-of d videos) [:a {:href (layout/route-href ctx :videos)} (t lang :session/replay-ready)]
           (= sess :today)                (chip lang :live)
           :else                          (i18n/day-label (state/utc-date date)))]])]))

;; ---- REPL-as-data readout --------------------------------------------------

(defn- edn-hiccup
  "Clojure data as coloured hiccup: strings green, the rest plain. Maps print
  one entry per line, like the REPL does for small maps."
  [x indent]
  (cond
    (string? x) [:span.s (pr-str x)]
    (map? x)
    (let [pad (apply str (repeat (inc indent) " "))
          w   (apply max 0 (map (comp count str key) x))]
      (list "{"
            (interpose
             (str "\n" pad)
             (for [[k v] x
                   :let [ks (str k)]]
               (list ks (apply str (repeat (- (inc w) (count ks)) " "))
                     (edn-hiccup v (+ indent 1 w 1)))))
            "}"))
    :else (pr-str x)))

(defn readout
  "A black data panel: `user=> (form)` then the value."
  [label form value]
  [:div.clip {:role "group" :aria-label label}
   [:span.p "user=>"] " " form "\n" (edn-hiccup value 0)])

(defn stub-page
  "A simple page: crumbs, a heading, an optional line, extra content, the primary button."
  [ctx route body-key & extra]
  (let [lang (:lang ctx)]
    {:route route
     :title (i18n/t-str lang (keyword "route" (name route)))
     :main  (list
             (layout/crumbs ctx (t lang (keyword "route" (name route))))
             [:section.panel
              [:h1 (layout/emoji route) (t lang (keyword "route" (name route)))]
              (when body-key [:p (t lang body-key)])
              extra
              (layout/primary-button ctx)])}))
