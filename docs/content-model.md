# Content model

Every page is built from the EDN files in `data/`. A daily update is a data edit, never a template edit. All times are UTC.

## `data/site.edn`

```clojure
{:site/name          "Nooks & Parens"
 :site/tagline       "It's dangerous to go alone! Take this."
 :site/positioning   "Clojure. A power tool for evolving systems."
 :site/framing       "Each Advent of Code day is a small evolving system: part 2 usually changes the question part 1 asked."
 :site/url           "https://nooks-and-parens.pages.dev"   ; confirm after the Pages project exists
 :site/base-path     ""                                       ; "/nooks-and-parens" only on the GitHub Pages fallback
 :site/languages     [:en :pt-BR]
 :cta/slack          {:label "Join #adventofcode on Clojurians Slack" :url "https://clojurians.net"}
 :cta/slack-channel  nil        ; Jordan pastes the verified channel link here
 :cta/discord        {:url "https://discord.clojure.camp"}
 :template/public?   false      ; flip to true only when the template repo is public with "Use this template" on
 :template/url       "https://github.com/lambduhh/clojure-template-aoc"
 :aoc/year           2026
 :aoc/days           12}        ; assumed; Advent of Code hasn't published a count for 2026
```

## `data/days.edn`

A vector of 12 maps. See the file for all of them.

```clojure
{:day/n             1
 :day/unlock        #inst "2026-12-01T05:00:00Z"   ; 00:00 EST
 :day/spoiler-until #inst "2026-12-04T05:00:00Z"   ; unlock + 3 days
 :day/puzzle-url    "https://adventofcode.com/2026/day/1"
 :day/tool          :hammer                        ; icon on the tool wall
 :day/session       {:n 1 :date #inst "2026-12-04" :format :live-coding
                     :time nil                      ; nil renders "[time TBA]"
                     :guests [{:name "…" :url "…" :confirmed? false}]}
 :day/recipes       [:parse :freq]                 ; keys into recipes.edn
 :day/solutions     [{:author "…" :url "…" :ring? true}]
 :day/tutorial      nil                            ; path once written (days 1–3)
 :day/updated       #inst "2026-12-04"}
```

## `data/videos.edn`

A vector, empty until the first session.

```clojure
{:video/youtube-id "…"
 :video/title      "…"
 :video/day        1
 :video/date       #inst "2026-12-04"
 :video/format     :live-coding
 :video/kind       :explanation           ; :tutorial :how-to :reference :explanation
 :video/length     "47:12"
 :video/guests     [{:name "…" :url "…" :confirmed? true}]
 :video/chapters   [{:t "04:12" :label "Turning the input into maps" :note "…"}]
 :video/links      [{:label "…" :url "…"}]
 :video/summary    nil                     ; nil renders a draft placeholder
 :video/captions   [:en]                   ; add :pt-BR when ready
 :video/next-step  {:label "…" :url "…"}}
```

Format → kind defaults: workshop → tutorial; demo → how-to; everything else → explanation.

## `data/recipes.edn`

DIY recipes and Workbench starters. Made-up data only, never puzzle text or examples. Each has the code and the expected printed result (used by tests and by the offline fallback).

## `data/ring.edn`

```clojure
{:ring/members [{:handle "…" :site "…" :days #{1 3} :opted-in #inst "2026-12-05"}]}
```

Only members who have opted in. Ring links for a day render only after that day's spoiler date.

## Derived state

`src/nooks/state.clj` computes everything from `as-of` (an instant) and a day map. No other code reads the clock.

| Value | Rule |
|---|---|
| `unlocked?` | `as-of >= :day/unlock` |
| `open?` (solutions) | `as-of >= :day/spoiler-until` |
| `status` | `:soon` if not unlocked; `:locked` if unlocked and not open; `:open` otherwise |
| `session-state` | `:upcoming` before the session date (UTC day); `:today` on it; `:done` after |
| `replay?` | `:done` **and** a video with this day exists in `videos.edn` |
| `today-day` | the highest day with `unlocked?`, or nil before Dec 1 |
| `archive?` | `as-of` after the last session date |
| `primary-cta` (day page) | not `open?` → "Join session N on Discord"; `open?` → "Discuss Day NN in #adventofcode" |

What a day page shows:

| Status | Shows | Never shows |
|---|---|---|
| `:soon` | Facts note, "Not open yet" sign with unlock time, DIY recipes, session info | Puzzle link, solutions, replay, tutorial, ring |
| `:locked` | Facts, puzzle link, "Closed" sign with gate date, DIY recipes, live session info | Solutions, replay, tutorial, ring |
| `:open` | Facts, puzzle link, "Open" sign, DIY recipes, replay (if `replay?`), tutorial (days 1–3), credited solutions, ring strip, roundup link | — |

## Strings and languages

UI strings (labels, chips, gate text, button text) live in `data/i18n/en.edn` and `data/i18n/pt-BR.edn`, keyed by keyword. The decided labels "Nooks & Parens" and "Workbench" stay untranslated. The pt-BR reviewer decides how to render "What're ya REPLin'?", "DIY recipes" and the tagline in Portuguese; until then the pt-BR file holds the English string marked as untranslated.
