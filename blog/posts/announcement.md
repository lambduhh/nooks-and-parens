Title: Clojure Template and Tooling Helpers for Advent of Code 2026
Date: 2026-11-24
Tags: announcement
Draft-Title: false
Style: rfc
Rfc-Number: 0001
Rfc-Category: Informational
Rfc-Author: L. Jordan Miller
Rfc-Org: Software Engineer, Clojure Team
Rfc-Date: November 2026
Draft-Header: false

<!--
  Jordan's post, laid out as an old-school RFC (Style: rfc).

  Metadata (the lines above the first blank line):
  - Preview: true keeps this post off the blog index, tags and feeds. It's still
    reachable at /powertools-and-parens/blog/announcement.html. Delete the line to
    publish; don't write "Preview: false".
  - Draft-Title / Draft-Header: true show the title / the RFC header block as
    drafts (only the word true counts; false or deleting the line both work).
  - Rfc-Org is the optional line under the author in the header.
  - Rfc-*: the header block (group, number, category, author, date). The footer
    reuses Rfc-Author and Rfc-Category.

  Body:
  - Sections are Markdown headings, numbered by hand ("## 4. The Plan").
    Each heading gets GitHub's id for its text: "## 4. The Plan" -> #4-the-plan,
    "## 9. Author's Address" -> #9-authors-address.
    If you rename or renumber one, update its Table of Contents link;
    bb check fails on a TOC link with no matching heading.
  - Wrap unwritten sentences in <span class="draft">…</span> or <p class="draft">…</p>.
    Plain Markdown paragraphs work too, once a paragraph is yours.
  - The end-of-preview marker below the Abstract is where the blog index
    excerpt stops.
  - Link hub pages by site path. Inside <p class=…> blocks use HTML,
    <a href="/powertools-and-parens/start/">Start here</a>; in plain paragraphs Markdown
    works too. Cite a reference with <a href="#ref-aoc">[AOC]</a>.
  - The page adds one next step (the Slack button) after the post.
-->

## Status of This Memo

This memo provides information for the Clojure community. Distribution of this memo is unlimited.

## Abstract

<p class="body"> What a girl wants: I want developers new to Clojure to have one clear <a href="/powertools-and-parens/start/">place to start</a>, with <a href="/powertools-and-parens/learn/">guides, references and examples</a>. I want to reduce the friction of our onboarding experience. I want to give experimenters a reason to stay: other people to learn with, <a href="/powertools-and-parens/formats/">formats that are fun</a>, and finally - I want to give Clojure developers a way to bring others with them.</p>

<!-- end-of-preview -->

## Table of Contents

- [Status of This Memo](#status-of-this-memo)
- [Abstract](#abstract)
- [Table of Contents](#table-of-contents)
- [1. Introduction](#1-introduction)
- [2. Background](#2-background)
- [3. How I Got Here](#3-how-i-got-here)
- [4. The Plan](#4-the-plan)
- [5. Audience](#5-audience)
- [6. Joining](#6-joining)
- [7. Security Considerations](#7-security-considerations)
- [8. References](#8-references)
- [9. Author's Address](#9-authors-address)

## 1. Introduction

A few weeks ago I started taking music lessons. It's funny how much they look like my journey learning to program.

Until now I've been mostly self-taught. So every lesson is part review, part learning how to do the thing the *actual* right way.

And just like programming, I keep getting better. The more code katas I did, the more bugs I hunted down, the more Advent of Code puzzles (<a href="#ref-aoc">[AOC]</a>) I solved every week, the better I got. Same with the keys. My fingers find their place on their own now. Practising with a metronome means I can actually *hear* whether I'm on the beat or not.

One of the best things about working with a teacher is the scaffolding. It's intentional: each lesson builds on the last. And watching my teacher's workflow on the MPC is what's helping me actually *finish* a beat.

The last thing I noticed: just like skating, when I learn around other people, the learning works better and it's more fun. Same goes for programming.

Some people ask: what's the point of lessons, and practising every day, when you can just use Suno?

They're missing the point. The journey, the struggle: that's the important part.

My teacher says learning is all about consistency over time. Five to fifteen minutes every day beats three hours once a week. The same is true for problem solving. The same is true for programming.

Advent of Code is exactly that kind of daily practice, a small puzzle every day, and it's better with people. That's what Power Tools &amp; Parens is for.


## 2. Background

This started with a simple ask: "do something for Advent of Code."

Of course I had to be super extra about it. I care about pedagogy, and about doing things right. So "something" turned into a research project (<a href="#ref-research">[RESEARCH]</a>), a template (<a href="#ref-template">[TEMPLATE]</a>), a plan for December, and this hub.

## 3. How I Got Here

Before building anything, I wanted to know what people actually do when they solve Advent of Code in Clojure. So I went and found every Advent of Code repo I could, starting from Thiago Carvalho's datasets (<a href="#ref-datasets">[DATASETS]</a>): 10,960 repos across four languages, 3,512 of them in Clojure.

Then I stuck it all in [Datomic](https://www.datomic.com/). For good luck. (If you want a how-to-Datomic reference, the analysis repo doubles as one (<a href="#ref-research">[RESEARCH]</a>), and the template has a short <a href="/powertools-and-parens/learn/howto/datomic/">Datomic how-to</a> too.)

With the data in one place, I looked at three things: which dependencies people reach for, what kinds of problems they're solving, and how they solve them.

That analysis is where some of the how-to guides came from. The libraries that kept showing up ([math.combinatorics](https://github.com/clojure/math.combinatorics), [data.priority-map](https://github.com/clojure/data.priority-map), [instaparse](https://github.com/Engelberg/instaparse), [core.async](https://github.com/clojure/core.async), [loom](https://github.com/clj-commons/loom)) each got a guide in the template: <a href="/powertools-and-parens/learn/howto/combinatorics/">combinatorics</a>, <a href="/powertools-and-parens/learn/howto/shortest_path/">shortest paths</a>, <a href="/powertools-and-parens/learn/howto/parsing/">parsing</a>, <a href="/powertools-and-parens/learn/howto/channels/">channels</a> and <a href="/powertools-and-parens/learn/howto/graphs/">graphs</a> (<a href="#ref-template">[TEMPLATE]</a>). They're all on the hub: <a href="/powertools-and-parens/learn/">Learn</a>.

## 4. The Plan

<p class="body">December: twelve puzzles, and sessions to work through them together. The calendar is <a href="/powertools-and-parens/days/">here</a>.</p>

Sessions run on weekdays from Friday 4 to Tuesday 22 December. What each one looks like is still taking shape, so details go up on the calendar as they're set.

Some sessions will be recorded. Recordings go up on [ClojureTV](https://www.youtube.com/@ClojureTV) (<a href="#ref-cljtv">[CLJTV]</a>) afterwards and link from here.

## 5. Audience

<p class="body">Who it's for: people new to Clojure (1-3 years experience). First time using Clojure? 
<a href="/powertools-and-parens/start/">Start here</a>.</p>

## 6. Joining

<p class="body">How to join: check out the #adventofcode channel on <a href="https://clojurians.net">Clojurians Slack</a> (<a href="#ref-slack">[SLACK]</a>), and the sessions on the <a href="https://discord.clojure.camp">Clojure Camp Discord</a> (<a href="#ref-discord">[DISCORD]</a>).</p>

## 7. Security Considerations

<p class="body">The spoiler policy: anything here that gives away a solution waits until 3 days after its puzzle unlocks.</p>

## 8. References

<dl class="rfc-refs">
<dt id="ref-aoc">[AOC]</dt>
<dd>"Advent of Code 2026", <a href="https://adventofcode.com/2026">https://adventofcode.com/2026</a>.</dd>
<dt id="ref-slack">[SLACK]</dt>
<dd>"Clojurians Slack", <a href="https://clojurians.net">https://clojurians.net</a>.</dd>
<dt id="ref-discord">[DISCORD]</dt>
<dd>"Clojure Camp Discord", <a href="https://discord.clojure.camp">https://discord.clojure.camp</a>.</dd>
<dt id="ref-template">[TEMPLATE]</dt>
<dd>"clojure-template-aoc", <a href="https://github.com/lambduhh/clojure-template-aoc">https://github.com/lambduhh/clojure-template-aoc</a>. Its guides on the hub: <a href="/powertools-and-parens/start/">Start here</a> and <a href="/powertools-and-parens/learn/">Learn</a>.</dd>
<dt id="ref-research">[RESEARCH]</dt>
<dd>"clojure-aoc-research": the analysis, with Datomic Local and Datalog, <a href="https://github.com/lambduhh/clojure-aoc-research">https://github.com/lambduhh/clojure-aoc-research</a>.</dd>
<dt id="ref-datasets">[DATASETS]</dt>
<dd>Carvalho, T., "programming-language-vitality": the repo lists the analysis starts from, <a href="https://github.com/thiagooak/programming-language-vitality/tree/916e0f55bd44afe55382c9925f05cf3e243abfc8">https://github.com/thiagooak/programming-language-vitality</a> (commit 916e0f5).</dd>
<dt id="ref-cljtv">[CLJTV]</dt>
<dd>"ClojureTV", <a href="https://www.youtube.com/@ClojureTV">https://www.youtube.com/@ClojureTV</a>.</dd>
<dt id="ref-hub">[HUB]</dt>
<dd>"Power Tools &amp; Parens", <a href="/powertools-and-parens/">https://lambduhh.github.io/powertools-and-parens/</a>.</dd>
</dl>

## 9. Author's Address

L. Jordan Miller, Software Engineer, Clojure Team. [@lambduhhh](https://linktr.ee/lambduhhh)
