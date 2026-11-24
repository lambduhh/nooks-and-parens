# Nooks & Parens

The public hub for a December 2026 Clojure Advent of Code campaign: the schedule of live sessions, how to join, session replays with chapters, beginner material, an in-browser Workbench, and links to community solutions. It's for people new to Clojure first, then returning Clojurians and people following along.

Jordan Miller owns this project. **All site copy is Jordan's to write.** You build structure, styling, data plumbing and checks; you use placeholders for words.

## Read these first

| File | What it holds |
|---|---|
| `docs/plan.md` | What to build, in order, with acceptance checks. Start here. |
| `docs/decisions.md` | What's decided and what's still open. Don't reopen decided items. |
| `docs/design-system.md` | Colours, type, components, and the power-tool backlog. |
| `docs/content-model.md` | The EDN files every page is built from, and how page state is derived. |
| `docs/hosting.md` | Where the site is hosted and how it deploys. |
| `prototype/nooks-and-parens-v4.html` | The approved look and feel. A visual reference: port its CSS and structure, don't ship the file. |
| `notes/` | Git-ignored. Jordan's private design brief and handoffs. **Never copy text from `notes/` into tracked files** without checking the public-repo rules below. |

## Stack and commands

A static site. No server, no database.

- **babashka** renders every page that isn't a blog post (landing, calendar, day pages, video pages, Learn, Start here, Workbench, About) from `data/*.edn` into `public/`, using hiccup.
- **quickblog** renders `/blog/` (roundups, explanation articles, the announcement).
- **Scittle** (vendored into `public/vendor/`, pinned) runs the Workbench in the browser.
- A few lines of plain JS for click-to-load video and the toolbelt shortcuts.

Site tasks (add them to `bb.edn` as part of plan task 1; it already holds the docs tasks in [Docs metadata](#docs-metadata-temper)):

| Command | Does |
|---|---|
| `bb dev` | Build, serve `public/` locally, rebuild on changes |
| `bb build` | Build the whole site into `public/` as of now |
| `bb build --as-of 2026-12-07` | Build as if it were that date (UTC). Use it to check spoiler states |
| `bb check` | Build for a set of dates and run every invariant in `docs/plan.md` § Checks |
| `bb test` | Unit tests for date and state logic |
| `bb blog:new "Title"` | New quickblog post |
| `bb deploy` | CI only. Deploys `public/` (see `docs/hosting.md`) |

Planned layout:

```
bb.edn
data/        site.edn days.edn videos.edn recipes.edn ring.edn i18n/{en,pt-BR}.edn
src/nooks/   build.clj state.clj pages/*.clj layout.clj check.clj
blog/        quickblog posts (Markdown)
templates/   quickblog templates matching the site layout
assets/      css/site.css, js/site.js, vendor/scittle.js, fonts if self-hosted
public/      build output (git-ignored)
prototype/   design reference
docs/        planning docs (tracked)
notes/       private (git-ignored)
```

## Rules that are never broken

### Copy
- Use placeholders, marked `<span class="draft">…</span>`, for any sentence Jordan hasn't written. Never ship invented copy as final.
- These labels are decided and must appear exactly: **Nooks & Parens** (name), **"It's dangerous to go alone! Take this."** (tagline), **"Clojure. A power tool for evolving systems."** (positioning line), **"What're ya REPLin'?"** (heading over the session schedule, a heading, never a button), **DIY recipes** (per-day beginner how-tos), **Workbench** (in-browser eval).

### Advent of Code
- Don't make it look like Advent of Code: no dark background with green and gold text, no copy of its font, no ASCII-art calendar picture, no star motifs, no AoC-like name.
- Never include puzzle text, puzzle example inputs, or anyone's input. Link to puzzles; don't copy them. Tutorials and recipes use made-up data.
- No AI-generated solutions published as answers. No AI in the Workbench.
- The Workbench is a scratchpad, not a solver: no fetching puzzle inputs, no saved sessions, no squint (not even a link).

### Spoilers
- A day's solutions, replay, tutorial and webring links appear only from **unlock + 3 days** (unlock is 00:00 EST = 05:00 UTC).
- The gate is a visible state with the date in words ("Solutions open Fri 4 Dec, 05:00 UTC"). Never blurred or hidden content revealed by a click.
- DIY recipes are not solutions: they stay visible before the gate and never hint at an answer.

### Gated data
- Don't link the template repo while `:template/public?` is `false` in `data/site.edn`. Show "coming soon".
- Don't show a guest's name unless their entry has `:confirmed? true`. Otherwise "To be announced".

### Public repo
- No links to or data from internal company systems (internal chat, wikis, issue trackers, channel IDs).
- Don't quote or cite internal positioning documents. The messaging rules below are the whole of what you need.
- Jordan's employer role is disclosed on `/about`. Don't use the employer as proof anywhere else.

### Messaging
- One primary button per page. Default: join `#adventofcode` on Clojurians Slack (`https://clojurians.net`). Day pages: "Join session N on Discord" (`https://discord.clojure.camp`) before the spoiler date, "Discuss Day NN in #adventofcode" after it.
- No feature words as headings or badges: immutable, functional, data-oriented, concurrent. Don't lead with fast, productive, expressive or scalable.
- Never name a competing language in a headline or opening.
- "Evolving" describes systems, never Clojure itself.
- Show data before code: hero and preview visuals are REPL results as data, not walls of parentheses.
- Every video and article ends with one next step.

### Game references
- The labels carry the jokes. **No game art, logos, sprites, game fonts or character names** anywhere.

### Accessibility (WCAG 2.2 AA)
- Only use colour pairs listed in `docs/design-system.md`; every pair there is checked.
- State is always in words, never colour or icon alone.
- Real headings, lists, tables, buttons. Decorative SVG gets `aria-hidden="true"`.
- Visible focus. Works at 320 CSS px wide without sideways scrolling (data tables excepted).
- Respect `prefers-reduced-motion`.
- Number-key shortcuts: never fire in inputs or with modifier keys, and have an on/off switch.
- Workbench results and errors are announced through `aria-live`.
- Every video has captions.

### Privacy
- No cookies, no trackers, no third-party requests on page load. Fonts and Scittle are self-hosted.
- YouTube loads only on click, from `youtube-nocookie.com`.
- The Workbench sends nothing anywhere and never asks for an Advent of Code session cookie.

## Docs metadata (temper)

Every Markdown file in `docs/` except `index.md` starts with OKF frontmatter. [temper](https://github.com/lambduhh/temper) checks it; temper is pinned by `:git/sha` in `bb.edn`, and the schema is `docs/metadata-schema.edn`.

| Command | Does |
|---|---|
| `bb validate` | Frontmatter: `type`, review level and its label, dates, provenance |
| `bb links` | Links and anchors in `docs/`. Links into `notes/` are warnings |
| `bb maturity --check` | The generated table below matches the schema (`bb maturity` regenerates it) |
| `bb new-doc --type Guide --title "…" --path docs/x.md` | A new doc with valid frontmatter |
| `bb review <pr>` | Raise docs Jordan marked Viewed in a PR to L2 |

Review levels, generated from the schema by `bb maturity` (don't edit between the markers):

<!-- review-maturity:table:begin -->
| Level | Label | Meaning |
|---|---|---|
| **L0** | AI-generated | No human review. Raw AI output. |
| **L1** | Human-directed | Human specified what to produce. Output not yet verified. |
| **L2** | Human-reviewed | Human read the output, corrected obvious errors. Claims not individually checked. |
| **L3** | Human-verified | Human verified specific claims against primary sources (running system, database, upstream docs). |
| **L4** | Human-endorsed | Human takes ownership. Content is treated as human-authored with AI assistance. |
<!-- review-maturity:table:end -->

- **Write `review_maturity` as the code plus its label,** e.g. `L2 (Human-reviewed)`.
- **At L2 and above, `reviewed_at` is required.** If a doc changes after its review, re-review it, or lower the level.
- **AI-drafted docs set `ai_assisted`, `session` and `tools`.** Use `unrecorded` only when the provenance genuinely wasn't captured.
- **No frontmatter needed** on data files (`*.edn`) or `prototype/`.
- **CI runs all three checks:** on PRs and on pushes to `main` (`.github/workflows/docs-metadata.yml`).

## Workflow

- Work on a branch and open a PR. Don't push to `main`.
- No AI attribution in commits or PRs.
- **Notes locations:**
  - plans: `notes/plans/<YYYY-MM-DD>-<slug>-plan.md`;
  - review-run output: `notes/plans/updates/`;
  - handoffs: `notes/handoffs/<YYYY-MM-DD>-<slug>.md`.

  `notes/` is git-ignored, and is never part of a PR.
- **Skills:** use the `handoff` skill (`.claude/skills/handoff/`) at phase boundaries, and `grill-me` (`.claude/skills/grill-me/`) before building something unclear.
- **Commit identity:** commits use the repo-local identity: `user.name` "Jordan Miller", and `user.email` set to the lambduhh GitHub noreply address, never a work address. Check `git config user.email` before the first commit in a new clone.
- Run `bb check` before every PR, and paste its summary in the PR description. When `docs/` changes, also run `bb validate`, `bb links` and `bb maturity --check`.
- When unsure whether something breaks a rule above, stop and ask Jordan.

## Definition of done, per page

- [ ] Built from `data/`, not hard-coded content
- [ ] Correct at every date in the `bb check` date set
- [ ] Exactly one primary button
- [ ] Placeholders marked `draft` wherever Jordan hasn't written copy
- [ ] Has a pt-BR route (translated, or a clear "em breve" notice)
- [ ] Keyboard and screen-reader pass; 320 px reflow pass
- [ ] Matches the prototype's components and tokens
