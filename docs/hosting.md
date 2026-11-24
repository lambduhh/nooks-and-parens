---
type: Reference
title: "Hosting"
description: "Where Nooks & Parens is hosted (a GitHub Pages project site) and how it deploys."
tags: [nooks-and-parens, hosting]
created: 2026-11-24
modified: 2026-11-24
ai_assisted: "Claude via Claude Desktop (Claude Design); updated by Claude Code for the GitHub Pages decision"
session: unrecorded
tools: unrecorded
agents_skills: []
review_maturity: L1 (Human-directed)
review_note: "Drafted in Claude Design at Jordan Miller's direction, 2026-11-24; rewritten by Claude Code the same day after Jordan chose GitHub Pages. Not yet reviewed."
---

# Hosting

**Decision (Jordan, 2026-11-24): a GitHub Pages project site at <https://lambduhh.github.io/nooks-and-parens/>, deployed from GitHub Actions.** Cloudflare Pages was the first recommendation; Jordan dropped it the same day because its setup was too complicated. It stays below as an alternative.

Facts below were checked on 2026-11-24. Free tiers change; re-check the linked pages before relying on a number.

## What the site needs

- Static files only: `bb build` produces `public/`.
- Deploys from CI, because the build needs babashka.
- **A scheduled daily rebuild just after 05:00 UTC**, so spoiler gates and "today" change on time with no commit.
- HTTPS, a custom 404 page, a Content-Security-Policy, no cookies.
- Not a company property (the hub discloses Jordan's employer role but isn't run by the employer).

## How GitHub Pages fits

GitHub allows **one user site per account** (`lambduhh.github.io`, already used), but **one project site per repository**. The `nooks-and-parens` repo, once public, publishes at `https://lambduhh.github.io/nooks-and-parens/`. Source: [GitHub Pages limits](https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits) (1 GB site, soft 100 GB a month; not for commercial sites, and a free community hub isn't one).

What follows from it:

| Constraint | What the site does |
|---|---|
| The site lives under `/nooks-and-parens/` | Every generated link starts with `:site/base-path` in `data/site.edn`. `bb check` fails on an internal link outside it. `bb dev` serves the same path locally |
| No custom response headers | The CSP is a `<meta http-equiv="Content-Security-Policy">` tag in every page ([`layout.clj`](../src/nooks/layout.clj)). A meta CSP can't set `frame-ancestors` |
| No PR previews | PRs run `bb test`, `bb check` and `bb build` only. Review a PR locally with `bb dev` |
| The repo must be public | Not yet. Jordan made it public in the GitHub web UI on 2026-11-24, then private again the same day, pending a security review. The site can't deploy until the review approves making it public |

Pages source is **GitHub Actions** (set in the repo's Pages settings).

## Setup

### Jordan, once (done 2026-11-24)
1. Make the repo public (blocked until the security review approves it; see `docs/decisions.md`).
2. In Settings → Pages, set the source to GitHub Actions.

### Claude Code: the workflow
[`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml):

| Trigger | Runs |
|---|---|
| Push to `main` | `bb test`, `bb check`, `bb build`, then `actions/upload-pages-artifact` and `actions/deploy-pages` |
| Daily at 05:05 UTC | The same, so gates and "today" move without a commit |
| Manual dispatch | The same |
| Pull request | `bb test`, `bb check`, `bb build`; no deploy |

Notes:
- Actions are pinned by commit SHA, with the release tag in a comment. babashka is pinned to a version.
- Scheduled runs use the default branch and can start late when GitHub is busy. Gates may open a little late, never early, because state is computed at build time from the real clock.
- `public/404.html` is in the site style, with links to Home and the calendar. GitHub Pages serves it for any missing path under the project.

## Security

- **No third-party requests on load.** Fonts are self-hosted, scripts are same-origin, and the CSP allows only `'self'` for scripts, styles, fonts, images and fetches. `bb check` fails a page that loads an off-site URL, or has an inline script or style.
- **Frames** only from `https://www.youtube-nocookie.com`, for click-to-load videos.
- **No cookies.** The one stored value is the toolbelt-shortcut switch, in `localStorage` on the visitor's device; it's never sent anywhere.
- **Content is trusted.** Pages and posts are written by maintainers, merged by Jordan. Data values are escaped by hiccup; Markdown posts may contain raw HTML (that's how `draft` placeholders work), so treat a post PR like a code PR.
- **`bb dev` listens on 127.0.0.1 only.** Its live reload is a same-origin script written only by dev builds.
- **CI** has `contents: read`; only the deploy job gets `pages: write` and `id-token: write`, and only on `main`.

## Alternatives considered

| Host | URL shape | Notes |
|---|---|---|
| Cloudflare Pages (first recommendation, dropped) | `nooks-and-parens.pages.dev` | Static requests unlimited; 500 builds a month. Direct Upload from CI, `_headers` for a real CSP header, PR preview URLs. Setup needs an account, an API token and two repo secrets |
| Cloudflare Workers static assets | `nooks-and-parens.<account>.workers.dev` | Cloudflare now puts new features here rather than Pages; the URL includes the account subdomain |
| Netlify | `nooks-and-parens.netlify.app` | New free accounts get 300 credits a month at 15 per production deploy, about 20 deploys: too few for a daily rebuild |
| Vercel Hobby | `nooks-and-parens.vercel.app` | Non-commercial personal use only; a campaign run partly in an employee's devrel role is a grey area |
| Codeberg Pages | `<user>.codeberg.page/nooks-and-parens` | Mid-migration to a new server (old one stops 2026-12-31) |

Sources: Cloudflare Pages free-tier summaries ([DEV Community](https://dev.to/david_viejo_4d48fdfa7cfff/cloudflare-pages-free-tier-limits-pricing-2026-1f8f), [convika.com](https://convika.com/compare/cloudflare-pages-vs-workers/)), [Cloudflare Workers static assets](https://developers.cloudflare.com/workers/static-assets/), [Netlify free plan](https://www.netlify.com/blog/introducing-netlify-free-plan/) and [a 2026 limits summary](https://netli.fyi/blog/netlify-free-plan-limits-2026), [Vercel fair use guidelines](https://vercel.com/docs/limits/fair-use-guidelines), [Codeberg Pages](https://docs.codeberg.org/codeberg-pages/) and its [migration guide](https://docs.codeberg.org/codeberg-pages/migrating-from-pages-v2/).

Moving hosts later is a config change: set `:site/url` and `:site/base-path` (empty at a domain root) in `data/site.edn` and swap the deploy steps.

## Later: a custom domain

GitHub Pages supports one per repository. Point DNS at it, set it in Settings → Pages, then set `:site/url` and `:site/base-path ""`. Keep `:site/url` the only place the domain is written.
