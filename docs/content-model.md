---
type: Data Model
title: "Content model"
description: "The EDN files every Power Tools & Parens page is built from, and how page state is derived from them."
tags: [nooks-and-parens, data]
created: 2026-11-24
modified: 2026-11-28
ai_assisted: "Claude via Claude Desktop (Claude Design); updated by Claude Code"
session: unrecorded
tools: unrecorded
agents_skills: []
review_maturity: L1 (Human-directed)
review_note: "Drafted in Claude Design at Jordan Miller's direction, 2026-11-24. The session and tools weren't recorded. Not yet reviewed."
---

# Content model

Every page is built from the EDN files in `data/`. A daily update is a data edit, never a template edit. All times are UTC.

## `data/site.edn`

```clojure
{:site/name          "Power Tools & Parens"
 :site/tagline       "It's dangerous to go alone! Take this."
 :site/tagline-emoji "📝🧚"     ; decorative, after the tagline, aria-hidden; nil for none
 :site/positioning   "Clojure. A power tool for evolving systems."
 :site/url           "https://lambduhh.github.io/powertools-and-parens"   ; GitHub Pages project site, no trailing slash
 :site/base-path     "/powertools-and-parens"                               ; prefixes every generated link
 :site/author        "Jordan Miller"                                   ; the blog feed author
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
                     :guests [{:name "…" :url "…" :confirmed? false}]
                     :announced? false}             ; false: the row shows pixel bars and the date only
 :day/recipes       [:haystack :nest]              ; keys into recipes.edn
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

The Workbench's exercises, and each day's DIY recipes (`:day/recipes` holds exercise ids). A **vector**, in learning order, grouped by `:group`. Each exercise has `:id`, `:kind`, `:group`, `:title`, `:task`, `:code`, `:why` and `:links`, plus:

| `:kind` | Exercise type ([Teaching Tech Together, ch. 12](https://teachtogether.tech/en/#s:exercises)) | Also has |
|---|---|---|
| `:predict` | Guess the result, then run it (teasers, tracing). Running shows `:why` | `:expect` |
| `:fill` | Replace each `___` so the result matches the goal | `:goal`, `:solution`, `:hint` |
| `:reverse` | Work backward: an input that gives the goal | `:goal`, `:solution`, `:hint` |
| `:fix` | One small change that makes the result match the goal | `:expect`, `:goal`, `:solution`, `:hint` |
| `:vary` | One small change that changes the result to the goal | `:expect`, `:goal`, `:solution`, `:hint` |

`:expect` is the starter's own result as `pr-str` (offline preview, tests). `:goal` is compared with the learner's result by value (`=`), so set and map order don't matter. `:solution` is `[search replace]`, one exact replacement on `:code`, used only by tests and never rendered. Teasers carry `:source` (*Clojure Brain Teasers* by Alex Miller and L. Jordan Miller, Pragmatic Bookshelf, 2025). Never puzzle text or examples. Every exercise must give the same result in ClojureScript (the Workbench) and on the JVM (the template).

## `data/ring.edn`

```clojure
{:ring/members [{:handle "…" :site "…" :days #{1 3} :opted-in #inst "2026-12-05"}]}
```

Only members who have opted in. Ring links for a day render only after that day's spoiler date.

## `data/formats.edn`

The session formats guide and the pool of session formats, for `/formats/`. Each day's session is picked from the pool, by puzzle and guest availability; there is no day-by-day format plan.

- `:formats/pool`: one map per format.
  - `:format/id` is a keyword with a `:format/<id>` string in `data/i18n/`.
  - `:format/source` is `:favorite` (the guide's 12 favorites) or `:extra` (this campaign's additions).
  - `:format/good-for`: the favorites table's "Good for" line. Required on `:favorite` formats.
  - Optional: `:format/length`, `:format/people`, `:format/prepare`, `:format/example-video`. Until they're set, the page shows a draft "to be written".
- `:formats/guide`: Jordan's guide to 40-minute learning sessions, the full menu of formats.
  - Page copy: `:guide/title`, `:guide/lede`, `:guide/audiences` (`:audience/who`, `:audience/text`), `:guide/variety`, `:guide/favorites-title`, `:guide/favorites-lede`, `:guide/menu-lede`, `:guide/heuristics-title`, `:guide/heuristics` (`:heuristic/q`, `:heuristic/a`).
  - `:guide/groups`: the menu grouped by shape. Each group has `:group/id`, `:group/title`, `:group/blurb` and `:group/formats`.
  - Each guide entry has `:format/id` (a guide id, distinct from pool ids), `:format/name` and `:format/text`. An entry that is in the pool has `:format/pool` (the pool id; pairing and mob both map to `:pairing-mob`) and `:format/favorite? true`.
  - Favorites show as a "Favorite" chip in words, never a star glyph.
  - The favorites table is built from the pool's `:favorite` formats, so its names match the schedule's labels.
- `:formats/sources`: where the managers note's two terms (cognitive debt, comprehension debt) come from: `:source/term`, `:source/title`, `:source/author`, `:source/date`, `:source/url`, and an optional `:source/caveat-url`.
- Tests check that every day's draft `:format` is in the pool, that favorites are exactly the guide entries mapped to a `:favorite` pool format, and that `data/formats.edn` has no star glyphs, no employer name and no group-specific name: the guide uses generic words like "session".

## `data/resources.edn`

Outside resources shown in the Learn drawers, next to the template's guides: `:resource/title` (the resource's own name), `:resource/url` (https), and `:resource/kind`, which is the drawer: `:tutorial`, `:how-to`, `:reference` or `:explanation`. Don't repeat links the template's guides already carry.

## `data/learn.edn`

How the Learn page's How-to drawer is grouped and ordered, beginner first. `:learn/how-to` is a vector of `{:group k :items [key …]}`: `:group` is the UI-string key for the group's heading, and each item key is a hub path for a template page (`"/start/"`, `"/learn/howto/<name>/"`) or a `:resource/url` from `resources.edn`. Order in the file is order on the page. Every how-to must be in exactly one group and every key must exist, or the build fails. Other drawers stay alphabetical.

## `data/template.edn`

Guides from the template repo (`:template/url` in `site.edn`), rendered into the hub at build time.

```clojure
{:template/sha   "5e0f08f…"   ; full 40-character commit; move it with bb template:bump
 :template/pages [{:path "docs/setup-flowchart.md" :hub "/start/" :kind :how-to}
                  {:path "docs/tutorial.md"        :hub "/learn/tutorial/" :kind :tutorial}]}
```

`:kind` is the Learn drawer (`:tutorial` `:how-to` `:reference` `:explanation`), or `:ai` for the optional "Using an AI agent?" shelf. Every `src/howto/*.clj` also becomes a how-to page at `/learn/howto/<name>/`. Links between listed files go to their hub pages; links to anything else open on GitHub at the pinned commit. A leading `<!-- DRAFT … -->` comment shows a draft notice on the page.

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
| `:locked` | Facts, puzzle link, "Closed" sign with gate date, DIY recipes, session info (once announced) | Solutions, replay, tutorial, ring |
| `:open` | Facts, puzzle link, "Open" sign, DIY recipes, replay (if `replay?`), tutorial (days 1–3), credited solutions, ring strip | — |

## Strings and languages

UI strings (labels, chips, gate text, button text) live in `data/i18n/en.edn` and `data/i18n/pt-BR.edn`, keyed by keyword. The decided labels "Power Tools & Parens" and "Workbench" stay untranslated. The pt-BR reviewer decides how to render "Shop hours", "DIY recipes" and the tagline in Portuguese; until then the pt-BR file holds the English string marked as untranslated.
