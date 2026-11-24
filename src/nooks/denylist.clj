(ns nooks.denylist
  "Words and hosts that must never appear on the site or in the repo.

  Names are stored as SHA-256 hashes of their lower-case spelling, so the repo
  that guards against a name doesn't itself contain it. Matching hashes every
  substring of a letter run at each listed length, so the name is caught inside
  longer words and URLs too (the same reach as a case-insensitive substring
  regex). Add an entry with:

    printf 'word' | shasum -a 256

  Hosts are workplace tools (wikis, chat workspaces, shared docs) that a public
  site has no reason to link. Their names aren't sensitive, so they're plain."
  (:require [clojure.string :as str])
  (:import [java.security MessageDigest]))

(def names
  "Jordan, 2026-11-24: the employer is never named. {:len n :sha256 hex}."
  [{:len 6 :sha256 "44c2fc7b06e5b214c7e9febbf3c0a2487b83a8ed062c3a8df7f7617748a399db"}])

(def internal-host-suffixes
  ["atlassian.net" "docs.google.com" "drive.google.com" "glean.com" "slack.com"])

(def allowed-hosts
  "Public hosts under a suffix above."
  #{"clojurians.slack.com" "api.slack.com"})

(defn- sha256 [^String s]
  (let [bs (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) bs))))

(defn names-found
  "Count of deny-listed names in `text` (0 if none). `entries` defaults to `names`."
  ([text] (names-found text names))
  ([text entries]
   (let [by-len (group-by :len entries)
         runs   (re-seq #"\p{L}+" (str/lower-case text))]
     (count
      (for [run runs
            [n es] by-len
            i (range 0 (inc (- (count run) n)))
            :let [h (sha256 (subs run i (+ i n)))]
            e es :when (= h (:sha256 e))]
        e)))))

(defn internal-hosts
  "Internal-tool hosts linked from `text`, e.g. \"acme.atlassian.net\"."
  [text]
  (distinct
   (for [[_ host] (re-seq #"(?i)(?:https?:)?//([a-z0-9.-]+)" text)
         :let [host (str/lower-case host)]
         :when (not (allowed-hosts host))
         :when (some #(or (= host %) (str/ends-with? host (str "." %))) internal-host-suffixes)]
     host)))
