---
type: Reference
title: Glossary
description: "Project vocabulary for Nooks & Parens: what each recurring term means in these docs, and which similar-looking terms are different things."
tags: [nooks-and-parens, glossary]
created: 2026-11-24
modified: 2026-11-24
ai_assisted: Claude Code (claude-opus-5-5)
session: f97674e1-0e49-4d17-94aa-7708e57af519
tools: [Read, Bash]
agents_skills: []
generated: { by: "claude-code/opus-5-5", at: 2026-11-24T12:57:43Z }
review_maturity: L0 (AI-generated)
review_note: AI-generated; not yet human-reviewed
---

# Glossary

> **AI-generated (L0).** Claude Code drafted every definition below from the other files in `docs/` and from `CLAUDE.md`. No one has checked them yet, and AI drafts often contain false statements. Each entry links to its source. Where they disagree, the source wins.

Terms that have a specific meaning in this project. Each entry says what the term means here, where it comes from, and which nearby terms are different things. Link an entry as `glossary.md#term`. `bb links --glossary` lists entries that no doc links to yet.

## The campaign

### Day
One Advent of Code puzzle, numbered 1 to 12 (`:aoc/days`, assumed, because Advent of Code hasn't published a count for 2026). Each day has an [unlock](#unlock) and a [spoiler date](#spoiler-date), and gets a page at `/days/NN/`. Source: [content model](content-model.md#datadaysedn).
**Not the same as** a [session](#session): a day is a puzzle, a session is a live event about it.

### Session
A live event in the Clojure Camp Discord, numbered (`:n`), held on its own date, and listed under the day it covers (`:day/session`). Session 1 covers Day 1 and is on Fri 4 Dec, three days after Day 1 unlocks. There's no session on Fri 11 Dec. Source: [content model](content-model.md#datadaysedn), [plan dates](plan.md#dates-that-drive-the-plan).
**Not the same as** a [replay](#replay), which is the recording of a session.

### Format
How a session is run, e.g. `:live-coding`, workshop or demo (`:day/session :format`, `:video/format`). Each format gets a card on `/formats/`. Source: [content model](content-model.md#datavideosedn), [plan task 5](plan.md#5-pages-claude-code).
**Not the same as** [kind](#kind).

### Kind
What a video is for: `:tutorial`, `:how-to`, `:reference` or `:explanation` (`:video/kind`). It defaults from the format: workshop → tutorial, demo → how-to, anything else → explanation. The four kinds also match the four drawers on `/learn/`. Source: [content model](content-model.md#datavideosedn).

### Guest
Someone who appears in a session. Their name is shown only when their entry has `:confirmed? true`. Until then the site says "To be announced". Source: [CLAUDE.md](../CLAUDE.md#gated-data).

### Replay
The recorded video of a session, from `data/videos.edn`. It appears on its day page only when the session is done, a video for that day exists ([`replay?`](content-model.md#derived-state)) and the day is past its [spoiler gate](#spoiler-gate). Each replay also gets a video page at `/videos/NN/`. Source: [content model](content-model.md#derived-state).

### Tutorial
A written walkthrough for days 1 to 3, at `/learn/tutorials/day-NN/`. Tutorials are gated like solutions and use made-up data only. Source: [plan task 15](plan.md#p2-december-722-dec), [decisions](decisions.md#decided).
**Not the same as** a [DIY recipe](#diy-recipe), which is ungated, or a [replay](#replay), which is a video.

### DIY recipe
A short beginner how-to for one technique (for example parsing), shown on day pages as a recipe card. A day lists its recipes in `:day/recipes`, and the recipes live in `data/recipes.edn` with made-up data. Recipes aren't solutions. They stay visible before the gate and never hint at an answer. Each one links to a matching Workbench starter. "DIY recipes" is a [decided label](#decided-labels). Source: [CLAUDE.md](../CLAUDE.md#spoilers), [content model](content-model.md#datarecipesedn).

### Community solutions
Credited links to other people's solutions for a day (`:day/solutions`). Shown only after the day's spoiler gate. Source: [content model](content-model.md#datadaysedn).
**Not the same as** the [webring](#webring): a solution is a link for one day, and a webring member is a person who has opted in.

### Webring
The opted-in members in `data/ring.edn`, linked to each other through prev / random / next pages generated per member, so no server is needed. A day's ring links appear only after its spoiler date. Who keeps the member list is still [open](decisions.md#open). Source: [content model](content-model.md#dataringedn), [plan task 17](plan.md#p2-december-722-dec).

### Roundup
A blog post published each Monday in December (7, 14 and 21 Dec). Source: [plan dates](plan.md#dates-that-drive-the-plan).

### Template
The starter repo `clojure-template-aoc`. The site doesn't link it while `:template/public?` is `false`, and shows "coming soon" in its place. The Codespaces route on Start here depends on it too. Source: [CLAUDE.md](../CLAUDE.md#gated-data), [content model](content-model.md#datasiteedn).

### Workbench
The page at `/workbench/` where Clojure runs in the browser on [Scittle](#scittle). It's a scratchpad, not a solver: no fetching puzzle inputs, no saved sessions, no AI, and nothing leaves the browser. A [decided label](#decided-labels). Source: [plan](plan.md#workbench-requirements).

### Start here
The onboarding page at `/start/`: one curated path from official material, with no installer. Source: [plan](plan.md#start-here-requirements).

## Spoilers and page state

### Unlock
When a day's puzzle opens: 00:00 EST, which is 05:00 UTC (`:day/unlock`). A day is `unlocked?` once the [as-of](#as-of) instant reaches it. Source: [content model](content-model.md#derived-state).

### Spoiler date
Unlock + 3 days (`:day/spoiler-until`). From this instant the day is `open?`, and its solutions, replay, tutorial and ring links can appear. Source: [content model](content-model.md#derived-state), [CLAUDE.md](../CLAUDE.md#spoilers).
**Not the same as** the [spoiler gate](#spoiler-gate): the date is data, and the gate is what the page shows.

### Spoiler gate
What a day page shows until the spoiler date: a sign that gives the date in words (for example "Solutions open Fri 4 Dec, 05:00 UTC"). Gated content is left out of the page. It's never there but blurred or hidden behind a click. Source: [CLAUDE.md](../CLAUDE.md#spoilers), [design system](design-system.md#components).

### Status
A day's spoiler state: `:soon` (not unlocked), `:locked` (unlocked, before the spoiler date) or `:open`. It decides what the day page shows. Source: [content model](content-model.md#derived-state).
**Not the same as** [session state](#session-state).

### Session state
Where a day's session is in time: `:upcoming`, `:today` or `:done`, compared by UTC day. Source: [content model](content-model.md#derived-state).

### Today day
The highest day that has unlocked, or none before 1 Dec. `/today/` redirects to it. Source: [content model](content-model.md#derived-state), [plan task 5](plan.md#5-pages-claude-code).

### Archive state
How the site looks after the last session date (`archive?`): the landing page says all days are out, and nothing says "today". Source: [content model](content-model.md#derived-state), [plan task 20](plan.md#p3-wrap-up-mid-jan).

### As-of
The UTC instant that a build treats as "now". `src/nooks/state.clj` takes it as an argument, and no other code reads the clock. `bb build --as-of 2026-12-07` builds the site as of that date. Source: [content model](content-model.md#derived-state), [CLAUDE.md](../CLAUDE.md#stack-and-commands).

## Pages and components

### Primary button
The one main action on a page, styled as a green pill. Every page has exactly one, and `bb check` fails otherwise. Its target depends on the page and the day's status. Source: [CLAUDE.md](../CLAUDE.md#messaging), [content model](content-model.md#derived-state).

### Decided labels
Six fixed strings that must appear exactly as written: the name, the tagline, the positioning line, "What're ya REPLin'?", "DIY recipes" and "Workbench". Source: [CLAUDE.md](../CLAUDE.md#copy).

### Draft placeholder
A `<span class="draft">…</span>` that holds a sentence Jordan hasn't written yet. Source: [CLAUDE.md](../CLAUDE.md#copy).

### Gate sign
The day-page component that shows the [spoiler gate](#spoiler-gate): a wooden "Closed" sign with the date in words, or "Open" in paper. Source: [design system](design-system.md#components).

### Chip
A small status label that always uses words ("Solutions open", "Live today"), with a distinct border as well as a colour. Source: [design system](design-system.md#components).

### Tool wall
The 12-slot pegboard on the calendar page, one slot per day, with words in every slot. Each day has a tool (`:day/tool`). Source: [design system](design-system.md#components).
**Not the same as** the [toolbelt](#toolbelt).

### Toolbelt
The navigation bar fixed to the bottom of every page: 8 slots with number-key shortcuts and an on/off switch for them. Source: [design system](design-system.md#components), [CLAUDE.md](../CLAUDE.md#accessibility-wcag-22-aa).

### Data readout
A black panel that shows a REPL result as Clojure data. Used on the landing page and in the Workbench. Source: [design system](design-system.md#components).

### Power-tool backlog
The list of power-tool allusions to choose from with Jordan in P2. Each one keeps its meaning in words. Source: [design system](design-system.md#power-tool-backlog-pick-with-jordan-in-p2).

### pt-BR twin
The `/pt-br/` copy of an English route. Every English route needs one, either translated or showing a clear "em breve" notice. Source: [plan task 8](plan.md#8-pt-br-scaffolding-claude-code).

## Build and deploy

### Phase
One of the plan's four milestones: P0 launch, P1 first session, P2 December, P3 wrap-up. Source: [plan dates](plan.md#dates-that-drive-the-plan).

### Check set
The ten dates (and edge instants such as `2026-12-01T04:59Z`) that `bb check` builds the site for, checking every invariant at each one. Source: [plan](plan.md#checks).

### Daily rebuild
The scheduled CI deploy at 05:05 UTC. It opens spoiler gates and moves "today" without a commit. Scheduled runs can start late, so a gate may open a little late but never early. Source: [hosting](hosting.md#claude-code-the-workflow).

### Scittle
A runtime that runs Clojure code in the browser. It's vendored into `public/vendor/`, pinned, and used only by the Workbench. Source: [CLAUDE.md](../CLAUDE.md#stack-and-commands), [plan](plan.md#workbench-requirements).

### quickblog
The tool that renders `/blog/` from Markdown posts. Every other page is rendered by babashka from `data/`. Source: [CLAUDE.md](../CLAUDE.md#stack-and-commands).

## Docs process

### temper
The tool, pinned in `bb.edn`, that checks this bundle's frontmatter, links and maturity table (`bb validate`, `bb links`, `bb maturity`). Its rules live in `docs/metadata-schema.edn`. Source: [CLAUDE.md](../CLAUDE.md#docs-metadata-temper).

### OKF frontmatter
The metadata block at the top of each doc in `docs/` (all except `index.md`). `index.md` declares `okf_version: "0.2"`. Claude Code didn't find what "OKF" stands for in the files it read. Source: [CLAUDE.md](../CLAUDE.md#docs-metadata-temper).

### Review maturity
How much human review a doc has had, from L0 (AI-generated) to L4 (Human-endorsed), written as the code plus its label. At L2 and above `reviewed_at` is required. Source: [CLAUDE.md](../CLAUDE.md#docs-metadata-temper).

### Provenance
The `ai_assisted`, `session` and `tools` keys on an AI-drafted doc, which record what produced it and what it could check its claims against. `unrecorded` is allowed only when this wasn't captured. Source: [CLAUDE.md](../CLAUDE.md#docs-metadata-temper).
