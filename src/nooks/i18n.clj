(ns nooks.i18n
  "UI strings from data/i18n/*.edn.

  A string value is final copy; a {:draft \"…\"} value is a placeholder and
  always renders inside <span class=\"draft\">. `t` returns hiccup so a draft
  can't lose its marker on the way to the page; `t-str` is for attributes and
  <title>, where markup can't go."
  (:require [clojure.string :as str])
  (:import [java.time.format DateTimeFormatter]
           [java.util Locale]))

(def ^:dynamic *strings*
  "{:en {...} :pt-BR {...}}, bound by the build."
  nil)

(defn lookup
  "The raw value for k in lang, falling back to English."
  [lang k]
  (let [v (or (get-in *strings* [lang k]) (get-in *strings* [:en k]))]
    (when (nil? v)
      (throw (ex-info (str "Missing UI string " k) {:lang lang :key k})))
    v))

(defn draft? [v] (and (map? v) (contains? v :draft)))

(defn- fill [s args] (if (seq args) (apply format s args) s))

(defn t
  "Hiccup for k: a plain string, or [:span.draft …] for placeholders."
  [lang k & args]
  (let [v (lookup lang k)]
    (if (draft? v)
      [:span.draft (fill (:draft v) args)]
      (fill v args))))

(defn t-str
  "Plain text for k; placeholders are prefixed [draft] so they stay visible."
  [lang k & args]
  (let [v (lookup lang k)]
    (if (draft? v)
      (str "[draft] " (fill (:draft v) args))
      (fill v args))))

(defn translated?
  "True when lang has its own value for k (not an English fallback, not a draft)."
  [lang k]
  (let [v (get-in *strings* [lang k])]
    (and (some? v) (not (draft? v)))))

;; ---- Dates (all UTC) -------------------------------------------------------

(def ^:private day-fmt (DateTimeFormatter/ofPattern "EEE d MMM" Locale/UK))

(defn day-label
  "\"Fri 4 Dec\". Only English for now; pt-BR pages render English content."
  [^java.time.LocalDate d]
  (str/replace (.format d day-fmt) "." ""))

(defn pad2 [n] (format "%02d" n))
