(ns nooks.home-test
  "The landing page's HTML at the date edges: the dialog, the language switch,
  and the Today panel's puzzle link card and battery meter (Jordan's QA,
  2026-11-28)."
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is testing]]
            [nooks.build :as build])
  (:import [java.time Instant]))

(defn- home-at
  "Build the site as of `iso` and return the English and pt-BR landing pages."
  [iso]
  (let [out (str ".work/test-home-" (subs iso 0 10))]
    (build/build! {:as-of (Instant/parse iso) :out-dir out
                   :work-dir (str out "-work") :quiet? true})
    {:en    (slurp (str (fs/path out "index.html")))
     :pt-BR (slurp (str (fs/path out "pt-br" "index.html")))
     :out   out}))

(deftest dialog-and-language-switch
  (let [{:keys [en pt-BR out]} (home-at "2026-11-28T12:00:00Z")]
    (testing "the name pill is gone; the name is still the page's h1"
      (is (not (re-find #"class=\"who\"" en)))
      (is (re-find #"<h1 class=\"sr-only\">Power Tools &amp; Parens</h1>" en)))
    (testing "the positioning line's emoji are decoration"
      (is (re-find #"systems\. <span aria-hidden=\"true\" class=\"emoji\">" en)))
    (testing "the explainer links the template and the announcement post"
      (is (re-find #"<a href=\"https://github.com/lambduhh/clojure-template-aoc\">" en))
      (is (re-find #"<a href=\"/powertools-and-parens/blog/announcement.html\">" en))
      (is (fs/exists? (fs/path out "blog" "announcement.html")) "the linked post is built"))
    (testing "untranslated pt-BR: in words, faded, and not a link from English pages"
      (is (re-find #"<span class=\"soon\" lang=\"pt-BR\">PT-BR · em breve</span>" en))
      (is (not (re-find #"hreflang=\"pt-BR\"" en))))
    (testing "pt-BR pages still link back to English"
      (is (re-find #"<a href=\"/powertools-and-parens/\" hreflang=\"en\"" pt-BR)))
    (testing "before 1 Dec: no puzzle card or battery"
      (is (not (re-find #"puzzle-card|class=\"battery\"" en))))))

(deftest today-panel-during-the-event
  (let [{:keys [en]} (home-at "2026-12-07T12:00:00Z")]
    (testing "the puzzle is a link, never embedded"
      (is (re-find #"<a href=\"https://adventofcode.com/2026/day/7\">" en))
      (is (not (re-find #"<iframe" en))))
    (testing "the day's DIY recipes open in the Workbench"
      (is (re-find #"DIY recipes: <a href=\"/powertools-and-parens/workbench/#six-of-one\">" en)))
    (testing "the battery says the count in words; the cells match it"
      (is (re-find #"7 of 12 puzzles out" en))
      (is (= 7 (count (re-seq #"class=\"cell on\"" en))))
      (is (= 12 (count (re-seq #"class=\"cell(?: on)?\"" en))))
      (is (re-find #"<span aria-hidden=\"true\" class=\"cells\">" en)))))

(deftest today-panel-after-the-event
  (let [{:keys [en]} (home-at "2027-01-15T12:00:00Z")]
    (is (re-find #"All 12 days are out\." en))
    (is (not (re-find #"puzzle-card|class=\"battery\"" en))
        "\"All 12 days are out.\" says it; no card or battery")))
