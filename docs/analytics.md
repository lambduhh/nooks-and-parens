---
type: Guide
title: Analytics
description: What Power Tools & Parens counts with GoatCounter, what it never sends, and how to turn it on.
tags: [nooks-and-parens, analytics, privacy]
created: 2026-11-28
modified: 2026-11-28
source: https://www.goatcounter.com/help/pixel
ai_assisted: Claude Opus 5.5 via Claude Code
session: 18c54804
tools: [babashka, WebFetch, gh]
agents_skills: []
generated: { by: "claude-code/claude-opus-5-5", at: 2026-11-28T05:52:08Z }
review_maturity: L1 (Human-directed)
review_note: "Jordan chose GoatCounter and the four counted interactions, 2026-11-28; Claude Code wrote this page. Not yet reviewed."
---

# Analytics

**Decision (Jordan, 2026-11-28): cookieless counts with [GoatCounter](https://www.goatcounter.com/), off until `:analytics/goatcounter` is set in [`data/site.edn`](../data/site.edn).** It counts page visits and four kinds of interaction. It replaces "no analytics" in the privacy rules, which is why the rules and this page changed together.

## What's counted

| Count | GoatCounter path | Where it comes from |
|---|---|---|
| Page visit | the page path, e.g. `/powertools-and-parens/learn/` | [`site.js`](../assets/js/site.js), on every page load |
| Primary button: Slack | `cta-slack` (event) | `data-count` on the button from [`layout/primary-button`](../src/nooks/layout.clj). Covers "Join #adventofcode" and "Discuss Day NN" |
| Primary button: Discord | `cta-discord` (event) | Same, for "Join session N on Discord" |
| Template repo link | `template` (event) | `data-count` on "Get the template" in [`template.clj`](../src/nooks/template.clj) |
| Workbench run | `workbench-run` (event) | `window.npCount` in [`workbench.js`](../assets/js/workbench.js), once per run that has code in it |
| Video play | `video-play-NN` (event) | **Not wired yet:** the click-to-load embed doesn't exist yet (video pages are stubs). When it's built, give its play button `data-count="video-play-NN"` |

For an event, GoatCounter's title column holds the page the click happened on, so "which page sends people to Slack" is a filter, not another event name.

To count something new, add `data-count="name"` to the link or button. Nothing else is needed: one click listener in `site.js` reads it.

## What's sent, and what isn't

Each hit is one request to `https://<code>.goatcounter.com/count`, using the parameters GoatCounter documents as stable ([pixel docs](https://www.goatcounter.com/help/pixel)): `p` (path or event name), `t` (title), `r` (referrer, for page visits only), `e` (event flag), `b` (bot flag, for WebDriver browsers) and `rnd` (a cache buster).

- **No cookies, and nothing is stored on the device.** GoatCounter doesn't set any. It tells visits apart with a hash it rotates server-side; see [GoatCounter's privacy notes](https://www.goatcounter.com/help/privacy) [unverified: Claude didn't read that page in this session].
- **Never sent:** what's typed in the Workbench, screen size, the language switch or shortcut setting, any identifier.
- **Nothing is loaded from GoatCounter.** The script is our own, same-origin; GoatCounter's `count.js` isn't used. The CSP gains the GoatCounter origin in `connect-src` (for `sendBeacon`) and `img-src` (for the image fallback) only.
- **Not sent at all** when the browser sends Do Not Track or Global Privacy Control, from `localhost`/`127.*` (so `bb dev` and `bb test:browser` send nothing), or while a page is prerendered.

## Guards

- `bb check` fails a page whose CSP names any off-site origin except `https://www.youtube-nocookie.com` and the configured GoatCounter origin (`check-csp-origins` in [`check.clj`](../src/nooks/check.clj)). Adding another service means changing that list on purpose.
- The site code is checked against `[a-z0-9][a-z0-9-]*` before it goes into the CSP; anything else fails the build (`layout/analytics-origin`).

## Turning it on

1. Create a free GoatCounter account at <https://www.goatcounter.com/signup> and choose a site code (the `<code>` in `<code>.goatcounter.com`). GoatCounter is free for non-commercial use.
2. In GoatCounter's site settings, consider turning on "Make dashboard viewable by anyone" only if you want public numbers.
3. **Update the README's "Privacy and security" section** (it currently says "no analytics"; that's Jordan's copy to write).
4. Set `:analytics/goatcounter "<code>"` in `data/site.edn`, run `bb check`, open a PR. The next deploy turns counting on; setting it back to `nil` turns it off.

## Alternatives considered

| Option | Why not |
|---|---|
| Cloudflare Web Analytics (the earlier candidate in [decisions](decisions.md)) | Loads Cloudflare's script on every page, and Claude didn't find custom click events in it, so interactions wouldn't be counted [unverified] |
| Plausible | Cookieless with events, but about $9 a month hosted [unverified], or a server to run |
| None | Was the rule until 2026-11-28. Slack joins, Discord, YouTube views and repo traffic are still worth reading alongside these counts |
