# Nooks & Parens

> It's dangerous to go alone! Take this.

The hub for a December 2026 Clojure [Advent of Code](https://adventofcode.com) campaign: the live-session schedule, how to join, session replays, beginner material, an in-browser Workbench, and links to community solutions. It's for people new to Clojure first, then returning Clojurians and people following along.

**Live site:** <https://lambduhh.github.io/nooks-and-parens/>

**Status:** the site shell is up; most copy is still a placeholder (shown as `[draft]` on the page). Day pages, the Workbench and video pages arrive through December. See [the build plan](docs/plan.md).

Not affiliated with Advent of Code. Puzzles are linked, never copied.

## Run it locally

You need [babashka](https://babashka.org) (`bb`) 1.12.201 or later.

```sh
bb dev
```

That builds the site, serves it at <http://localhost:1888/nooks-and-parens/>, and rebuilds and reloads the page when you change anything in `data/`, `src/`, `assets/`, `blog/` or `templates/`.

To see the site as it will look on another date (spoiler gates, "today", the schedule):

```sh
bb dev --as-of 2026-12-07          # a bare date means 12:00 UTC
bb build --as-of 2026-12-04T05:00Z # an exact instant
```

## Commands

| Command | Does |
|---|---|
| `bb dev` | Build, serve locally, rebuild on changes. Flags: `--as-of`, `--port` |
| `bb build` | Build the whole site into `public/`, as of now or `--as-of` |
| `bb check` | Build for each date in the check set and fail on any broken rule (below) |
| `bb test` | Unit tests: data shape, date and spoiler-gate logic, UI strings, the check rules |
| `bb blog:new "Title"` | New blog post, created as a preview. Flag: `--slug` |
| `bb template:bump` | Move the pinned template commit to its latest `main` (then `bb check` and a PR) |
| `bb validate`, `bb links`, `bb maturity --check` | Checks for the planning docs in `docs/` ([temper](https://github.com/lambduhh/temper)) |

## How it's built

A static site; no server, no database, no JavaScript framework.

- **Pages** are rendered by babashka with [hiccup](https://github.com/weavejester/hiccup) from the EDN files in [`data/`](data/). A daily update is a data edit, never a template edit.
- **Page state** (is a puzzle out, are its solutions open, is a session today) is computed by pure functions in [`src/nooks/state.clj`](src/nooks/state.clj) from an `as-of` instant. Nothing else reads the clock, which is what makes `--as-of` and `bb check` possible.
- **Guides** under `/start/` and `/learn/` are the [template repo](https://github.com/lambduhh/clojure-template-aoc)'s own Markdown and how-to namespaces, rendered at build time from a commit pinned in [`data/template.edn`](data/template.edn). Links between guides stay on the hub; anything else opens on GitHub at that commit. See [`src/nooks/template.clj`](src/nooks/template.clj).
- **The blog** at `/blog/` is rendered by [quickblog](https://github.com/borkdude/quickblog) inside the same page layout. Its `base.html` is generated from [`src/nooks/layout.clj`](src/nooks/layout.clj) on every build.
- **UI strings** live in [`data/i18n/`](data/i18n/). A `{:draft "…"}` value is a placeholder and always renders with a visible draft marker.
- **Portuguese (pt-BR):** every page has a `/pt-br/` twin; untranslated twins say so ("Tradução em breve").
- **Deploys** run from GitHub Actions to GitHub Pages on every push to `main` and daily at 05:05 UTC, just after puzzles unlock and spoiler gates open. See [hosting](docs/hosting.md).

```
data/        site, days, videos, recipes, webring, template pin, UI strings (EDN)
src/nooks/   build, state, layout, pages, blog, check, dev
blog/posts/  blog posts (Markdown)
templates/   quickblog templates
assets/      CSS, JS, self-hosted fonts
test/        bb test
docs/        planning docs: plan, decisions, design system, content model, hosting, glossary
prototype/   the approved look, as a single HTML file (reference only)
```

## Writing a blog post

```sh
bb blog:new "My post"
bb dev
```

Open <http://localhost:1888/nooks-and-parens/blog/my-post.html> and edit `blog/posts/my-post.md`; the page reloads as you save.

- A new post starts with `Preview: true`: reachable at its URL, but not on the blog index, tags or feeds. Delete the line to publish. (Don't write `Preview: false`; quickblog treats any value as a preview in its feeds.)
- `Style: rfc` lays a post out like an old-school RFC; see [the announcement](blog/posts/announcement.md) for the header fields.
- Raw HTML is allowed in posts. Wrap unwritten text in `<span class="draft">…</span>`.

## Checks

`bb check` builds the site for ten dates around the spoiler-gate edges and fails if any page:

- doesn't have exactly one primary button;
- loads anything from another site, or has an inline script or style (the Content-Security-Policy would block it);
- links to a page or in-page anchor that doesn't exist;
- links the template repo before it's public, or names a guest who hasn't confirmed;
- uses a feature word (immutable, functional, fast, …) in a heading or status chip;
- has an image without `alt` or an icon without `aria-hidden`;
- has the wrong `lang`, or has no `/pt-br/` twin;
- lists a preview post on the blog index, tags or feeds.

Each rule has a test that feeds it a violation.

## Privacy and security

- No cookies, no trackers, no analytics, no third-party requests on page load. Fonts are self-hosted.
- Session videos (from December) load from `youtube-nocookie.com`, and only when you click.
- A Content-Security-Policy `<meta>` tag allows only same-origin scripts, styles, fonts and images.
- The one thing stored in your browser is whether the toolbelt number-key shortcuts are on (`localStorage`, never sent anywhere).
- `bb dev` listens on `127.0.0.1` only.
- Posts may contain raw HTML, so a post PR is reviewed like a code PR.

## Contributing

Work on a branch and open a pull request; don't push to `main`. Run `bb test` and `bb check` first, and paste the `bb check` summary in the PR. If you change `docs/`, also run `bb validate`, `bb links` and `bb maturity --check`. CI runs all of them.

*[draft] How people can help, and where to ask first.*

## Credits

- [quickblog](https://github.com/borkdude/quickblog), [babashka](https://babashka.org) and [Scittle](https://github.com/babashka/scittle), by [borkdude](https://github.com/borkdude) and contributors.
- [Nunito](https://github.com/googlefonts/nunito) and [IBM Plex Mono](https://github.com/IBM/plex), under the SIL Open Font License ([assets/fonts/](assets/fonts/)).

## Licence

- **Code** (everything not listed below) is under the [Eclipse Public License 2.0](LICENSE) (`EPL-2.0`).
- **Written content** (blog posts in `blog/posts/`, the UI copy in `data/i18n/`, and the docs in `docs/`) is under [Creative Commons Attribution 4.0 International](LICENSE-CONTENT) (`CC-BY-4.0`).
- **Fonts** in `assets/fonts/` keep their own SIL Open Font License; see the `OFL-*.txt` files there.

Advent of Code puzzles and names belong to their owners and aren't part of this repo.
