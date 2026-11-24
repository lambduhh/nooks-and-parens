---
type: Reference
title: "Hosting"
description: "Where Nooks & Parens is hosted (Cloudflare Pages recommended, GitHub Pages as fallback) and how it deploys."
tags: [nooks-and-parens, hosting]
created: 2026-11-24
modified: 2026-11-24
ai_assisted: "Claude via Claude Desktop (Claude Design)"
session: unrecorded
tools: unrecorded
agents_skills: []
review_maturity: L1 (Human-directed)
review_note: "Drafted in Claude Design at Jordan Miller's direction, 2026-11-24. The session and tools weren't recorded. Not yet reviewed."
---

# Hosting

**Decision (Jordan, 2026-11-24): a free platform subdomain. Recommended host: Cloudflare Pages, deployed from GitHub Actions by Direct Upload, at `nooks-and-parens.pages.dev`.** Fallback: a GitHub Pages *project* site.

Facts below were checked on 2026-11-24. Free tiers change; re-check the linked pages before relying on a number.

## What the site needs

- Static files only: `bb build` produces `public/`.
- A name of its own on a free subdomain, with a custom domain possible later.
- Deploys from CI, because the build needs babashka.
- **A scheduled daily rebuild just after 05:00 UTC**, so spoiler gates and "today" change on time with no commit. That's about 31 deploys in December from the schedule alone, plus pushes and PR previews.
- HTTPS, a custom 404 page, response headers (CSP), no cookies.
- Not a company property (the hub discloses Jordan's employer role but isn't run by the employer).

## A correction first: github.io is still available

GitHub allows **one user site per account** (`lambduhh.github.io`, already used), but **one project site per repository**, with no cap on how many repositories have one. A public `nooks-and-parens` repo can publish at `https://lambduhh.github.io/nooks-and-parens/`. Source: [GitHub Pages limits](https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits).

The catch: the URL carries the personal handle and a sub-path, so every internal link must work under `/nooks-and-parens/`. That's why it's the fallback, not the first choice.

## Options compared

| Host | URL shape | Free-tier fit for this site | Notes |
|---|---|---|---|
| **Cloudflare Pages** (recommended) | `nooks-and-parens.pages.dev` | Static requests unlimited; 500 builds a month; 20,000 files, 25 MiB per file | Direct Upload means our CI builds with bb and Cloudflare only serves. `_headers` and `404.html` supported. PR previews get their own URLs. Cloudflare Web Analytics is cookieless, if analytics is ever wanted (open decision) |
| Cloudflare Workers static assets | `nooks-and-parens.<account>.workers.dev` | Same static-request treatment | Cloudflare now puts new features here rather than Pages, but the free URL includes the account subdomain, so it's less clean. A reasonable later move |
| GitHub Pages project site (fallback) | `lambduhh.github.io/nooks-and-parens/` | 1 GB site; soft 100 GB/month; no build limit with a custom Actions workflow | Needs a public repo. Not for commercial sites (a free community hub is fine) |
| Netlify | `nooks-and-parens.netlify.app` | New free accounts get 300 credits a month; a production deploy costs 15 credits, so about 20 deploys | **Too few for a daily scheduled rebuild.** Sites pause when credits run out |
| Vercel Hobby | `nooks-and-parens.vercel.app` | Generous | **Non-commercial personal use only.** A campaign run partly in an employee's devrel role is a grey area. Avoid |
| Codeberg Pages | `<user>.codeberg.page/nooks-and-parens` | Free | Code would live on Codeberg. Its Pages service is mid-migration to a new server (old one stops 2026-12-31). Not now |

Sources: Cloudflare Pages free-tier summaries ([DEV Community](https://dev.to/david_viejo_4d48fdfa7cfff/cloudflare-pages-free-tier-limits-pricing-2026-1f8f), [convika.com](https://convika.com/compare/cloudflare-pages-vs-workers/)), [Cloudflare Workers static assets](https://developers.cloudflare.com/workers/static-assets/), [Netlify free plan](https://www.netlify.com/blog/introducing-netlify-free-plan/) and [a 2026 limits summary](https://netli.fyi/blog/netlify-free-plan-limits-2026), [Vercel fair use guidelines](https://vercel.com/docs/limits/fair-use-guidelines), [Codeberg Pages](https://docs.codeberg.org/codeberg-pages/) and its [migration guide](https://docs.codeberg.org/codeberg-pages/migrating-from-pages-v2/). Cloudflare's own Pages limits page didn't load during research; confirm the numbers at <https://developers.cloudflare.com/pages/platform/limits/>.

## Setup (Cloudflare Pages, Direct Upload)

### Jordan, once
1. Create a **personal** Cloudflare account (not a company one).
2. Create the Pages project: `npx wrangler pages project create nooks-and-parens --production-branch main`. If the name is taken, pick another and update `data/site.edn` `:site/url`.
3. Create an API token with the **Cloudflare Pages: Edit** permission. Note the account ID.
4. In the GitHub repo, add secrets `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID`.

### Claude Code: the workflow
`.github/workflows/deploy.yml`, roughly as below. **Pin each action and tool to a current version before merging**; the versions shown are placeholders to verify.

```yaml
name: deploy
on:
  push:
    branches: [main]
  pull_request:
  schedule:
    - cron: "5 5 * * *"   # 05:05 UTC daily, after puzzles and gates open at 05:00
  workflow_dispatch:

permissions:
  contents: read
  deployments: write

concurrency:
  group: deploy-${{ github.ref }}
  cancel-in-progress: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: DeLaGuardo/setup-clojure@VERIFY   # installs babashka
        with:
          bb: VERIFY
      - run: bb check
      - run: bb build
      - uses: cloudflare/wrangler-action@v3      # VERIFY
        with:
          apiToken: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          accountId: ${{ secrets.CLOUDFLARE_ACCOUNT_ID }}
          command: pages deploy public --project-name=nooks-and-parens --branch=${{ github.head_ref || github.ref_name }}
```

Notes:
- Scheduled runs use the default branch and can start late when GitHub is busy. Gates may open a little late, never early, because state is computed at build time from the real clock.
- PRs from forks don't get secrets, so their deploy step will fail. Skip deploy when `github.event.pull_request.head.repo.fork` is true.
- `public/_headers`: a Content-Security-Policy that allows only same-origin scripts, styles and fonts, and frames from `https://www.youtube-nocookie.com`. Test the Workbench under the CSP; if Scittle needs more, document exactly what and why.
- `public/404.html`: in the site style, with links to Home and the calendar.

## Fallback: GitHub Pages project site

Same `bb build`, then `actions/upload-pages-artifact` and `actions/deploy-pages`. Set a `:site/base-path "/nooks-and-parens"` in `data/site.edn` and make every generated link use it. Keep links relative-safe from day one so switching hosts is a config change.

## Later: a custom domain

Add it in the Pages dashboard and point DNS at it. Keep `:site/url` the only place the domain is written. The `pages.dev` URL keeps working.
