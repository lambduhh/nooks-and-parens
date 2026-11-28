(ns nooks.workbench-test
  "The Workbench page's HTML. Behaviour in a browser is bb test:browser."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [nooks.build :as build]
            [nooks.data :as data]
            [nooks.pages.workbench :as wb])
  (:import [java.time Instant]))

(deftest value-kind
  (is (= "map" (wb/value-kind "{:a 1}")))
  (is (= "vector" (wb/value-kind "[1 2]")))
  (is (= "set" (wb/value-kind "#{:saw :level}")))
  (is (= "seq" (wb/value-kind "(1 2)")))
  (is (= "string" (wb/value-kind "\"hi\"")))
  (is (= "number" (wb/value-kind "42")))
  (is (= "keyword" (wb/value-kind ":k")))
  (is (= "nil" (wb/value-kind "nil")))
  (testing "every recipe's :expect reads as EDN"
    (doseq [{:keys [id expect]} (:recipes (data/load-all)) :when expect]
      (is (string? (wb/value-kind expect)) (str id))))
  (testing "every exercise's :goal reads as EDN"
    (doseq [{:keys [id goal]} (:recipes (data/load-all)) :when goal]
      (is (some? (edn/read-string goal)) (str id)))))

(def out ".work/test-workbench")

(defn- built []
  (build/build! {:as-of (Instant/parse "2026-12-07T12:00:00Z") :out-dir out
                 :work-dir ".work/test-workbench-work" :quiet? true})
  {:en    (slurp (str (fs/path out "workbench" "index.html")))
   :pt-BR (slurp (str (fs/path out "pt-br" "workbench" "index.html")))
   :home  (slurp (str (fs/path out "index.html")))})

(deftest page
  (let [{:keys [en pt-BR home]} (built)
        recipes (:recipes (data/load-all))]
    (doseq [[lang html] {:en en :pt-BR pt-BR}]
      (testing (name lang)
        (is (= 1 (count (re-seq #"<textarea\b" html))) "one real textarea")
        (is (re-find #"<label for=\"wb-code\"" html) "the textarea has a label")
        (is (re-find #"aria-live=\"polite\"" html) "results are announced")
        (testing "buttons say what they do (labels: Jordan, 2026-11-24)"
          (is (re-find #"<button class=\"craft\" id=\"wb-craft\" type=\"button\">Craft it: run code</button>" html))
          (is (re-find #"id=\"wb-clear\" type=\"button\">Clear code</button>" html))
          (is (re-find #"id=\"wb-stop\" type=\"button\">Stop running code</button>" html))
          (is (re-find #">Load a starter recipe \(replaces your code\)</h2>" html))
          (doseq [r recipes]
            (is (str/includes? html (str ">Load: " (:title r) "<span")) (:title r))))
        (is (not (re-find #"(?i)&quot;solution&quot;|\"solution\"" html)) "solutions never reach the page")
        (is (re-find #"id=\"wb-task\"" html) "the first exercise's task is on the page without JavaScript")
        (is (re-find #"data-worker=\"/[^\"]*/assets/js/workbench-worker\.js\"" html)
            "the evaluator runs in a self-hosted worker")
        (is (re-find #"data-time-limit=\"\d+\"" html) "runs have a time limit")
        (is (re-find #"<button class=\"ghost\" hidden=\"hidden\" id=\"wb-stop\"" html) "a Stop button")
        (is (not (re-find #"<script[^>]*scittle" html))
            "Scittle never runs on the page's main thread (an infinite loop would freeze the tab)")
        (is (re-find #"/learn/howto/errors/\"" html) "errors link the Reading an error how-to")
        (is (not (re-find #"(?i)squint" html)) "no squint, not even a link")
        (doseq [{k :id} recipes
                :let [tag (re-find (re-pattern (str "<button\\b[^>]*\\sid=\"" (name k) "\"[^>]*>")) html)]]
          (is (boolean (and tag (str/includes? tag (str "data-recipe=\"" (name k) "\""))))
              (str "#" (name k) " anchors the starter recipe, for DIY recipe links")))))
    (testing "the build installs the pinned Scittle and its licence"
      (is (fs/exists? (fs/path out "assets" "vendor" "scittle.js")))
      (is (fs/exists? (fs/path out "assets" "vendor" "scittle-LICENSE.txt"))))
    (testing "Scittle loads only on the Workbench"
      (is (not (str/includes? home "scittle.js"))))))
