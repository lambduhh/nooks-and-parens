(ns nooks.check-test
  "Each bb check rule must catch a violation. A regex that never matches would
  otherwise pass every page silently."
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is testing]]
            [nooks.check]
            [nooks.denylist]))

(defn run [chk html & [ctx]] (seq ((deref chk) (merge {:site {:template/public? false}} ctx) html)))

(deftest primary-button
  (is (run #'nooks.check/check-primary "<p>none</p>"))
  (is (run #'nooks.check/check-primary "<a class=\"pill\">a</a><a class=\"x pill\">b</a>"))
  (is (nil? (run #'nooks.check/check-primary "<a class=\"pill\" href=\"/\">a</a><span class=\"pill-note\">n</span>"))))

(deftest external-loads
  (is (run #'nooks.check/check-external-loads "<script src=\"https://cdn.example/x.js\"></script>"))
  (is (run #'nooks.check/check-external-loads "<link rel=\"stylesheet\" href=\"//fonts.example/css\">"))
  (is (run #'nooks.check/check-external-loads "<img src=\"https://i.example/a.png\" alt=\"\">"))
  (is (run #'nooks.check/check-external-loads "<p style=\"color:red\">x</p>") "inline style")
  (is (run #'nooks.check/check-external-loads "<script>alert(1)</script>") "inline script")
  (is (nil? (run #'nooks.check/check-external-loads
                 "<script src=\"/n/a.js\"></script><a href=\"https://clojurians.net\">x</a><link rel=\"alternate\" href=\"https://x.example/atom.xml\">"))))

(deftest template-gate
  (is (run #'nooks.check/check-template-gate "<a href=\"https://github.com/lambduhh/clojure-template-aoc\">t</a>"))
  (is (nil? (run #'nooks.check/check-template-gate "<a href=\"https://github.com/lambduhh/clojure-template-aoc\">t</a>"
                 {:site {:template/public? true}}))))

(deftest guests
  (is (run #'nooks.check/check-guests "<p>With Ada Lovelace</p>" {:unconfirmed ["Ada Lovelace"]}))
  (is (nil? (run #'nooks.check/check-guests "<p>To be announced</p>" {:unconfirmed ["Ada Lovelace"]}))))

(deftest unannounced-sessions
  (is (run #'nooks.check/check-unannounced "<b>Session 03 · Demo</b>" {:unannounced ["Session 03 · Demo"]}))
  (is (nil? (run #'nooks.check/check-unannounced "<span>Session 03: details to be announced</span>"
                 {:unannounced ["Session 03 · Demo"]}))))

(deftest forbidden-words
  (is (run #'nooks.check/check-forbidden-words "<h2 id=\"x\">A <em>fast</em> language</h2>"))
  (is (run #'nooks.check/check-forbidden-words "<span class=\"chip open\">Immutable</span>"))
  (is (nil? (run #'nooks.check/check-forbidden-words "<h2>Breakfast</h2><p>fast</p>")) "whole words, headings only"))

;; A stand-in name ("acme"), hashed like the real deny-list, so the test
;; exercises the matcher without the repo spelling the real name.
(def acme [{:len 4 :sha256 "822b33ad87c148a0a20a5ba7cd5ebcaa68d36a18e7aad165554903f52ca82757"}])

(deftest no-employer
  (is (run #'nooks.check/check-no-employer "<p>An engineer at ACME</p>" {:names acme}))
  (is (run #'nooks.check/check-no-employer "<a href=\"https://acme.example\">x</a>" {:names acme}) "attributes too")
  (is (run #'nooks.check/check-no-employer "<p>Acmeville</p>" {:names acme}) "inside a longer word")
  (is (nil? (run #'nooks.check/check-no-employer "<p>Software Engineer, Clojure Team</p>" {:names acme})))
  (is (nil? (run #'nooks.check/check-no-employer "<p>Software Engineer, Clojure Team</p>")) "the real list")
  (is (seq nooks.denylist/names) "the real list isn't empty"))

(deftest internal-tool-links
  (is (run #'nooks.check/check-internal-links "<a href=\"https://acme.atlassian.net/wiki/x\">w</a>"))
  (is (run #'nooks.check/check-internal-links "<a href=\"https://docs.google.com/document/d/x\">d</a>"))
  (is (run #'nooks.check/check-internal-links "<a href=\"https://acme.slack.com/archives/C1\">c</a>"))
  (is (nil? (run #'nooks.check/check-internal-links "<a href=\"https://clojurians.slack.com/archives/C1\">c</a>")) "Clojurians is public")
  (is (nil? (run #'nooks.check/check-internal-links "<a href=\"https://clojurians.net\">j</a>"))))

(deftest img-and-svg
  (is (run #'nooks.check/check-img-svg "<img src=\"/a.png\">"))
  (is (run #'nooks.check/check-img-svg "<svg><use href=\"#t\"></use></svg>"))
  (is (nil? (run #'nooks.check/check-img-svg "<img src=\"/a.png\" alt=\"\"><svg aria-hidden=\"true\"></svg>"))))

(deftest internal-links
  (let [out (str (fs/create-temp-dir))]
    (fs/create-dirs (fs/path out "start"))
    (spit (str (fs/path out "start" "index.html")) "x")
    (let [ctx {:base "/n" :out-dir out :file (str (fs/path out "index.html"))}]
      (is (nil? (run #'nooks.check/check-links "<a href=\"/n/start/\">s</a><a href=\"start/#x\">s</a><a href=\"#top\">t</a>" ctx)))
      (is (run #'nooks.check/check-links "<a href=\"/n/nope/\">x</a>" ctx))
      (is (run #'nooks.check/check-links "<a href=\"/start/\">x</a>" ctx) "missing base path")
      (is (run #'nooks.check/check-links "<meta http-equiv=\"refresh\" content=\"0; url=/n/gone/\">" ctx)))
    (fs/delete-tree out)))

(deftest anchors
  (is (run #'nooks.check/check-anchors "<a href=\"#2.-schedule\">2</a><h2 id=\"2.-timetable\">2. Timetable</h2>"))
  (is (nil? (run #'nooks.check/check-anchors "<a href=\"#main\">skip</a><main id=\"main\"></main><svg aria-hidden=\"true\"><use href=\"#t-map\"></use></svg>")) "svg <use> isn't a link")
  (is (nil? (run #'nooks.check/check-anchors "<!-- cite with <a href=\"#ref-aoc\">[AOC]</a> --><p>x</p>")) "a link inside an HTML comment isn't a link"))

(deftest links-in-comments
  (let [out (str (fs/create-temp-dir))
        ctx {:base "/n" :out-dir out :file (str (fs/path out "index.html"))}]
    (is (run #'nooks.check/check-links "<a href=\"/n/nope/\">x</a>" ctx) "the same link outside a comment fails")
    (is (nil? (run #'nooks.check/check-links "<!-- <a href=\"/n/nope/\">x</a> -->" ctx)) "a link inside an HTML comment isn't a link")))
