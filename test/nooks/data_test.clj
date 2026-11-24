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
    (is (= "Power Tools & Parens" (:site/name s)))
    (is (= "It's dangerous to go alone! Take this." (:site/tagline s)) "the tagline's words stay exact; decoration is :site/tagline-emoji")
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

(deftest formats
  (let [{:formats/keys [pool sources]} (:formats d)
        ids (set (map :format/id pool))]
    (is (= (count pool) (count ids)) "no duplicate formats")
    (is (every? #{:favorite :extra} (map :format/source pool)))
    (is (= 12 (count (filter #(= :favorite (:format/source %)) pool))))
    (testing "every day's draft format is in the pool"
      (is (every? ids (map #(get-in % [:day/session :format]) (:days d)))))
    (doseq [s sources]
      (is (str/starts-with? (:source/url s) "https://"))
      (is (string? (:source/title s))))))

(deftest formats-guide
  (let [{:formats/keys [pool guide]} (:formats d)
        pool-ids  (set (map :format/id pool))
        fav-ids   (set (map :format/id (filter #(= :favorite (:format/source %)) pool)))
        entries   (mapcat :group/formats (:guide/groups guide))
        entry-ids (map :format/id entries)]
    (is (every? string? [(:guide/title guide) (:guide/lede guide) (:guide/variety guide)
                         (:guide/favorites-title guide) (:guide/favorites-lede guide)
                         (:guide/menu-lede guide) (:guide/heuristics-title guide)]))
    (is (= (count entry-ids) (count (set entry-ids))) "no duplicate guide entries")
    (doseq [{:group/keys [id title blurb formats]} (:guide/groups guide)]
      (testing id
        (is (keyword? id))
        (is (string? title))
        (is (string? blurb))
        (is (seq formats))))
    (doseq [{:format/keys [id name text pool favorite?] :as e} entries]
      (testing id
        (is (string? name))
        (is (string? text))
        (is (boolean? (boolean favorite?)))
        (when pool (is (pool-ids pool) "a guide entry's :format/pool is a pool id"))
        (is (= (boolean favorite?) (contains? fav-ids pool))
            "favorites are exactly the entries that map to a favorite pool format")))
    (testing "every favorite pool format has a guide entry and a good-for line"
      (is (= fav-ids (set (keep :format/pool entries))))
      (is (every? (comp string? :format/good-for) (filter #(fav-ids (:format/id %)) pool))))
    (testing "copy rules: no star glyphs (no AoC motifs), no employer name, generic words only"
      (let [all-text (slurp "data/formats.edn")]
        (is (not (re-find #"[⭐★☆✨]" all-text)))
        (is (not (re-find #"(?i)acme" all-text)))
        (is (not (re-find #"(?i)guild" all-text)) "use generic words: session, group")))))

(deftest resources
  (doseq [r (:resources d)]
    (testing (:resource/title r)
      (is (string? (:resource/title r)))
      (is (str/starts-with? (:resource/url r) "https://"))
      (is (#{:tutorial :how-to :reference :explanation} (:resource/kind r))))))

(deftest videos-and-ring
  (is (vector? (:videos d)))
  (is (vector? (get-in d [:ring :ring/members]))))
