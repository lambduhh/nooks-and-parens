(ns nooks.data-test
  "Every data/*.edn file loads and has the shape docs/content-model.md gives."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [nooks.data :as data]))

(def d (data/load-all))

(defn inst? [x] (instance? java.util.Date x))

(deftest site
  (let [s (:site d)]
    (doseq [k [:site/name :site/tagline :site/positioning :site/framing :site/url
               :site/base-path :cta/slack :cta/discord :template/url]]
      (is (contains? s k) (str k)))
    (is (= "Nooks & Parens" (:site/name s)))
    (is (= "It's dangerous to go alone! Take this." (:site/tagline s)))
    (is (= "Clojure. A power tool for evolving systems." (:site/positioning s)))
    (is (boolean? (:template/public? s)))
    (testing "base path is either empty or /segment, and :site/url ends with it"
      (is (re-matches #"(/[a-z0-9-]+)?" (:site/base-path s)))
      (is (str/ends-with? (:site/url s) (:site/base-path s)))
      (is (not (str/ends-with? (:site/url s) "/"))))
    (is (str/starts-with? (:site/url s) "https://"))))

(deftest days
  (let [ds (:days d)]
    (is (= (:aoc/days (:site d)) (count ds)))
    (is (= (range 1 13) (map :day/n ds)))
    (doseq [{:day/keys [n unlock spoiler-until puzzle-url tool session recipes solutions] :as day} ds]
      (testing (str "day " n)
        (is (inst? unlock))
        (is (inst? spoiler-until))
        (is (= (str "https://adventofcode.com/" (:aoc/year (:site d)) "/day/" n) puzzle-url))
        (is (#{:hammer :screwdriver :wrench :saw :level :tape} tool))
        (is (= n (:n session)))
        (is (inst? (:date session)))
        (is (keyword? (:format session)))
        (is (vector? (:guests session)))
        (is (every? #(contains? % :confirmed?) (:guests session)) "every guest says whether they're confirmed")
        (is (every? (:recipes d) recipes) "recipes exist in recipes.edn")
        (is (vector? solutions))
        (is (contains? day :day/tutorial))))))

(deftest recipes
  (doseq [[k r] (:recipes d)]
    (testing (str k)
      (is (string? (:title r)))
      (is (string? (:code r)))
      (is (string? (:expect r))))))

(deftest videos-and-ring
  (is (vector? (:videos d)))
  (is (vector? (get-in d [:ring :ring/members]))))
