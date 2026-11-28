---
type: Decision Log
title: "Decisions"
description: "What's decided and what's open for Power Tools & Parens, and who decides."
tags: [nooks-and-parens, decisions]
created: 2026-11-24
modified: 2026-11-28
ai_assisted: "Claude via Claude Desktop (Claude Design); updated by Claude Code"
session: unrecorded
tools: unrecorded
agents_skills: []
review_maturity: L1 (Human-directed)
review_note: "Drafted in Claude Design at Jordan Miller's direction, 2026-11-24. The session and tools weren't recorded. Not yet reviewed."
---

# Decisions

What's decided, what's open, and who decides. Decided items aren't reopened without Jordan. Newest first within each table.

## Decided

| Date | Decision | Notes |
|---|---|---|
| 2026-11-28 | **Analytics: cookieless counts with GoatCounter**, off until `:analytics/goatcounter` is set. Counts page visits, primary-button clicks (Slack, Discord), the template link, Workbench runs and (once the embed exists) video plays | Jordan. Was an open question (none / Cloudflare Web Analytics). Our own same-origin script, no `count.js`; respects DNT and GPC. See [analytics](analytics.md) |
| 2026-11-28 | **The Workbench has exercises.** 15, in learning order: Advent of Code idioms (parse, count, grids, sets, building results) with four Brain Teasers where they explain a surprise. Types from Teaching Tech Together ch. 12: predict, fill in the blanks, work backward, fix the bug, change it. Each has a task, a goal checked by value, a hint while it doesn't match, and a why with links once it does | Before, learners never had to edit code. Review: `notes/plans/updates/2026-11-28-teachtogether-review.md` |
| 2026-11-27 | **Workbench starters are Clojure Brain Teasers.** Eight teasers from the book (Alex Miller and L. Jordan Miller, Pragmatic Bookshelf, 2025) replace the four made-up recipes, verbatim, credited on the page. Picked: puzzles 6, 7, 8, 9, 20, 22, 23, 24, the ones whose answer and reason match in ClojureScript and on the JVM | The book is reviewed, so the examples' answers are. Puzzles about numbers, NaN, identity, type hints, I/O or JVM errors stay out of the Workbench: Scittle answers differently |
| 2026-11-27 | **Spoiler gate: unlock + 3 days** (was 4). Every session now falls on or after its puzzle's gate; Session 1 (Fri 4 Dec, Day 1) had fallen a day early. A test fails any session dated before its gate | Supersedes the 2026-11-24 four-day row. Meets the 2026-11-06 rule of at least 3 days |
| 2026-11-27 | **Launch: draft markers removed.** Every remaining `{:draft …}` string accepted as final; the Formats page leaves out empty fields instead of showing "To be written" | New copy Jordan hasn't written still uses `{:draft …}` |
| 2026-11-27 | **"Shop hours" heads the session schedule,** with "Session calendar" under it. Replaces "What're ya REPLin'?", which didn't say what the list was | A hardware-store riff that still reads as a schedule |
| 2026-11-27 | **Session details stay hidden until announced.** A session's format, day covered, time and guests render only when `:announced? true` in `data/days.edn`; until then the row is decorative pixel bars, a screen-reader line, and the date. `bb check` fails a page that shows an unannounced session's title | Sessions' contents aren't settled. Mirrors guests' `:confirmed?` |
| 2026-11-27 | **Fewer promises about content.** No "live" wording; no "one thread per puzzle"; recordings are posted after some sessions, not every one; no promise of per-day solution content | Supersedes the 2026-11-24 videos row's "most days also get a piece of content" |
| 2026-11-24 | **Session videos are hosted on ClojureTV** and embedded on each day's page (click to load, `youtube-nocookie.com`) | Jordan, in the announcement brief. Most days also get a piece of content about the solution |
| 2026-11-24 | **Alpha for friends' review.** All placeholder copy accepted as final (draft markers removed); placeholders that were instructions rather than copy, and empty areas (the "Why it works" drawer), removed. About and toolbelt slot 7 left out, so the toolbelt has 6 slots. No alpha banner or feedback link: Jordan collects feedback by message. Tagged `v0.1.0-alpha` | New copy Jordan hasn't written still uses `{:draft …}`. About returns before launch |
| 2026-11-24 | **Power-tool emoji and a favicon.** Decorative, `aria-hidden` emoji on page and Learn-drawer headings (`layout/tool-emoji`); a self-hosted SVG favicon of the toolbelt's wrench | Not on RFC-style posts. Headings still read as their words |
| 2026-11-24 | **Spoiler gate: unlock + 4 days** (was 3). Solutions, replays, tutorials and ring links open at 05:00 UTC four days after the puzzle unlocks | Jordan's choice, so the announcement and the gate agree. Still meets the 2026-11-06 rule of at least 3 days |
| 2026-11-24 | **The template's guides are rendered into the hub.** `docs/setup-flowchart.md` is `/start/`; the tutorial, README, how-to namespaces and AI files are under `/learn/`. Rendered at build time from a commit pinned in `data/template.edn`, moved by `bb template:bump` in a PR | Pinned so `bb check` reviews template changes and a template edit can't break the daily gate-opening build. Mermaid diagrams show as source; the guides' numbered steps carry the same route |
| 2026-11-24 | **Template is public, with "Use this template" on.** `:template/public?` is `true` | Unlocks the template links and the Codespaces route |
| 2026-11-24 | **No employer name anywhere on the site.** Jordan's role reads "Software Engineer, Clojure Team": in post author lines, the RFC header and on `/about` | Supersedes the employer-disclosure decision earlier the same day. `bb check` fails any page that names the employer |
| 2026-11-24 | **Toolbelt slot 7 stays, as a placeholder.** It links to a stub page (`/slot-7/`) with a draft label | Was Roundups, cut the same day. Jordan may put reference docs there |
| 2026-11-24 | **Roundups cut.** No weekly roundup posts | Community solutions are still linked per day, after the gate, and the January wrap-up stays |
| 2026-11-24 | **Repo is private again, pending a security review.** `lambduhh/nooks-and-parens` was made public, then made private the same day after a security team asked to review whether it should be public. It stays private until that review approves it | Supersedes "Repo is public" (next row). GitHub Pages on a free account needs a public repo, so the site can't deploy until then. While it was private, the history was rewritten to remove the employer's name and group-specific names (see "No employer name anywhere on the site"), so commit IDs from before the rewrite no longer match `main` |
| 2026-11-24 | ~~**Repo is public.**~~ Superseded the same day (previous row). `lambduhh/nooks-and-parens` | Needed for a free GitHub Pages site. Jordan changed visibility in the GitHub web UI |
| 2026-11-24 | **Hosting: a GitHub Pages project site** at `https://lambduhh.github.io/nooks-and-parens/`, deployed from GitHub Actions | Supersedes the Cloudflare Pages recommendation earlier the same day; Jordan found its setup too complicated. Every link carries `/nooks-and-parens`. Details: `docs/hosting.md` |
| 2026-11-24 | **Stack: hybrid.** babashka renders pages from EDN; quickblog renders `/blog/`; Scittle runs only the Workbench | Supersedes "simple ClojureScript site" (2026-11-10). All output is static files |
| 2026-11-24 | **Look and feel: the v4 prototype**, a cozy life-sim game's menus with a workshop and power-tool theme, in the Clojure colours plus warm neutrals | Add more power-tool allusions (backlog in `docs/design-system.md`). ASCII styling is allowed but not used in v4 |
| 2026-11-24 | **Name and labels:** Nooks & Parens; tagline "It's dangerous to go alone! Take this."; "What're ya REPLin'?" over the session schedule; "DIY recipes" for per-day how-tos; "Workbench" for in-browser eval | No game art, logos, fonts or character names anywhere |
| 2026-11-24 | **Name: Power Tools & Parens.** Supersedes "Nooks & Parens" in the "Name and labels" row. The path moves to `/power-tools-and-parens/` | Jordan also wants the repo renamed to match. Internal identifiers (the `nooks.*` namespaces, docs tags, the prototype's filename) keep the old name |
| 2026-11-24 | **Path follows the repo name: `/powertools-and-parens/`.** The repo was renamed `lambduhh/powertools-and-parens` (no hyphen in "powertools"), and `:site/base-path` follows it | Supersedes the path in the previous row. A GitHub Pages project site is served at `/<repo-name>/`, so a mismatch would break every link. The site name stays "Power Tools & Parens" |
| 2026-11-24 | **Tagline decoration:** 📝🧚 after "Take this." | Decorative and `aria-hidden`; the tagline's words are unchanged. Jordan's call on the game-reference rule: an emoji is neither game art nor a character name |
| 2026-11-24 | **Deploy waits for `PUBLISH`.** The deploy job runs only when the repo variable `PUBLISH` is `true` | While the repo is private pending the security review (the "Repo is private again" row), pushes and the daily build still test and build without trying to deploy. Options for publishing: `docs/hosting.md` § Status |
| 2026-11-24 | **Toolbelt order and keys:** 0 Today, 1 Calendar, 2 Workbench, 3 Learn, 4 Videos, 5 First time? Number keys 0–5 match. The landing main menu follows the same order, numbered by the same keys | "First time?" is the toolbelt label only; the page, its breadcrumb, the menu row and links still say "Start here" |
| 2026-11-24 | **Workbench button labels:** "Craft it: run code", "Clear code", "Stop running code"; starter recipes as "Load: <title>" under "Load a starter recipe (replaces your code)" | Jordan chose these to say what each button does; "Craft it" stays as the label's lead |
| 2026-11-24 | **Workbench runs on Scittle.** No squint, not even a credited link | The Workbench is the hub's zero-install path. It's a scratchpad, not a solving environment: no input fetching, no saved sessions (keeps within the 2026-11-06 "no browser solver" decision) |
| 2026-11-24 | **Formats reference also serves managers**: a guide to leading technical conversations that keep humans in the loop | Shapes the `/formats/` copy and structure |
| 2026-11-24 | **Positioning line on the hub:** "Clojure. A power tool for evolving systems." | Leads the landing dialog. "Evolving" describes systems only |
| 2026-11-24 | **Calls to action:** primary = join `#adventofcode` on Clojurians Slack; secondary = the template (text link, only once public); live sessions = Clojure Camp Discord | One primary button per page |
| 2026-11-24 | **Start here:** one curated path from official material, no installer | Requirements in `docs/plan.md` |
| 2026-11-24 | **Style rule:** ASCII or retro styling is fine as long as the hub isn't a copy of Advent of Code's look | |
| 2026-11-10 | Day 1–3 tutorials, published after the spoiler delay | |
| 2026-11-06 | **Spoilers:** solutions linked at least 3 days after a puzzle unlocks | |
| — | **Tutor, not solver:** no AI-generated solutions as answers; no AI in the Workbench | |
| — | **pt-BR:** every public page gets a Portuguese version, reviewed for accuracy | Reviewer to be confirmed (Jordan's notes have the name; don't publish it until they agree) |
| 2026-11-06 | **No browser solver** | The Workbench stays a scratchpad |
| — | **Comments:** none on the hub; discussion goes to `#adventofcode` | Suggested, treated as decided unless Jordan says otherwise |

## Open

| Question | Options | Who decides | Needed by |
|---|---|---|---|
| Tagline and positioning line both on the landing page: too crowded? | Keep both / Jordan picks which leads | Jordan | P0 review |
| Workbench primary button | "Craft it" counts as the page's primary action / also add the Slack button | Jordan | P2. Built 2026-11-24 with the Slack button as primary and "Craft it" as a normal button (Claude Code's default when Jordan said "build it"; not yet Jordan's decision) |
| Webring: who keeps the member list, and the opt-in step | `data/ring.edn` edited by Jordan from Slack requests / a PR from each member | Jordan | P2 |
| Which power-tool allusions to add | Backlog in `docs/design-system.md` | Jordan with Claude Code | P2 |
| pt-BR for `/blog/` | A "Tradução em breve" notice at `/pt-br/blog/` (what the MVP builds) / translate posts / leave the blog English-only with no pt-BR page | Jordan | Before pt-BR pages go live |
| Who reviews pt-BR | Candidate in Jordan's notes | Jordan | Before pt-BR pages go live |
| pt-BR renderings of the tagline, "Shop hours" and "DIY recipes" | | The pt-BR reviewer | Before pt-BR pages go live |
| A setup clinic in the Clojure Camp Discord before 1 Dec | | Jordan | Mon 30 Nov |
| A custom domain later | | Jordan | After December |
