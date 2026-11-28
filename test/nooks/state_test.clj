(ns nooks.state-test
  "The date edges in docs/plan.md task 3. Every as-of is explicit."
  (:require [clojure.test :refer [deftest is testing]]
            [nooks.data :as data]
            [nooks.state :as s]))

(def days (:days (data/load-all)))
(defn day [n] (nth days (dec n)))
(defn at [x] (s/parse-as-of x))

(deftest parse-as-of
  (is (= (java.time.Instant/parse "2026-12-07T12:00:00Z") (at "2026-12-07")) "bare date = 12:00 UTC")
  (is (= (java.time.Instant/parse "2026-12-01T04:59:00Z") (at "2026-12-01T04:59Z")))
  (is (= (java.time.Instant/parse "2026-12-01T05:00:00Z") (at "2026-12-01T00:00-05:00")) "EST offset")
  (is (thrown? clojure.lang.ExceptionInfo (at "next tuesday"))))

(deftest before-december
  (let [t (at "2026-11-30")]
    (is (nil? (s/today-day t days)))
    (is (every? #(= :soon (s/status t %)) days))
    (is (= :upcoming (s/session-state t (day 1))))
    (is (not (s/archive? t days)))
    (is (= [] (s/open-days t days)))))

(deftest unlock-morning
  (testing "puzzle 1 unlocks at 05:00 UTC, not a minute before"
    (is (= :soon   (s/status (at "2026-12-01T04:59Z") (day 1))))
    (is (nil? (s/today-day (at "2026-12-01T04:59Z") days)))
    (is (= :locked (s/status (at "2026-12-01T05:00Z") (day 1))))
    (is (= 1 (:day/n (s/today-day (at "2026-12-01T05:00Z") days))))))

(deftest gate-edge
  (testing "solutions open at unlock + 3 days, 05:00 UTC"
    (is (= :locked (s/status (at "2026-12-04T04:59Z") (day 1))))
    (is (not (s/open? (at "2026-12-04T04:59Z") (day 1))))
    (is (= :discord-session (s/primary-cta (at "2026-12-04T04:59Z") (day 1))))
    (is (= :open (s/status (at "2026-12-04T05:00Z") (day 1))))
    (is (= :slack-day (s/primary-cta (at "2026-12-04T05:00Z") (day 1))))
    (is (= [1] (s/open-days (at "2026-12-04T05:00Z") days)))))

(deftest spoiler-dates-are-unlock-plus-three-days
  (doseq [d days]
    (is (= (.plus (s/->instant (:day/unlock d)) (java.time.Duration/ofDays 3))
           (s/->instant (:day/spoiler-until d)))
        (str "day " (:day/n d)))))

(deftest no-session-before-its-gate
  ;; A session discusses its puzzle, so it can't be on a UTC day before that
  ;; day's spoiler gate (Jordan, 2026-11-27: session 1 once fell a day early).
  (doseq [d days]
    (is (not (.isBefore (s/utc-date (get-in d [:day/session :date]))
                        (s/utc-date (:day/spoiler-until d))))
        (str "session " (get-in d [:day/session :n]) " is before day " (:day/n d) "'s gate"))))

(deftest session-day
  (let [t (at "2026-12-04")]
    (is (= :today (s/session-state t (day 1))))
    (is (= 1 (:day/n (s/session-today t days))))
    (is (= :today (s/session-state (at "2026-12-04T00:00Z") (day 1))) "whole UTC day")
    (is (= :done (s/session-state (at "2026-12-05T00:00Z") (day 1))))))

(deftest replay-needs-a-video
  (let [t (at "2026-12-05")
        video {:video/day 1 :video/youtube-id "x"}]
    (is (not (s/replay? t (day 1) [])) "no video, no replay")
    (is (s/replay? t (day 1) [video]))
    (is (not (s/replay? (at "2026-12-04") (day 1) [video])) "not on the session day itself")))

(deftest no-session-on-11-dec
  (is (nil? (s/session-today (at "2026-12-11") days))))

(deftest after-the-last-session
  (let [t (at "2026-12-23")]
    (is (s/archive? t days))
    (is (= 12 (:day/n (s/today-day t days))))
    (is (empty? (s/next-sessions t days 3)))
    (is (not (s/archive? (at "2026-12-22") days)) "the last session day isn't archive yet")))

(deftest mid-january
  (let [t (at "2027-01-15")]
    (is (s/archive? t days))
    (is (every? #(= :open (s/status t %)) days))
    (is (= (vec (range 1 13)) (s/open-days t days)))))

(deftest next-sessions
  (is (= [1 2 3] (map :day/n (s/next-sessions (at "2026-11-30") days 3))))
  (is (= [2 3 4] (map :day/n (s/next-sessions (at "2026-12-07") days 3))) "today's session is included")
  (is (= [12] (map :day/n (s/next-sessions (at "2026-12-22") days 3)))))
