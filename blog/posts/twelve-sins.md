Title: The twelve sins of Clojure
Date: 2026-11-27
Tags: code-review
Preview: true

<!--
  Drafting notes (HTML comments don't show on the page):
  - Preview: true keeps this post off the blog index, tags and feeds. It's still
    reachable at its URL. Delete the line to publish; don't write "Preview: false"
    (quickblog treats any value as a preview in the feed).
  - Wrap unwritten sentences in <span class="draft">…</span> or <p class="draft">…</p>.
  - An "end-of-preview" HTML comment marks where the excerpt on the blog index stops.
  - The site adds one next step (the Slack button) under every post.
-->

The bottleneck has moved from writing code to reviewing it.

The lint config that catches the sins a machine can see is in the AoC template: [lambduhh/clojure-template-aoc#5](https://github.com/lambduhh/clojure-template-aoc/pull/5), commit [`b311c17`](https://github.com/lambduhh/clojure-template-aoc/tree/b311c174e69ad813636a3456743b18fab36205cb).

<!-- end-of-preview -->

## Why review, and why now

- **Review skill grows from writing and debugging.** In Anthropic's RCT, the AI group scored 50% against 67% for hand-coders, with the biggest gap on debugging ([Anthropic](https://www.anthropic.com/research/AI-assistance-coding-skills)). The patterns that preserved learning were *generation-then-comprehension*, *hybrid code-explanation* and *conceptual inquiry*. The groups were tiny (n=2–7), and the authors don't claim causation.
- **Review is mostly about understanding, not finding bugs.** About 15% of review comments point to a possible defect ([Czerwonka et al., 2015](https://www.microsoft.com/en-us/research/wp-content/uploads/2015/05/PID3556473.pdf)). Knowledge transfer and team awareness are common outcomes ([Bacchelli & Bird, 2013](https://www.microsoft.com/en-us/research/publication/expectations-outcomes-and-challenges-of-modern-code-review/)). There is dissent: ["Code Reviews Do Find Bugs"](https://entropicthoughts.com/code-reviews-do-find-bugs).
- **Let machines take the mechanical sins.** In the template, a linter catches **7 of the 12 sins at least partly**. That was verified with `bb sins`, which lints a fixture of deliberately sinful code. The other 5 are what human review is for.

## The twelve sins

"Caught by" was checked against the template at `b311c17`. **Partly** means the linter catches some forms of the sin, not all.

| # | Sin | Caught by | Source |
|---|---|---|---|
| 1 | Side effects inside a lazy sequence | clj-kondo `unused-value`, Splint `lint/dorun-map` (partly) | [Sierra: Lazy Effects](https://stuartsierra.com/2015/08/25/clojure-donts-lazy-effects/), his "number one" Don't |
| 2 | `concat` in a loop | **a reviewer** | [Sierra: Concat](https://stuartsierra.com/2015/04/26/clojure-donts-concat/) |
| 3 | Threading mistakes: `->` vs `->>`, `as->` alone, `some->` going nil | **a reviewer** | [Sierra: Thread as](https://stuartsierra.com/2018/07/15/clojure-donts-thread-as/); [FAQ: arg order](https://clojure.org/guides/faq#arg_order) |
| 4 | `read-string` on input | clj-kondo `discouraged-var` (configured) | [docstring](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/read-string) |
| 5 | `if` with no else | clj-kondo `missing-else-branch`, Splint `lint/if-else-nil` | [Sierra: Single-branch if](https://stuartsierra.com/2015/06/16/clojure-donts-single-branch-if/) |
| 6 | `def` inside a function | clj-kondo `inline-def` | [Style guide](https://guide.clojure.style/#dont-def-vars-inside-fns) |
| 7 | Optional arguments faked with `& [x]` | **a reviewer** | [Sierra: Optional Arguments](https://stuartsierra.com/2015/06/01/clojure-donts-optional-arguments-with-varargs/) |
| 8 | A parameter that might be a value or a collection | **a reviewer** | [Sierra: Heisenparameter](https://stuartsierra.com/2015/06/10/clojure-donts-heisenparameter/) |
| 9 | `#()` misuse | Splint `lint/fn-wrapper` (partly) | [Sierra: Short Fn Syntax](https://stuartsierra.com/2019/09/11/clojure-donts-short-fn-syntax/) |
| 10 | `:refer :all`, shadowing `clojure.core` | clj-kondo `refer-all`, `shadowed-var` (turned on) | [Style guide](https://guide.clojure.style/#prefer-require-over-use) |
| 11 | Java interop where Clojure has a function | Splint `style/prefer-clj-string` (partly) | [Style guide](https://guide.clojure.style/#prefer-clojure-string-over-interop) |
| 12 | Datomic: more than one db value in one unit of work | **a reviewer** | [Datomic best practices](https://docs.datomic.com/reference/best.html#consistent-db-value-for-unit-of-work) |

## Bad and better

1. **Lazy effects.** `(map println lines)` prints nothing if the result is discarded. Use `(run! println lines)` or `doseq`.
2. **`concat` in a loop.** `(reduce (fn [acc row] (concat acc (parse row))) [] rows)` can overflow the stack far from the cause. Use `(into [] (mapcat parse) rows)`.
3. **Threading.** `(->> grid (get-in [y x]))` puts `grid` last, where it means something else; `get-in` wants the collection first, so use `(get-in grid [y x])` or `->`. And `(some-> m :pos neighbours)` quietly returns nil when `:pos` is missing.
4. **`read-string`.** `(read-string line)` on puzzle input can execute code. Use `parse-long` for numbers, or `clojure.edn/read-string` for data.
5. **No else.** `(if (wall? p) :blocked)`. Use `(when (wall? p) :blocked)`, and save `if` for two branches.
6. **`def` in a function.** A debugging `(def last-state s)` left in a solver. Use a REPL `comment` block, or tap>.
7. **Varargs as options.** `(defn solve [input & [part2?]] ...)` silently drops extra arguments. Use multi-arity, or an options map.
8. **Heisenparameter.** `(defn move [robot-or-robots dir] ...)` checks `coll?` inside. Take one robot and `map` it at the call site.
9. **`#()`.** `#(inc %)` is `inc`. `#([% %])` *calls* a vector, so use `(fn [x] [x x])`.
10. **Namespace hygiene.** `(:require [clojure.string :refer :all])` and `(defn score [map] ...)`, where the `map` argument shadows `clojure.core/map`. Use `[clojure.string :as str]` and rename the local, e.g. `grid`.
11. **Java-isms.** `(.split line ",")` and `(.toUpperCase s)`. Use `str/split` and `str/upper-case`.
12. **Datomic basis.** Calling `(d/db conn)` twice in one function, so two queries see different worlds. Take `db` once and pass it down.

## Notes

- **Sierra's caveat.** Sierra called the series "a mix of technical recommendations and my personal stylistic preferences". Sins 5 and 9 are closer to style.
- **Records aren't a sin.** Alex Miller favours records for internal domain data ([Clojurians log](https://clojurians-log.clojureverse.org/clojure/2015-11-12)).
- **Parens are no longer an AI failure mode.** clojure-mcp now says frontier models "edit Clojure reliably" ([README](https://github.com/bhauman/clojure-mcp)). The AI-written Clojure failures worth naming are Java-isms (11) and over-long code.
