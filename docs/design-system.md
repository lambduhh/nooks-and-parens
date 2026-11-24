---
type: Reference
title: "Design system"
description: "Colour tokens, type, components and the power-tool backlog for Nooks & Parens, from the approved v4 prototype."
tags: [nooks-and-parens, design]
created: 2026-11-24
modified: 2026-11-24
ai_assisted: "Claude via Claude Desktop (Claude Design)"
session: unrecorded
tools: unrecorded
agents_skills: []
review_maturity: L1 (Human-directed)
review_note: "Drafted in Claude Design at Jordan Miller's direction, 2026-11-24. The session and tools weren't recorded. Not yet reviewed."
---

# Design system

The approved look is `prototype/nooks-and-parens-v4.html` (Jordan, 2026-11-24: "I really like the general look and feel"). It reads as a cozy life-sim game's menus and screens, with a workshop and power-tool theme. **It needs a few more power-tool allusions**; see the backlog at the end.

Mood: a friendly shop in a cozy game. Wooden signs, cream dialog boxes, a pegboard tool wall, a toolbelt hotbar. Warm, rounded, readable. Never a copy of any real game's interface.

## Colour tokens

The Clojure colours lead. Warm neutrals were added for materials (paper, wood) because the cozy look doesn't work without them.

| Token | Hex | Role |
|---|---|---|
| `--green` | `#63b132` | Primary button, "open" chips, open tools on the wall |
| `--blue` | `#5881d8` | Name tabs, shop sign, recipe headers, locked tools |
| `--sky` | `#90b4fe` | Page background, hover/selected states |
| `--white` | `#ffffff` | Cards, menu rows |
| `--black` | `#000000` | Text, borders, button outlines |
| `--paper` | `#fffaf0` | Panels and dialog boxes |
| `--paper-2` | `#f3e6cc` | Inline code, soft chips |
| `--tan` | `#e9d3b0` | Panel borders, pegboard |
| `--wood` | `#8a5a32` | Signs, workbench top, toolbelt |
| `--wood-dark` | `#6e4424` | Sign borders, ropes, peg holes |
| `--muted` | `#5a5a52` | Secondary text |

### Checked text pairs (WCAG AA needs 4.5:1 for body text)

| Text on background | Ratio |
|---|---|
| black on paper / white / paper-2 / tan | 20.2 / 21 / 17.0 / 14.4 |
| black on sky | 10.2 |
| black on green | 7.9 |
| black on blue | 5.6 |
| paper on wood | 5.6 |
| paper on wood-dark | 8.0 |
| muted on paper / white / paper-2 | 6.7 / 7.0 / 5.6 |
| sky on black (code prompt) | 10.2 |
| `#b6e39a` on black (code strings) | 14.4 |
| `#bdbdb5` on black (code comments) | 11.1 |

**Failing pairs; never use for text:** white on blue (3.8), white on green (2.7), blue on white (3.8), green on anything light. Text on a colour is black; coloured text only on black.

Avoid colour pairings that read as a real tool brand: no yellow-and-black, no red-and-black, no teal-and-black, and no lime-green-and-black on tool-shaped objects.

## Type

- UI and body: **Nunito** (400, 700, 900), falling back to `ui-rounded, "SF Pro Rounded", "Trebuchet MS", system-ui, sans-serif`. Self-host it.
- Code and data: **IBM Plex Mono** (400, 600), falling back to `ui-monospace, Menlo, Consolas, monospace`. Self-host it.
- Never use a game's own font, or a font made to imitate one.
- Body 16px / 1.55. Headings 900 weight: h1 30px, h2 22px, h3 17px.

## Shape and depth

- Panels: 22px radius, 3px `--tan` border, a flat drop shadow `0 6px 0 rgba(0,0,0,.12)`.
- Buttons and chips: fully rounded, 3px black border; the primary button has a solid black bottom shadow that compresses when pressed.
- Wood: flat `--wood` with faint grain lines (repeating gradient), 4px `--wood-dark` border.

## Components

| Component | Where | Notes |
|---|---|---|
| **Sign header** | Every page | Hanging wooden sign: name + tagline. Links home. Language switch beneath |
| **Dialog box** | Landing | Cream bubble, blue name tab ("Nooks & Parens"), positioning line first, a ▼ hint |
| **Main menu** | Landing | Numbered rows `[1] Start here [2] Today [3] Calendar [4] Workbench` with short descriptions |
| **Primary button** | One per page | Green pill, black text and outline |
| **Chip** | Status | Always words: "Solutions open", "Solutions locked", "Not out yet", "Live today". Distinct borders (solid, plain, dashed) as well as colour |
| **Data readout** | Landing, Workbench | Black panel showing a REPL result as Clojure data |
| **Shop sign + shop list** | "What're ya REPLin'?" | Blue sign heading; rows with a tool icon, session, covers-day link, and a right-hand "price": date, "Live today" or "Replay ready" |
| **Tool wall** | Calendar | Pegboard in a wooden frame; 12 slots. Not out = dashed outline; locked = blue tool; open = green tool; today = ring. Words in every slot |
| **Pinned note** | Day page | White note with a pin: unlock time, puzzle link, session, guest, gate date |
| **Gate sign** | Day page | Hanging wooden sign: "Closed" + the date in words; "Open" in paper when solutions are out |
| **Recipe card** | DIY recipes | Blue "DIY recipe" header, title, ingredients (functions), footer links: the how-to, and "Try it at the Workbench" |
| **Workbench** | `/workbench/` | Wood top bar with evaluator status; code textarea; "Craft it: run code", "Clear code" and (while running) "Stop running code" buttons; result slot with a kind label; "Load: <title>" starter recipe buttons |
| **Toolbelt** | Every page, fixed bottom | 8 slots with key numbers 1–8; current page highlighted; shortcut on/off switch |

## Motion

Minimal: a 2px lift on hover for tool slots and buttons; the button press. All transforms off under `prefers-reduced-motion`.

## Writing for components

- Sentence case everywhere except the decided labels.
- Status text says what and when: "Locked until Thu 10 Dec", not "Locked".
- Use the decided labels exactly. Everything else is a `draft` placeholder until Jordan writes it.

## Power-tool backlog (pick with Jordan in P2)

Jordan wants a few more allusions to power tools. Each keeps the meaning in words.

| Idea | Where | What it shows |
|---|---|---|
| **Lockout tag** for the spoiler gate | Day page | A tag on a padlock hasp: "DO NOT OPERATE · Solutions locked until Thu 10 Dec, 05:00 UTC". Could replace the hanging "Closed" sign |
| **Tape-measure timeline** | Landing, calendar | December as a tape measure with ticks 1–12 and a marker at today; text label "Day 7 of 12" |
| **Battery meter** | Landing Today panel | "7 of 12 puzzles out" as cordless-tool battery bars, with the number in text |
| **Hang tags** | Status chips | Chips shaped like hardware-store price tags with a hole |
| **Power LED** | Workbench | A small green LED plus "Evaluator ready" / "Offline preview" text |
| **Trigger button** | Workbench | "Craft it" styled like a tool trigger; still a normal button |
| **Toolbox drawers** | Learn | Four drawers: tutorials, how-to guides, reference, why it works, with item counts |
| **Owner's manual** | Start here | Parts list (Java, Clojure CLI, babashka, VS Code + Calva), numbered assembly steps per OS, a troubleshooting table |
| **Rating plate** | Video pages | A metal spec plate: "Model S-07 · Runtime [length] · Captions EN / PT-BR" |
| **Safety-glasses notice** | Above spoilers | "Eye protection: spoilers below" on open day pages |
| **Hazard-stripe divider** | Footer | Blue-and-black or wood-and-paper stripes, never yellow-and-black |
