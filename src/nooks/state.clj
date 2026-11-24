(ns nooks.state
  "Derived page state, exactly as in docs/content-model.md § Derived state.

  Every function is pure: `as-of` (a java.time.Instant) is passed in. Nothing
  in this namespace, or anywhere else in the build, reads the clock except
  `nooks.build/parse-as-of` when no --as-of is given."
  (:require [clojure.string])
  (:import [java.time Instant LocalDate LocalTime ZoneOffset OffsetDateTime]
           [java.time.format DateTimeParseException]
           [java.util Date]))

(defn ->instant
  "#inst values read from EDN are java.util.Date; normalise to Instant."
  ^Instant [x]
  (cond (instance? Instant x) x
        (instance? Date x)    (.toInstant ^Date x)
        :else (throw (ex-info "Not an instant" {:value x}))))

(defn utc-date ^LocalDate [x]
  (LocalDate/ofInstant (->instant x) ZoneOffset/UTC))

(defn- at-or-after? [as-of t]
  (not (.isBefore (->instant as-of) (->instant t))))

(defn parse-as-of
  "Parse a --as-of value. A bare date (2026-12-07) means 12:00 UTC that day,
  the middle of the UTC day, so it lands after the 05:00 UTC unlock and gate
  changes. A date-time must carry a zone: 2026-12-01T04:59Z."
  ^Instant [s]
  (try
    (if (re-matches #"\d{4}-\d{2}-\d{2}" s)
      (-> (LocalDate/parse s) (.atTime LocalTime/NOON) (.toInstant ZoneOffset/UTC))
      (.toInstant (OffsetDateTime/parse
                   ;; allow 2026-12-01T04:59Z (no seconds)
                   (if (re-matches #".*T\d{2}:\d{2}(Z|[+-].*)" s)
                     (clojure.string/replace s #"(T\d{2}:\d{2})" "$1:00")
                     s))))
    (catch DateTimeParseException e
      (throw (ex-info (str "Can't read --as-of " (pr-str s)
                           ". Use 2026-12-07 or 2026-12-01T05:00Z.")
                      {:as-of s} e)))))

;; ---- Per day ---------------------------------------------------------------

(defn unlocked? [as-of day] (at-or-after? as-of (:day/unlock day)))

(defn open?
  "Solutions, replay, tutorial and ring links may show."
  [as-of day]
  (at-or-after? as-of (:day/spoiler-until day)))

(defn status [as-of day]
  (cond (not (unlocked? as-of day)) :soon
        (not (open? as-of day))     :locked
        :else                       :open))

(defn session-state
  "Compared by UTC calendar day: :upcoming before the session date, :today on
  it, :done after."
  [as-of day]
  (let [today (utc-date as-of)
        sdate (utc-date (get-in day [:day/session :date]))]
    (cond (.isBefore today sdate) :upcoming
          (.isEqual today sdate)  :today
          :else                   :done)))

(defn video-for [videos day]
  (first (filter #(= (:day/n day) (:video/day %)) videos)))

(defn replay? [as-of day videos]
  (boolean (and (= :done (session-state as-of day))
                (video-for videos day))))

(defn primary-cta
  "Which primary button a day page gets: before the gate, the live session on
  Discord; after it, the day's thread in #adventofcode."
  [as-of day]
  (if (open? as-of day) :slack-day :discord-session))

(defn day-state [as-of day videos]
  {:day         day
   :unlocked?   (unlocked? as-of day)
   :open?       (open? as-of day)
   :status      (status as-of day)
   :session     (session-state as-of day)
   :replay?     (replay? as-of day videos)
   :primary-cta (primary-cta as-of day)})

;; ---- Across days -----------------------------------------------------------

(defn today-day
  "The highest unlocked day, or nil before the first unlock."
  [as-of days]
  (last (filter #(unlocked? as-of %) (sort-by :day/n days))))

(defn last-session-date [days]
  (last (sort (map #(utc-date (get-in % [:day/session :date])) days))))

(defn archive?
  "After the last session's UTC date."
  [as-of days]
  (.isAfter (utc-date as-of) (last-session-date days)))

(defn session-today [as-of days]
  (first (filter #(= :today (session-state as-of %)) days)))

(defn next-sessions
  "Sessions from today (UTC) on, in date order, at most n."
  [as-of days n]
  (->> days
       (remove #(= :done (session-state as-of %)))
       (sort-by #(utc-date (get-in % [:day/session :date])))
       (take n)))

(defn open-days [as-of days]
  (->> days (filter #(open? as-of %)) (map :day/n) sort vec))
