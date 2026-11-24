(ns nooks.pages.workbench
  "/workbench/: try Clojure in the browser, no install (docs/plan.md
  § Workbench requirements).

  The page works in layers:
  - Without JS: the code, the starter recipes' names, and a line saying the
    Workbench needs JavaScript, with a link to Start here.
  - With JS but no evaluator (scittle.js failed to load): an unedited starter
    recipe still shows its :expect from data/recipes.edn (the offline preview).
  - With Scittle: anything typed is evaluated in the browser.

  assets/js/workbench.js does the evaluating; this namespace only renders HTML
  and hands the script its strings and recipes as data attributes (the CSP
  forbids inline scripts, and bb check fails a page that has one)."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [nooks.i18n :as i18n :refer [t t-str]]
            [nooks.layout :as layout]))

(defn value-kind
  "The kind label for a printed result, as workbench.js computes it in
  Scittle. Used for the offline preview's labels; bb test:browser checks the
  two agree for every recipe."
  [printed]
  (let [v (edn/read-string printed)]
    (cond (nil? v) "nil" (map? v) "map" (vector? v) "vector" (set? v) "set"
          (seq? v) "seq" (string? v) "string" (keyword? v) "keyword"
          (symbol? v) "symbol" (number? v) "number" (boolean? v) "boolean"
          :else "value")))

(def ^:private js-strings
  "Strings workbench.js shows, by its key. Drafts arrive prefixed [draft]
  (t-str), so they stay visible as placeholders."
  {:statusReady   :workbench/status-ready
   :statusStarting :workbench/status-starting
   :starting      :workbench/starting
   :busyKind      :workbench/busy-kind
   :busy          :workbench/busy
   :stoppedKind   :workbench/stopped-kind
   :stoppedTimeout :workbench/stopped-timeout
   :stoppedByYou  :workbench/stopped-by-you
   :statusOffline :workbench/status-offline
   :waitingKind   :workbench/waiting-kind
   :waiting       :workbench/waiting
   :loaded        :workbench/loaded
   :emptyKind     :workbench/empty-kind
   :empty         :workbench/empty
   :errorKind     :workbench/error-kind
   :errorAt       :workbench/error-at
   :offlineKind   :workbench/offline-kind
   :offline       :workbench/offline})

(def time-limit
  "Seconds a run may take before the Workbench stops it (the infinite-loop
  failsafe in assets/js/workbench.js). Long enough for honest work on small
  made-up data; short enough that a stuck loop is obvious."
  10)

(defn- lang-href
  "A path under the page's language root, e.g. /pt-br/learn/howto/errors/."
  [ctx path]
  (layout/href ctx (str (layout/lang-prefix (:lang ctx)) path)))

(defn recipes
  "[key recipe] pairs in data/recipes.edn order. (EDN keeps a map's order up
  to 8 keys; past that, sort here.)"
  [ctx]
  (seq (get-in ctx [:data :recipes])))

(defn page [ctx]
  (let [lang (:lang ctx)
        rs   (recipes ctx)
        [_ first-recipe] (first rs)]
    {:route :workbench
     :title (t-str lang :route/workbench)
     ;; Scittle itself loads inside the worker (workbench-worker.js), not here.
     :head  [:script {:src (layout/href ctx "/assets/js/workbench.js") :defer true}]
     :main
     (list
      (layout/crumbs ctx (t lang :route/workbench))
      [:section.bench {:id "wb" :aria-labelledby "wb-title"
                       :data-worker (layout/href ctx "/assets/js/workbench-worker.js")
                       :data-time-limit (str time-limit)
                       :data-strings (json/generate-string
                                      (update-vals js-strings #(t-str lang %)))}
       [:div.top
        [:h1#wb-title (layout/emoji :workbench) (t lang :label/workbench)]
        [:span.status {:id "wb-status"} (t lang :workbench/status-no-js)]]
       [:div.area
        [:div
         [:p.small.intro (t lang :workbench/intro)]
         [:label {:for "wb-code"} (t lang :workbench/code-label)]
         [:textarea {:id "wb-code" :spellcheck "false" :autocapitalize "off"
                     :autocomplete "off" :aria-describedby "wb-code-help"}
          (:code first-recipe)]
         [:p.small {:id "wb-code-help"} (t lang :workbench/code-help)]
         ;; Hidden until workbench.js runs: these buttons do nothing without it.
         [:div {:id "wb-controls" :hidden true}
          [:div.row
           [:button.craft {:id "wb-craft" :type "button"} (t lang :workbench/craft)]
           [:button.ghost {:id "wb-stop" :type "button" :hidden true} (t lang :workbench/stop)]
           [:button.ghost {:id "wb-clear" :type "button"} (t lang :workbench/clear)]]
          [:h2.starters-h (t lang :workbench/starters)]
          [:ul.starters
           (for [[k r] rs]
             [:li [:button.ghost {:type "button" :id (name k)
                                  :data-recipe (name k)
                                  :data-title (:title r)
                                  :data-code (:code r)
                                  :data-expect (:expect r)
                                  :data-kind (value-kind (:expect r))}
                   (t lang :workbench/load (:title r))]])]]]
        [:div
         [:h2.result-h (t lang :workbench/result)]
         [:div.slotout {:id "wb-out"}
          ;; The live region: results and errors are announced as they land.
          [:div {:aria-live "polite" :aria-atomic "true"}
           [:span.kind {:id "wb-kind"} (t lang :workbench/no-js-kind)]
           [:pre {:id "wb-value"} (t lang :workbench/no-js)]]
          [:div {:id "wb-printed" :hidden true}
           [:h3.printed-h (t lang :workbench/printed)]
           [:pre {:id "wb-printed-text"}]]
          [:p.small {:id "wb-errhelp" :hidden true}
           (t lang :workbench/error-help) " "
           [:a {:href (lang-href ctx "/learn/howto/errors/")} (t lang :workbench/error-link)]]]]]]
      [:section.panel {:aria-labelledby "wb-know"}
       [:h2#wb-know (t lang :workbench/know)]
       [:ul
        [:li (t lang :workbench/know-data)]
        [:li (t lang :workbench/know-local)]
        [:li (t lang :workbench/know-time-limit time-limit)]
        [:li (t lang :workbench/know-no-ai)]
        [:li (t lang :workbench/know-scratchpad) " "
         [:a {:href (layout/route-href ctx :start)} (t lang :route/start)]]
        [:li (t lang :workbench/know-credit) " "
         [:a {:href "https://github.com/babashka/scittle"} "Scittle"]]]
       (layout/primary-button ctx)])}))
