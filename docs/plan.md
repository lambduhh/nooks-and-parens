---
type: Guide
title: "Build plan"
description: "Ordered build tasks for Power Tools & Parens, each with an owner and acceptance checks."
tags: [nooks-and-parens, plan]
created: 2026-11-24
modified: 2026-11-27
ai_assisted: "Claude via Claude Desktop (Claude Design); updated by Claude Code"
session: unrecorded
tools: unrecorded
agents_skills: []
review_maturity: L1 (Human-directed)
review_note: "Drafted in Claude Design at Jordan Miller's direction, 2026-11-24. The session and tools weren't recorded. Not yet reviewed."
---

# Build plan

Ordered tasks for building Power Tools & Parens. Each task has an owner (**Claude Code** or **Jordan**) and acceptance checks. Do them in order unless a task says otherwise. Read `CLAUDE.md` first.

## Dates that drive the plan

| Date | What happens |
|---|---|
| **Tues 24 Nov** | **P0 launch.** Landing, Start here, calendar, About, formats, CTAs live |
| Tue 1 Dec | Puzzle 1 unlocks, 05:00 UTC |
| **Fri 4 Dec** | **P1.** Session 1. Its replay is added after the session, behind Day 1's gate |
| Sat 5 Dec | Day 1's spoiler gate opens, 05:00 UTC |
| 7–22 Dec | **P2.** Sessions 2–12, replays daily, tutorials for days 1–3, Workbench, webring |
| Fri 11 Dec | No session |
| mid-Jan 2027 | **P3.** Wrap-up post, archive state |

Session schedule and formats are in `data/days.edn`. Formats are drafts; guests are unconfirmed.

## P0: launch (by Mon 30 Nov)

### 0. Hosting — Jordan (done 2026-11-24)
GitHub Pages project site (`docs/hosting.md`): the repo must be public and its Pages source is GitHub Actions. The repo is private for now, pending a security review, so the site isn't deployed yet.
**Done when:** `:site/url` and `:site/base-path` in `data/site.edn` match the Pages URL.

### 1. Scaffold — Claude Code
- `bb.edn` with the tasks in `CLAUDE.md`; `deps` for quickblog (pin a version; read its README for the current coordinate).
- `.gitignore`: `public/`, `notes/`, `.work/`, `.cpcache/`, `node_modules/`.
- `src/nooks/build.clj` entry point taking `--as-of`.
- Copy `assets/` structure from the layout in `CLAUDE.md`.
**Done when:** `bb build` writes `public/index.html` with the layout shell (sign header, toolbelt, footer) and `bb dev` serves it.

### 2. Data — Claude Code
Create `data/site.edn`, check `data/days.edn`, `data/recipes.edn`, `data/videos.edn`, `data/ring.edn` against `docs/content-model.md`. Add `data/i18n/en.edn` and `data/i18n/pt-BR.edn` for UI strings (labels, chips, gate text). Put every user-facing string that isn't from Markdown in i18n files.
**Done when:** a `bb test` spec loads all files and validates their shape.

### 3. State logic — Claude Code
`src/nooks/state.clj`: pure functions from `(as-of, day)` to state, exactly as in `docs/content-model.md` § Derived state. No `now` calls inside; `as-of` is passed in.
**Done when:** `bb test` covers: before Dec 1; unlock morning (04:59 vs 05:00 UTC); gate edge (unlock + 4 days, 04:59 vs 05:00); session day ("Today"); day after session ("Replay ready" only if a video id exists); after Dec 22; mid-January.

### 4. Styles — Claude Code
Port the prototype's CSS into `assets/css/site.css` using the tokens in `docs/design-system.md`. Self-host Nunito and IBM Plex Mono (check their licences; both are OFL) or fall back to system fonts.
**Done when:** the shell matches the prototype at 1280 px, 768 px and 320 px.

### 5. Pages — Claude Code
Build in this order; each one passes the definition of done in `CLAUDE.md`.
1. **Landing** `/`: dialog with positioning line, main menu `[0] Today [1] Calendar [2] Workbench [5] Start here` (in toolbelt order; each number is that tool's shortcut key), primary button, Today panel with the REPL-as-data readout, "Shop hours" next three sessions.
2. **Calendar** `/days/`: "Shop hours" heading, tool wall (12 slots), legend in words, session schedule list.
3. **Day pages** `/days/01/` … `/days/12/`: facts note, gate sign, primary button by state, DIY recipes (always visible), then by state: session info (once announced), or replay + community solutions.
4. **Start here** `/start/`: the onboarding path in § Start here requirements below. Since 2026-11-24 it renders the template's `docs/setup-flowchart.md`; the OS tabs, click-to-load videos and copy buttons are still to come.
5. **About** `/about/`: who runs it, Jordan's role ("Software Engineer, Clojure Team"; no employer name), credits (quickblog, Scittle, borkdude), "not affiliated" line. Copy is draft.
6. **Formats** `/formats/`: Jordan's session formats guide from `data/formats.edn` (favorites table, the full menu grouped by shape, heuristics), then this campaign's additions. Pool formats show length, people, what to prepare and an example recording when one exists. Linked from the calendar and the announcement post. It also serves managers as a guide to leading technical conversations that keep humans in the loop. The guide copy is Jordan's; the page's own labels (intro, additions heading, managers note) are draft.
7. **Today** `/today/`: redirect page (meta refresh + link) to the current day page as of build time, or to `/` before Dec 1.

### 6. Blog — Claude Code
quickblog renders into `public/blog/`. Follow its README section on serving an alternate content root. Templates in `templates/` reuse the site header, toolbelt and footer. The announcement post and roundups live here.
**Done when:** a sample post renders at `/blog/` with the site chrome, and the index links back to `/`.

### 7. Checks — Claude Code
`bb check` builds the site for each date in the check set and fails on any violation. See § Checks.

### 8. pt-BR scaffolding — Claude Code
Mirror every route under `/pt-br/`. Untranslated pages show the English page with a clear notice ("Tradução em breve", draft) at the top. The language switch links the matching page, not the home page. `lang` attributes are correct.
**Done when:** every English route has a `/pt-br/` twin and `bb check` verifies it.

### 9. Deploy — Claude Code, then Jordan
GitHub Actions workflow per `docs/hosting.md`: on push to `main`, on a daily schedule just after 05:00 UTC, and on manual dispatch. PRs run the tests, checks and build but don't deploy (GitHub Pages has no PR previews).
**Done when:** a push to `main` updates the live site, and a manual run with no changes redeploys.

### 10. Launch check — Jordan
- [ ] Show the site to two people who know Advent of Code. If either mistakes it for an AoC page, change the look.
- [ ] Two or three people new to Clojure follow Start here from zero (one per OS if possible). Note where they stop.
- [ ] Every placeholder that must be real for launch is written.
- [ ] The `#adventofcode` channel link is verified.

## P1: first session (by Fri 4 Dec)

11. **Video pages** `/videos/NN/` from `videos.edn`: click-to-load embed (youtube-nocookie), chapters as real `?t=` links, summary, credits (confirmed guests only), format, kind (tutorial / how-to / reference / explanation), links mentioned, transcript, one next step, discuss button. A `/videos/` list, one line per video plus a metadata line.
12. **Day page replay section** uses the same chapter component.
13. **Verify the day 1 gate** with `bb build --as-of 2026-12-05` and on the live site after the 05:05 UTC rebuild.

## P2: December (7–22 Dec)

14. **Workbench** `/workbench/`: requirements in § Workbench below. Built 2026-11-24 (copy is draft); `bb test:browser` runs every starter recipe in headless Chrome.
15. **Tutorials** for days 1–3 at `/learn/tutorials/day-NN/`, gated like solutions. Made-up data only.
16. **Learn** `/learn/`: four drawers (tutorials, how-to guides, reference, why it works) with counts.
17. **Community solutions and webring** from `ring.edn`: links with credit, appear only after each day's gate, prev / random / next pages generated per member so no server is needed. Members opt in.
18. **Power-tool allusions**: pick from the backlog in `docs/design-system.md` with Jordan.
19. ~~**Roundups**~~ Cut (Jordan, 2026-11-24). The number is kept so references to later tasks stay valid.

## P3: wrap-up (mid-Jan)

20. **Archive state**: after Dec 22, the landing shows "All 12 days are out" and the replays; nothing says "today".
21. Wrap-up post with the numbers.

## Start here requirements

In this order on the page:
1. "New to programming?" first. Yes → Clojure Camp handbook (`https://handbook.clojure.camp`) and its mob sessions.
2. No-install routes: "Just want to try it?" → the Workbench; for the full setup without installing, GitHub Codespaces on the template (only once the template is public; until then "coming soon").
3. Install, per OS, in tabs (macOS, Linux, Windows): pre-select from the browser's platform; each tab embeds that OS's Zero to REPL video (click-to-load) and the official install commands. Babashka is in every path.
   - macOS `huGyv5yLIM8`, Linux `544xOAIg3y0`, Windows (WSL) `TQgzA3QLz-k`.
4. Three checks, each with a copy button: `java --version`, `clojure -M -e '(clojure-version)'`, `bb --version`. Then "Stuck? Ask in #adventofcode".
5. Editor: VS Code + Calva; side link to `https://clojure.org/guides/editors`.
6. Finish: get the template, Jack-In, evaluate `(+ 1 2)`, go to the Day 1 tutorial.
7. The path as numbered steps that work without JS; an optional diagram.
8. Below the main path, an optional "Using an AI agent?" link to the template's setup skill. Never part of the main route.

## Workbench requirements

1. The hub's zero-install path: a scratchpad for trying expressions, not a solving environment. No input fetching, no saved sessions. Links to Start here for the full local setup. No squint, not even a link.
2. Scittle, vendored and pinned (the prototype used 0.8.33 from jsDelivr; self-host the same file).
3. Results print as Clojure data via `pr-str`, with a small label (map, vector, set, seq…).
4. Nothing leaves the browser; nothing is saved; never asks for the AoC session cookie.
5. No AI.
6. Starter recipes from `recipes.edn`, made-up data, linked from DIY recipes (`/workbench/#parse`).
7. Real `<textarea>`; Tab is not trapped; a "Craft it: run code" button plus Ctrl/Cmd+Enter; results and errors in an `aria-live` region; errors link to the "Reading an error" how-to.

## Checks

`bb check` builds for each date: `2026-11-30`, `2026-12-01T04:59Z`, `2026-12-01T05:00Z`, `2026-12-05T04:59Z`, `2026-12-05T05:00Z`, `2026-12-07`, `2026-12-11`, `2026-12-18`, `2026-12-23`, `2027-01-15`. For every HTML page it fails if:

- [ ] there isn't exactly one element with the primary-button class;
- [ ] a day page before its gate contains a replay embed, a solutions list, a tutorial link or ring links;
- [ ] a locked or not-yet-out day page lacks its gate text with the date;
- [ ] `clojure-template-aoc` appears while `:template/public?` is `false`;
- [ ] a guest name appears whose entry isn't `:confirmed? true`;
- [ ] the employer is named anywhere in the HTML (Jordan's role is "Software Engineer, Clojure Team"); the name is checked by hash (`src/nooks/denylist.clj`);
- [ ] a link points to a workplace tool (wiki, chat workspace, shared doc; Clojurians Slack is allowed);
- [ ] the forbidden words appear in an `h1`–`h3` or a chip: immutable, functional, data-oriented, concurrent, fast, productive, expressive, scalable;
- [ ] an internal link points to a page that wasn't built;
- [ ] an external request would fire on load (any `src` or stylesheet `href` that isn't same-origin);
- [ ] an English route has no `/pt-br/` twin;
- [ ] an `img` lacks `alt`, or a decorative `svg` lacks `aria-hidden`.

Before the dates, it runs the employer-name and workplace-link checks over every tracked text file and every commit message reachable from `HEAD` (CI checks out full history for this).

Print a one-line summary per date. Exit non-zero on any failure.

## Daily routine in December (target: under 10 minutes)

1. After a session: add the video map to `data/videos.edn` (id, title, chapters, summary placeholder until Jordan writes it).
2. After the gate opens: add credited solution links to `data/days.edn` `:day/solutions` and any ring members to `data/ring.edn`.
3. Commit on a branch, open a PR, check it locally with `bb dev` (and `bb build --as-of` for a future date), merge. The 05:05 UTC scheduled build handles gate openings with no commit needed.
