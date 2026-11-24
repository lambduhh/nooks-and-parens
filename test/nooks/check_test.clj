(ns nooks.check-test
  "Each bb check rule must catch a violation. A regex that never matches would
  otherwise pass every page silently."
  (:require [babashka.fs :as fs]
            [clojure.test :refer [deftest is testing]]
            [nooks.check]))

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

(deftest forbidden-words
  (is (run #'nooks.check/check-forbidden-words "<h2 id=\"x\">A <em>fast</em> language</h2>"))
  (is (run #'nooks.check/check-forbidden-words "<span class=\"chip open\">Immutable</span>"))
  (is (nil? (run #'nooks.check/check-forbidden-words "<h2>Breakfast</h2><p>fast</p>")) "whole words, headings only"))

(deftest no-employer
  (is (run #'nooks.check/check-no-employer "<p>An engineer at acme</p>"))
  (is (run #'nooks.check/check-no-employer "<a href=\"https://acme.example\">x</a>") "attributes too")
  (is (nil? (run #'nooks.check/check-no-employer "<p>Software Engineer, Clojure Team</p>"))))

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
  (is (nil? (run #'nooks.check/check-anchors "<a href=\"#main\">skip</a><main id=\"main\"></main><svg aria-hidden=\"true\"><use href=\"#t-map\"></use></svg>")) "svg <use> isn't a link"))
