---
type: Decision Log
title: "Decisions"
description: "What's decided and what's open for Nooks & Parens, and who decides."
tags: [nooks-and-parens, decisions]
created: 2026-11-24
modified: 2026-11-24
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
| 2026-11-24 | **Alpha for friends' review.** All placeholder copy accepted as final (draft markers removed); placeholders that were instructions rather than copy, and empty areas (the "Why it works" drawer), removed. About and toolbelt slot 7 left out, so the toolbelt has 6 slots. No alpha banner or feedback link: Jordan collects feedback by message. Tagged `v0.1.0-alpha` | New copy Jordan hasn't written still uses `{:draft …}`. About (with the employer disclosure) returns before launch |
| 2026-11-24 | **Power-tool emoji and a favicon.** Decorative, `aria-hidden` emoji on page and Learn-drawer headings (`layout/tool-emoji`); a self-hosted SVG favicon of the toolbelt's wrench | Not on RFC-style posts. Headings still read as their words |
| 2026-11-24 | **Spoiler gate: unlock + 4 days** (was 3). Solutions, replays, tutorials and ring links open at 05:00 UTC four days after the puzzle unlocks | Jordan's choice, so the announcement and the gate agree. Still meets the 2026-11-06 rule of at least 3 days |
| 2026-11-24 | **The template's guides are rendered into the hub.** `docs/setup-flowchart.md` is `/start/`; the tutorial, README, how-to namespaces and AI files are under `/learn/`. Rendered at build time from a commit pinned in `data/template.edn`, moved by `bb template:bump` in a PR | Pinned so `bb check` reviews template changes and a template edit can't break the daily gate-opening build. Mermaid diagrams show as source; the guides' numbered steps carry the same route |
| 2026-11-24 | **Template is public, with "Use this template" on.** `:template/public?` is `true` | Unlocks the template links and the Codespaces route |
| 2026-11-24 | **Employer disclosure:** Jordan may be named as "Software Engineer, Clojure Team" as an author affiliation (post author lines), as well as on `/about` | Still never as proof |
| 2026-11-24 | **Toolbelt slot 7 stays, as a placeholder.** It links to a stub page (`/slot-7/`) with a draft label | Was Roundups, cut the same day. Jordan may put reference docs there |
| 2026-11-24 | **Roundups cut.** No weekly roundup posts | Community solutions are still linked per day, after the gate, and the January wrap-up stays |
| 2026-11-24 | **Repo is public.** `lambduhh/nooks-and-parens` | Needed for a free GitHub Pages site. Jordan changed visibility in the GitHub web UI |
| 2026-11-24 | **Hosting: a GitHub Pages project site** at `https://lambduhh.github.io/nooks-and-parens/`, deployed from GitHub Actions | Supersedes the Cloudflare Pages recommendation earlier the same day; Jordan found its setup too complicated. Every link carries `/nooks-and-parens`. Details: `docs/hosting.md` |
| 2026-11-24 | **Stack: hybrid.** babashka renders pages from EDN; quickblog renders `/blog/`; Scittle runs only the Workbench | Supersedes "simple ClojureScript site" (2026-11-10). All output is static files |
| 2026-11-24 | **Look and feel: the v4 prototype**, a cozy life-sim game's menus with a workshop and power-tool theme, in the Clojure colours plus warm neutrals | Add more power-tool allusions (backlog in `docs/design-system.md`). ASCII styling is allowed but not used in v4 |
| 2026-11-24 | **Name and labels:** Nooks & Parens; tagline "It's dangerous to go alone! Take this."; "What're ya REPLin'?" over the session schedule; "DIY recipes" for per-day how-tos; "Workbench" for in-browser eval | No game art, logos, fonts or character names anywhere |
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
| Analytics | None / cookieless (Cloudflare Web Analytics) with outbound clicks to Slack and the template as the events | Jordan | Before launch if wanted |
| Webring: who keeps the member list, and the opt-in step | `data/ring.edn` edited by Jordan from Slack requests / a PR from each member | Jordan | P2 |
| Which power-tool allusions to add | Backlog in `docs/design-system.md` | Jordan with Claude Code | P2 |
| pt-BR for `/blog/` | A "Tradução em breve" notice at `/pt-br/blog/` (what the MVP builds) / translate posts / leave the blog English-only with no pt-BR page | Jordan | Before pt-BR pages go live |
| Who reviews pt-BR | Candidate in Jordan's notes | Jordan | Before pt-BR pages go live |
| pt-BR renderings of the tagline, "What're ya REPLin'?" and "DIY recipes" | | The pt-BR reviewer | Before pt-BR pages go live |
| A setup clinic in the Clojure Camp Discord before 1 Dec | | Jordan | Mon 30 Nov |
| A custom domain later | | Jordan | After December |
