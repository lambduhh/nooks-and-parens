Title: init-commit
Date: 2026-11-24
Tags: announcement
Preview: true
Draft-Title: true
Style: rfc
Rfc-Group: Nooks & Parens Working Group
Rfc-Number: 0001
Rfc-Category: Informational
Rfc-Author: J. Miller
Rfc-Date: November 2026
Draft-Header: true

<!--
  Jordan's draft, laid out as an old-school RFC (Style: rfc). Every paragraph
  below is a placeholder that says what goes there; write over it in place.

  This repo is public, and HTML comments like this one are published in the
  page source. Keep internal notes out of this file.

  Metadata (the lines above the first blank line):
  - Preview: true keeps this post off the blog index, tags and feeds. It's still
    reachable at /nooks-and-parens/blog/announcement.html. Delete the line to
    publish; don't write "Preview: false".
  - Draft-Title / Draft-Header: true show the title / the RFC header block as
    drafts. Delete each line when that part is final.
  - Rfc-*: the header block (group, number, category, author, date). The footer
    reuses Rfc-Author and Rfc-Category.

  Body:
  - Sections are Markdown headings, numbered by hand ("## 2. What We'll Do").
    Each heading gets an id from its text: lower case, spaces to hyphens, so
    "## 3. How to Take Part" -> #3.-how-to-take-part. If you rename or renumber
    one, update its Table of Contents link; bb check fails on a TOC link with no
    matching heading.
  - Wrap unwritten sentences in <span class="draft">…</span> or <p class="draft">…</p>.
    Plain Markdown paragraphs work too, once a paragraph is yours.
  - The end-of-preview marker below the Abstract is where the blog index
    excerpt stops.
  - Link hub pages with site paths: /nooks-and-parens/start/, /nooks-and-parens/days/.
  - Don't link the template repo until it's public; say "coming soon".
  - The page adds one next step (the Slack button) after the post.

  Before publishing:
  - "Why" opens with an idea or a problem, not features. Nothing leads with
    fast, productive, expressive or scalable. No competing language in the
    title or opening.
  - One recommended route, named once.
  - "Evolving" describes systems, never Clojure itself.
  - No puzzle text or inputs.
  - Guests named only once they've agreed.
  - Every link opened by a human.
-->

## Status of This Memo

<p class="draft">Proposed. Comments in #adventofcode on Clojurians Slack until [date].</p>

## Abstract

<p class="draft">One paragraph: what (twelve live sessions and a daily thread for each puzzle), when (1–22 December), who it's for, and how to join.</p>

<!-- end-of-preview -->

## Table of Contents

1. [Why](#1.-why)
2. [What We'll Do](#2.-what-we'll-do)
    1. [The Plan](#2.1.-the-plan)
    2. [Session Formats](#2.2.-session-formats)
    3. [Who It's For](#2.3.-who-it's-for)
    4. [How It Works](#2.4.-how-it-works)
3. [How to Take Part](#3.-how-to-take-part)
4. [Open Questions](#4.-open-questions)
5. [Security Considerations](#5.-security-considerations)
6. [References](#6.-references)
7. [Acknowledgements](#7.-acknowledgements)
8. [Author's Address](#8.-author's-address)

## 1. Why

<p class="draft">Your story. Beats: music lessons, and the humility of finding the holes in self-taught knowledge. Fundamentals and daily practice: putting in the reps is like practising scales. Structure: learning to build projects you can keep building on, and getting out of ruts. Like music, programming is better with people.</p>

<p class="draft">Daily reps and the puzzles: Advent of Code is a daily practice, and its FAQ makes the point about doing the work yourself ([AOC-FAQ]). Building on what you've built: part 2 usually changes the question part 1 asked. Better with people leads to the sessions.</p>

<p class="draft">Optional: echo "Clojure. A power tool for evolving systems." If you do, pair it with the Advent of Code framing: each day is a small evolving system.</p>

## 2. What We'll Do

### 2.1. The Plan

<p class="draft">The session dates, and the daily threads. Each session's format is picked from a pool, by puzzle and guest availability; no day-by-day plan, and guests named only once they've agreed. Also the template (coming soon), how-to guides and references. Link the calendar: /nooks-and-parens/days/.</p>

### 2.2. Session Formats

<p class="draft">Share the formats list: it's a resource in its own right. Managers can use it as a guide to leading technical conversations that keep humans in the loop, against "cognitive debt" and "comprehension debt" ([DEBT-COG], [DEBT-COMP]). People engaging with each other is how learning happens and memories form. Link /nooks-and-parens/formats/.</p>

### 2.3. Who It's For

<p class="draft">Experienced developers trying Clojure, December returners, managers who lead technical conversations, and meetup organisers and collaborators.</p>

### 2.4. How It Works

<p class="draft">REPL-first. Tutor, not solver ([AOC-FAQ]). AI that checks the work: the research repo, with its line counts ([RESEARCH]).</p>

## 3. How to Take Part

<p class="draft">The one route. First: join Clojurians Slack and follow #adventofcode ([SLACK]). Start at the hub, Nooks &amp; Parens: guides, links and the Workbench, to try Clojure in the browser without installing. Then the template for the full setup (coming soon). Live sessions are on the Clojure Camp Discord ([DISCORD]); new programmers start with the Clojure Camp handbook ([HANDBOOK]).</p>

## 4. Open Questions

<p class="draft">What you want comments on, and by when. Candidates: session guests; which past puzzle to solve live; "a sin a day"; where to collect community solutions (a webring).</p>

## 5. Security Considerations

<p class="draft">The spoiler delay: solutions, replays and tutorials appear three days after each puzzle unlocks. No puzzle inputs in repos. The Workbench sends nothing anywhere and never asks for your Advent of Code session cookie.</p>

## 6. References

<!-- Public links only. "Checked" in the private scaffold meant opened on
     2026-11-24; open each again before publishing. Drop any you don't cite. -->

<dl class="rfc-refs">
<dt>[AOC]</dt>
<dd>"Advent of Code 2026", <a href="https://adventofcode.com/2026">https://adventofcode.com/2026</a>.</dd>
<dt>[AOC-FAQ]</dt>
<dd>"About Advent of Code", <a href="https://adventofcode.com/2026/about">https://adventofcode.com/2026/about</a>.</dd>
<dt>[SLACK]</dt>
<dd>"Clojurians Slack", <a href="https://clojurians.net">https://clojurians.net</a>.</dd>
<dt>[DISCORD]</dt>
<dd>"Clojure Camp Discord", <a href="https://discord.clojure.camp">https://discord.clojure.camp</a>.</dd>
<dt>[CAMP]</dt>
<dd>"Clojure Camp", <a href="https://clojure.camp">https://clojure.camp</a>.</dd>
<dt>[HANDBOOK]</dt>
<dd>"Clojure Camp handbook", <a href="https://handbook.clojure.camp">https://handbook.clojure.camp</a>.</dd>
<dt>[Z2R]</dt>
<dd>"Zero to REPL", clojure.org, 30 March 2026, <a href="https://clojure.org/news/2026/03/30/zero_to_repl">https://clojure.org/news/2026/03/30/zero_to_repl</a>. Videos: <a href="https://www.youtube.com/watch?v=huGyv5yLIM8">macOS</a>, <a href="https://www.youtube.com/watch?v=544xOAIg3y0">Linux</a>, <a href="https://www.youtube.com/watch?v=TQgzA3QLz-k">Windows (WSL)</a>.</dd>
<dt>[CLJTV]</dt>
<dd>"ClojureTV", <a href="https://www.youtube.com/@ClojureTV">https://www.youtube.com/@ClojureTV</a>.</dd>
<dt>[TEACHING]</dt>
<dd>"Teaching Clojure Together", Clojure/Conj 2024, <a href="https://www.youtube.com/watch?v=xOPoYHxZjdc">https://www.youtube.com/watch?v=xOPoYHxZjdc</a>.</dd>
<dt>[RESEARCH]</dt>
<dd>"clojure-aoc-research", <a href="https://github.com/lambduhh/clojure-aoc-research">https://github.com/lambduhh/clojure-aoc-research</a>.</dd>
<dt>[DOCS]</dt>
<dd>"ClojureDocs", <a href="https://clojuredocs.org">https://clojuredocs.org</a>.</dd>
<dt>[DEBT-COMP]</dt>
<dd>Gorman, J., "Comprehension debt: The ticking time bomb of LLM-generated code", 30 September 2025, <a href="https://codemanship.wordpress.com/2025/09/30/comprehension-debt-the-ticking-time-bomb-of-llm-generated-code/">https://codemanship.wordpress.com/2025/09/30/comprehension-debt-the-ticking-time-bomb-of-llm-generated-code/</a>. See also Osmani, A., <a href="https://addyosmani.com/blog/comprehension-debt/">https://addyosmani.com/blog/comprehension-debt/</a>.</dd>
<dt>[DEBT-COG]</dt>
<dd>Kosmyna, N., et al., "Your Brain on ChatGPT: Accumulation of Cognitive Debt when Using an AI Assistant for Essay Writing Task", MIT Media Lab, 2025, <a href="https://www.media.mit.edu/publications/your-brain-on-chatgpt/">https://www.media.mit.edu/publications/your-brain-on-chatgpt/</a>. A preprint (arXiv 2506.08872); its findings are contested, see <a href="https://arxiv.org/pdf/2601.00856">https://arxiv.org/pdf/2601.00856</a>.</dd>
<dt>[HUB]</dt>
<dd>"Nooks &amp; Parens", <a href="/nooks-and-parens/">https://lambduhh.github.io/nooks-and-parens/</a>.</dd>
</dl>

## 7. Acknowledgements

<p class="draft">Clojure Camp's resources: the handbook, the mob sessions and the Discord ([CAMP]).</p>

## 8. Author's Address

<p class="draft">Name, and where to reach you (a link, not an email address, unless you want one public).</p>
