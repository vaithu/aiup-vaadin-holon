# Design.md — Acme CRM/ERP Visual System

> The complete visual specification for the Acme CRM/ERP design language. Companion
> to `vision.md` (project overview) and `AGENTS.md` (build conventions). This document
> is the **design source of truth** — when a design question comes up, the answer
> is here.

**Design version**: Acme CRM v3.4
**Last reviewed**: 2026-08-30
**Maintainer**: Mavis (MiniMax-M3)

---

## Table of contents

1. [Design philosophy](#1-design-philosophy)
2. [Color system](#2-color-system)
3. [Typography](#3-typography)
4. [Spacing & layout grid](#4-spacing--layout-grid)
5. [Elevation, shadows, borders](#5-elevation-shadows-borders)
6. [Iconography](#6-iconography)
7. [The 5-cell strip](#7-the-5-cell-strip)
8. [Card patterns](#8-card-patterns)
9. [Donut & chart patterns](#9-donut--chart-patterns)
10. [Data tables](#10-data-tables)
11. [Forms & inputs](#11-forms--inputs)
12. [Status pills & tags](#12-status-pills--tags)
13. [The master list](#13-the-master-list)
14. [The detail header](#14-the-detail-header)
15. [Tab strip](#15-tab-strip)
16. [The reverse flow card (6 steps)](#16-the-reverse-flow-card-6-steps)
17. [The approval flow card (4 steps)](#17-the-approval-flow-card-4-steps)
18. [The 3-yr portfolio annotation](#18-the-3-yr-portfolio-annotation)
19. [Mobile patterns](#19-mobile-patterns)
20. [Motion & micro-interactions](#20-motion--micro-interactions)
21. [Empty states, errors, loading](#21-empty-states-errors-loading)
22. [Voice & copy](#22-voice--copy)
23. [Accessibility](#23-accessibility)
24. [Anti-patterns](#24-anti-patterns)

---

## 1. Design philosophy

**Three principles, in order of priority:**

1. **Information density, not whitespace.** Industrial automation buyers want to see
   the 5 key numbers in 2 seconds. Don't waste a 1440px canvas on a 600px hero with
   one number. The dark 5-cell strip always leads. Cards always pack 4-8 facts.
2. **Color = meaning, not decoration.** Every color choice encodes a domain concept
   (gold = out, teal = in, red = reverse). A gold card *means* money is leaving.
   Never decorative.
3. **Concrete over abstract.** "94% on-time" beats "Reliable vendor". "€48K saved via
   early-pay discount" beats "Cost-effective". The user is a buyer; they want numbers.

**Voice**: quiet, professional, B2B. No exclamation marks. No emoji. No marketing
puffery. The 3-yr portfolio annotation is the closest we get to a sales pitch.

**Inspiration**: Vaadin Lumo, Stripe Dashboard, Linear, Attio. The reference mockup
(`/workspace/attachments/b8cfe10d__*.html`) is the visual north star.

---

## 2. Color system

### 2.1 Brand palette (CSS custom properties)

```css
:root {
  /* Surfaces */
  --bg:         #f3f5f8;   /* page background (cool gray) */
  --surface:    #ffffff;   /* card / panel background */
  --surface-2:  #f8fafc;   /* nested surface (table head, input bg) */

  /* Lines */
  --line:       #e4e9f0;   /* primary border */
  --line-soft:  #eef1f6;   /* secondary border (row divider) */

  /* Text */
  --text:       #0f1b2d;   /* primary text (near-black, navy tint) */
  --text-mute:  #5b6878;   /* secondary text */
  --text-dim:   #8a96a6;   /* tertiary text / placeholders */

  /* Brand */
  --primary:    #1576d3;   /* blue — actions, links, primary */
  --primary-2:  #0a5fb8;   /* blue gradient end */
  --primary-soft: #e6f1fb; /* blue tint (selected row, info chip) */

  /* Semantic */
  --success:    #2e9a6a;   /* green — OK / paid / settled */
  --success-soft: #e6f5ee;
  --warn:       #b8860b;   /* amber — watch / partial / pending */
  --warn-soft:  #fbf3dc;
  --danger:     #c0392b;   /* red — failure / reverse / damage */
  --danger-soft: #fbe7e4;

  /* Domain-meaning colors */
  --gold:       #d4a017;   /* MONEY OUT — payments, refunds, expenses */
  --gold-soft:  #fdf3d1;
  --gold-dark:  #a87c1e;   /* gradient end (deeper for cards) */
  --teal:       #0d9488;   /* MONEY IN / STOCK — receipts, builds, vendors */
  --teal-soft:  #d9f5f1;
  --teal-dark:  #0a7268;
  --violet:     #6f7afc;   /* CUSTOMER / RECURRING — customers, subs, projects */
  --violet-soft: #eceefd;
  --violet-dark: #4a55e0;
}
```

### 2.2 The meaning of color (the 8-color vocabulary)

| Token        | Meaning                                  | Use case                                         |
|--------------|------------------------------------------|--------------------------------------------------|
| `--primary`  | Default / brand                          | Buttons, links, headers, default actions         |
| `--gold`     | Money **out**                            | Payments made, refunds, expenses, bills (AP)     |
| `--teal`     | Money **in** / stock                     | Receipts, vendors, POs, builds, kits, payment-in |
| `--violet`   | Customer / recurring                     | Customers, subscriptions, projects, drafts       |
| `--danger`   | Reverse / destructive                    | Returns, QC failure, damage, credit              |
| `--success`  | OK / settled / paid                      | Success pills, paid status, OK                   |
| `--warn`     | Watch / partial                          | Partial fills, pending, due soon                 |
| `--text`     | Dark / identity                          | Hero cards, summary strips, identity             |

**Strict rules:**
- A refund is **gold** (money out), never red.
- A return is **red** (flow direction), never gold.
- A payment received is **teal** (money in), never green.
- Drafts are **violet**, never gray.
- Identity / hero cards are **navy/text** (`#0f1b2d`), not gray.

### 2.3 Tints vs. solids

Each domain color has a `-soft` variant for backgrounds:

```css
.gold-card-bg { background: var(--gold-soft); }   /* subtle, large areas */
.gold-chip    { background: var(--gold); color: white; }  /* strong, small areas */
```

Cards use **gradients** for hero treatment:
```css
background: linear-gradient(135deg, var(--gold), var(--gold-dark));
```

Plain text/values use **solid `-soft` backgrounds** with **solid color text**:
```css
background: var(--success-soft);
color: var(--success);
font-weight: 600;
```

### 2.4 Dark gradient (the strip)

The 5-cell strip and identity hero use a 3-stop navy gradient:

```css
background: linear-gradient(135deg, #0f1b2d 0%, #1a3554 50%, #0d3a5e 100%);
```

Plus a radial highlight in the top-right corner (always present, opacity 18%):

```css
.hero::after {
  content: '';
  position: absolute;
  top: -30%; right: -10%;
  width: 50%; height: 160%;
  background: radial-gradient(circle, rgba(21, 118, 211, 0.18), transparent 60%);
  pointer-events: none;
}
```

---

## 3. Typography

### 3.1 Font stack

| Use case    | Font              | Weights      | Source       |
|-------------|-------------------|--------------|--------------|
| Desktop UI  | **Outfit**        | 400/500/600/700 | Google Fonts |
| Mobile UI   | **Inter**         | 400/500/600/700 | Google Fonts |
| Mono / data | **JetBrains Mono**| 400/500      | Google Fonts |

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;500;600;700&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
```

### 3.2 Type scale

| Token          | px    | Weight | Use                              |
|----------------|-------|--------|----------------------------------|
| `.display`     | 22    | 600    | detail name (h1)                 |
| `.h1`          | 19–20 | 600    | page title, card header          |
| `.h2`          | 17    | 600    | section title                    |
| `.h3`          | 15–16 | 600    | card heading                     |
| `.body`        | 14    | 400    | default body                     |
| `.body-sm`     | 13    | 400    | secondary body                   |
| `.label`       | 12.5  | 500    | field label, list item           |
| `.label-sm`    | 11.5  | 500    | small label, cell label          |
| `.caption`     | 10.5  | 500    | caption, badge                   |
| `.mono-sm`     | 11.5  | 500    | mono small (PO-2026-0001)        |
| `.mono-md`     | 12.5  | 500    | mono medium (€7,580.00)          |
| `.mono-lg`     | 14.5  | 600    | mono large (header value)        |

### 3.3 Number formatting

Always use **`JetBrains Mono`** for numbers, currency, IDs, dates.
Always **right-align** numbers in tables.
Always use **non-breaking spaces** (`&nbsp;`) between value and unit: `€7,580&nbsp;·&nbsp;90%`.

### 3.4 Letter-spacing

```css
.detail-name { letter-spacing: -0.01em; }   /* tighter for headlines */
.tab-label   { letter-spacing: 0.02em; }    /* looser for nav */
.caps        { letter-spacing: 0.06em; text-transform: uppercase; }  /* section heads */
```

---

## 4. Spacing & layout grid

### 4.1 Base unit

**4px base unit.** All paddings, gaps, and margins are multiples of 4.

| Token   | px  | Use                                  |
|---------|-----|--------------------------------------|
| `--s1`  | 4   | tight inline (icon ↔ label)           |
| `--s2`  | 8   | cell padding                         |
| `--s3`  | 12  | card padding (small)                 |
| `--s4`  | 16  | card padding (default)               |
| `--s5`  | 20  | section gap                          |
| `--s6`  | 24  | page horizontal padding              |
| `--s7`  | 32  | hero padding                         |
| `--s8`  | 40  | large section gap                    |

### 4.2 Layout (desktop)

```
┌──────────┬──────────────────────────────────────────────────┐
│          │  Topbar  (54px)                                  │
│          ├──────────┬───────────────────────────────────────┤
│ Sidebar  │          │                                       │
│ 228px    │  Left     │  Right pane                           │
│          │  pane     │  (detail)                             │
│          │  240–340px│  1fr (max 980px)                      │
│          │          │                                       │
│          │          │  • 5-cell strip                       │
│          │          │  • Tab strip                          │
│          │          │  • Cards                              │
│          │          │  • 3-yr portfolio                     │
└──────────┴──────────┴───────────────────────────────────────┘
```

**Page max-width**: 1280px shell, then 100% with horizontal scroll.
**Body padding**: 26px sides, 18px top.

### 4.3 Layout (mobile)

```
┌────────────────────────────────────┐
│ Notch                              │
├────────────────────────────────────┤
│ Status bar    (44px)               │
├────────────────────────────────────┤
│ App bar       (54px, sticky)       │
├────────────────────────────────────┤
│ Detail head   (variable)           │
├────────────────────────────────────┤
│ Strip / donut (variable)           │
├────────────────────────────────────┤
│                                    │
│ Cards (scrollable)                 │
│  • 12px side padding               │
│  • 10px gap between cards          │
│                                    │
├────────────────────────────────────┤
│ Tab bar    (64px, fixed)           │
└────────────────────────────────────┘
```

**Frame**: iPhone 14 Pro 390×844, 8px bezel, 124px notch, 64px home indicator.
**Padding**: 12–16px sides on cards.

---

## 5. Elevation, shadows, borders

### 5.1 Shadows

```css
--shadow-sm: 0 1px 2px rgba(15, 27, 45, 0.04);
--shadow-md: 0 4px 14px -2px rgba(15, 27, 45, 0.06), 0 2px 4px rgba(15, 27, 45, 0.04);
--shadow-lg: 0 24px 60px -10px rgba(0, 0, 0, 0.4);  /* phone frame */
```

Use sparingly. Most depth comes from **borders**, not shadows.

### 5.2 Border-radius

| Element              | Radius  |
|----------------------|---------|
| Card                 | 10–12px |
| Card (small)         | 7–8px   |
| Pill / chip          | 999px   |
| Input                | 6–7px   |
| Button               | 6–7px   |
| Avatar / icon mark   | 50%     |
| Strip cell           | 0       |

### 5.3 Borders

```css
.card       { border: 1px solid var(--line); }
.card-body  { border-top: 1px solid var(--line-soft); }
```

**1px line, no inner shadows.** Cards = surface + line + sm shadow.

---

## 6. Iconography

### 6.1 Source

**Lucide-style stroke icons** (24×24, stroke 2, round caps). Inline SVG, no icon font.

```html
<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
     stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
  <polyline points="20 6 9 17 4 12"/>
</svg>
```

### 6.2 Sizes

| Context           | Size  |
|-------------------|-------|
| Inline label      | 11px  |
| Card heading icon | 13px  |
| Strip icon        | 14px  |
| Sidebar nav       | 14px  |
| Topbar button     | 13px  |
| Empty state hero  | 28px  |

### 6.3 Color

Icons inherit `color` from parent. For colored cards, use the matching **tint**:

```css
.gold-card .icon { color: rgba(255, 255, 255, 0.85); }
```

White-tinted on dark cards. Solid color on light cards.

---

## 7. The 5-cell strip

**The single most important pattern.** Every detail view starts with this.

### 7.1 Anatomy

```
┌────────────────────┬────────────────────┬────────────────────┬────────────────────┬────────────────────┐
│  ▣  Received       │  €  Value          │  ⛁  Vendor        │  ⤺  Backorder      │  ⏱  QC            │
│   180u /200u       │   €7,580 · 90%     │   TKH NV · MSA     │   20u · ETA +5d    │   Scheduled        │
└────────────────────┴────────────────────┴────────────────────┴────────────────────┴────────────────────┘
       1/5                  2/5                 3/5                 4/5                 5/5
```

Each cell:
- **Icon** in a 28×28 rounded square (`rgba(255,255,255,0.1)` background)
- **Label** in caps, 9.5px JetBrains Mono, 0.04em tracking, 60% white
- **Value** in 14.5px JetBrains Mono, white, line-height 1.1
- **Small** secondary value (optional) in 10.5px JetBrains Mono, 60% white
- **Tint** for value: gold / ok / teal / warn — 1–2 cells max

### 7.2 Container

```css
.strip {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  background: linear-gradient(135deg, #0f1b2d 0%, #1a3554 50%, #0d3a5e 100%);
  border-bottom: 1px solid var(--line);
}
.strip-cell {
  padding: 13px 16px;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  display: flex; align-items: center; gap: 10px;
  color: #fff;
}
.strip-cell:last-child { border-right: 0; }
```

### 7.3 Cell content rules

- **Always 5 cells**. Never 4, never 6. The constraint creates consistency.
- **Pick by priority**: total → status → counter → next-action → meta.
- **Use small text for context** (`.small`): "of 200u", "+5d", "Helix MSA".
- **Highlight 1 cell in gold** (the money direction) — or 0 if the entity isn't monetary.

### 7.4 Mobile variant

On mobile, the strip becomes **3 cells wide** (full-width, single row) and the other 2
facts move into a card. Or, for less critical entities, **2 cells** + a meta card.

```css
.m-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: linear-gradient(135deg, #0f1b2d, #1a3554);
}
```

---

## 8. Card patterns

### 8.1 Card anatomy

```css
.card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(15, 27, 45, 0.04);
}
.card-h {
  padding: 11px 16px;
  border-bottom: 1px solid var(--line-soft);
  display: flex; justify-content: space-between; align-items: center;
}
.card-h h3 { font: 600 13.5px 'Outfit'; display: flex; align-items: center; gap: 7px; }
.card-b { padding: 14px 16px; }
```

### 8.2 Card variants

#### Plain card (default)
White surface, neutral border. For facts that don't have domain meaning.
```css
background: var(--surface);
border: 1px solid var(--line);
```

#### Domain card (gold / teal / violet / red)
Gradient background, white text. For "this thing IS gold/teal/etc."
```css
.gold-card { background: linear-gradient(135deg, var(--gold), var(--gold-dark)); color: #fff; }
.teal-card { background: linear-gradient(135deg, var(--teal), var(--teal-dark)); color: #fff; }
.violet-card { background: linear-gradient(135deg, var(--violet), var(--violet-dark)); color: #fff; }
.danger-card { background: linear-gradient(135deg, var(--danger), #7a1f1a); color: #fff; }
.danger-card-gold { /* mixed red base + gold highlight for refunds */
  background: linear-gradient(135deg, #7a1f1a, #4a1212);
}
.danger-card-gold::after { /* gold highlight for refund endpoint */
  content: ''; position: absolute; top: -20%; right: -10%;
  width: 45%; height: 140%;
  background: radial-gradient(circle, rgba(212, 160, 23, 0.18), transparent 60%);
}
```

#### Dark hero card (navy gradient)
For identity, summary, or "this is the entity itself."
```css
background: linear-gradient(135deg, #0f1b2d 0%, #1a3554 50%, #0d3a5e 100%);
color: #fff;
position: relative; overflow: hidden;
```
Plus a radial highlight in the top-right.

#### Soft card (subtle tint)
For lists of items within a section. The fact is gold/teal/violet but you want
a calmer visual rhythm than a gradient.
```css
background: var(--gold-soft);
color: var(--gold-dark);
font-weight: 600;
```

### 8.3 Card grid

Use a 2-column grid for paired facts (e.g. shipping + payment, source + status):

```css
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; }
```

### 8.4 Card content rules

- **Header**: 1 line, max 4 words. Subline in `text-mute`, 11.5px Inter.
- **Body**: 4–8 fields OR a table OR a chart. Never a single field (use a row instead).
- **Sub-card pattern**: 4-cell mini-strip inside a card for related facts.
  Used heavily in the PO link, the bank card, the totals preview.

---

## 9. Donut & chart patterns

### 9.1 The donut

Used for **progress, splits, completion**. Center shows primary value.

```html
<div class="donut">
  <svg viewBox="0 0 120 120">
    <circle cx="60" cy="60" r="50" stroke="rgba(255,255,255,0.18)" stroke-width="14" fill="none"/>
    <circle cx="60" cy="60" r="50" stroke="#86efac" stroke-width="14"
            stroke-dasharray="283 314" stroke-linecap="round" transform="rotate(-90 60 60)"/>
  </svg>
  <div class="ctr">90%<br><span>180 / 200u</span></div>
</div>
```

**Stroke-dasharray formula**: `dasharray = (percent * circumference) (circumference - percent * circumference)`

For multi-color donuts (e.g. received + backorder + QC), stack 3 circles with offset:

```html
<circle stroke-dasharray="215 239" stroke-dashoffset="0"/>      <!-- 90% green -->
<circle stroke-dasharray="24 239"  stroke-dashoffset="-215"/>    <!-- 10% amber -->
<circle stroke-dasharray="11 239"  stroke-dashoffset="-239"/>    <!-- 4% red -->
```

### 9.2 Donut size

| Context       | Size  | Stroke | Center value |
|---------------|-------|--------|--------------|
| Hero / strip  | 120px | 14     | 26px         |
| Card hero     | 90px  | 10     | 19px         |
| Compact       | 60px  | 7      | 13px         |

### 9.3 Bar charts

Used for **time series, completion, status breakdown**. Horizontal stacked bars
in the `m-bar` family:

```css
.m-bar { height: 4px; background: var(--line-soft); border-radius: 2px; overflow: hidden; }
.m-bar-fill { height: 100%; border-radius: 2px; background: var(--success); }
```

For larger visualizations, use 8-color stacked bars (10–20px tall) with
labels above each segment.

### 9.4 Variant matrix (entity 32)

A 24-cell matrix (size × color × material axes) with **ok / warn / out tints**:

```css
.variant-cell { background: var(--surface-2); border: 1px solid var(--line-soft); }
.variant-cell.ok   { background: var(--success-soft); }
.variant-cell.warn { background: var(--warn-soft); }
.variant-cell.out  { background: var(--danger-soft); color: var(--text-dim); }
```

---

## 10. Data tables

### 10.1 Anatomy

```html
<table class="lines">
  <thead>
    <tr><th>SKU</th><th>Description</th><th>Ordered</th><th>Received</th><th>Cost</th></tr>
  </thead>
  <tbody>
    <tr><td class="sku">MOT-150W</td><td>Servo motor</td><td class="num">200u</td><td class="num">180u</td><td class="num">€42.10</td></tr>
  </tbody>
  <tfoot>
    <tr><td colspan="4" style="text-align:right">Total</td><td class="num">€7,578</td></tr>
  </tfoot>
</table>
```

### 10.2 Rules

- **Header**: 10.5px JetBrains Mono, uppercase, 0.04em tracking, `--text-mute`.
  Background `--surface-2`. Bottom border `--line`.
- **Body cells**: 12.5px Inter. Padding 10px. Border-bottom `--line-soft`.
- **Numeric cells**: JetBrains Mono, right-aligned. Use color tint for status
  (`var(--success)` for OK, `var(--warn)` for partial, `var(--danger)` for fail).
- **Hover**: `--surface-2` row background.
- **Footer**: 12px Outfit bold, `--surface-2` background.
- **No vertical borders.** Only horizontal lines.

### 10.3 Special cell types

| Cell class | Style |
|------------|-------|
| `.sku` | `font: 500 11.5px 'JetBrains Mono'; color: var(--primary);` |
| `.num` | right-aligned, mono |
| `.num.ok` | right-aligned, mono, `color: var(--success); font-weight: 600;` |
| `.num.warn` | right-aligned, mono, `color: var(--warn);` |
| `.num.primary` | right-aligned, mono, `color: var(--primary); font-weight: 600;` |

### 10.4 Status pills in tables

```css
.status-pill {
  font: 500 9.5px 'JetBrains Mono';
  padding: 2px 6px;
  border-radius: 3px;
  display: inline-block;
}
.status-pill.ok     { background: var(--success-soft); color: var(--success); }
.status-pill.warn   { background: var(--warn-soft); color: var(--warn); }
.status-pill.danger { background: var(--danger-soft); color: var(--danger); }
.status-pill.pen    { background: var(--surface-2); color: var(--text-mute); }
```

---

## 11. Forms & inputs

### 11.1 Field anatomy

```html
<div class="field">
  <label class="lbl">Field label <span class="req">*</span></label>
  <input class="input" value="..."/>
</div>
```

```css
.lbl { font: 500 11px 'Inter'; color: var(--text-mute); display: flex; align-items: center; gap: 3px; }
.lbl .req { color: var(--danger); margin-left: 1px; }
.input, .select, .textarea {
  padding: 7px 11px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--surface);
  color: var(--text);
  font: 400 13px 'Outfit';
  outline: none;
  width: 100%;
  height: 34px;
}
.input.mono { font-family: 'JetBrains Mono', monospace; font-size: 12px; }
.input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-soft); }
```

### 11.2 Select dropdown chevron

```css
.select {
  appearance: none;
  background-image: url("data:image/svg+xml;utf8,<svg ...><polyline points='6 9 12 15 18 9'/></svg>");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 28px;
}
```

### 11.3 Color picker (entity 38 wizard)

8 swatches in a row, 30–34px squares, 2px border that turns solid on selection:

```css
.color-opt { width: 30px; height: 30px; border-radius: 6px; cursor: pointer; border: 2px solid var(--line); }
.color-opt.on { border-color: var(--text); box-shadow: 0 0 0 3px var(--primary-soft); }
.color-opt.on::after { content: '✓'; position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 700; }
```

### 11.4 Preset cards (regional preset picker)

Used in onboarding wizard. 2x2 grid of full-width cards with on-card highlight.

```css
.preset { background: var(--surface-2); border: 2px solid var(--line); border-radius: 7px; padding: 9px 11px; }
.preset.on { border-color: var(--primary); background: var(--primary-soft); box-shadow: 0 0 0 3px var(--primary-soft); }
```

---

## 12. Status pills & tags

### 12.1 Pill anatomy

```html
<span class="pill warn">Partial · 90%</span>
```

```css
.pill {
  font: 600 10px 'Outfit';
  padding: 2.5px 8px;
  border-radius: 3px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.pill::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}
```

### 12.2 Variants

| Variant     | Background           | Color             | Use                                |
|-------------|----------------------|-------------------|------------------------------------|
| `.ok`       | `--success-soft`     | `--success`       | Settled, paid, complete, healthy   |
| `.warn`     | `--warn-soft`        | `--warn`          | Partial, pending, due soon         |
| `.danger`   | `--danger-soft`      | `--danger`        | Failed, damaged, void              |
| `.pri`      | `--primary-soft`     | `--primary`       | Default / in-progress / linked     |
| `.vio`      | `--violet-soft`      | `--violet`        | Customer-facing / draft / new      |
| `.gold`     | `--gold-soft`        | `--gold`          | Money-out, refund, payment         |
| `.neu`      | `--surface-2`        | `--text-mute`     | Inactive, archived, secondary      |

### 12.3 Pill dot

Every pill has a leading **5px dot** (except `.neu` and `.pri` sometimes). The dot
color matches the pill text. It signals "live state" at a glance.

### 12.4 Pill size

- Default: `10px / 8px padding / 3px radius`
- Detail head: `10.5px / 8px padding / 4px radius`
- Mobile compact: `9.5px / 7px padding / 3px radius`

---

## 13. The master list

The left-pane list of records. Common across most entities.

### 13.1 Anatomy

```
┌──────────────────────────────────────┐
│  Customers · 248                  + │  ← header w/ count + new
├──────────────────────────────────────┤
│  🔍 Search                            │
├──────────────────────────────────────┤
│  All · Active · Draft · Archived     │  ← filter tabs
├──────────────────────────────────────┤
│  ACM-2024-0089        today          │  ← row top
│  Helix Systems AG                    │  ← row name
│  €48,200 MSA            ● Active     │  ← row bot: amount + pill
├──────────────────────────────────────┤
│  ...                                  │
└──────────────────────────────────────┘
```

### 13.2 Row anatomy

```css
.list-row {
  padding: 11px 16px;
  border-bottom: 1px solid var(--line-soft);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 5px;
  position: relative;
}
.list-row:hover { background: var(--surface-2); }
.list-row.on { background: var(--primary-soft); }
.list-row.on::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: var(--primary); }
```

### 13.3 Row content

- **Top line**: Record number (mono, primary) + relative date (mono, dim)
- **Middle line**: Record name (Inter, text)
- **Bottom line**: Amount / count (mono, text) + status pill (right)

### 13.4 Tab filters

```css
.tab { font: 500 12px 'Outfit'; padding: 7px 11px; color: var(--text-mute); cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tab.on { color: var(--primary); font-weight: 600; border-bottom-color: var(--primary); }
.tab .num { font: 500 10px 'JetBrains Mono'; opacity: 0.7; margin-left: 3px; }
```

Tab counts in mono. Active tab has primary color + primary underline.

---

## 14. The detail header

```html
<div class="detail-head">
  <div class="dh-num">REC-2026-08-0427 · PO-2026-0842 · 1 line · 180 of 200u received</div>
  <h1 class="dh-name">TKH Group NV · 200u MOT-150W partial receive</h1>
  <div class="dh-tags">
    <span class="pill warn">Partial · 90%</span>
    <span class="pill pri">QC pending</span>
    <span class="pill vio">3-yr MSA</span>
    <span class="pill gold">Helix MSA</span>
    <span class="sep">·</span>
    <span>Created 27 Aug 2026 09:14 by M. Bauer</span>
  </div>
</div>
```

### 14.1 Rules

- **Number line** (top): full breadcrumb-style identifier with key facts in mono.
  Includes any cross-references (PO #, customer, vendor).
- **Name line** (middle): human-readable name, 22px, 600 weight, -0.01em tracking.
  Goes to two lines if needed; never truncate.
- **Tags line** (bottom): 3–5 pills, then a separator dot, then metadata
  ("Created / Modified by").

### 14.2 Actions (top-right)

```html
<div class="topbar-r">
  <div class="btn">Back</div>
  <div class="btn">Print</div>
  <div class="btn danger">Reject</div>
  <div class="btn primary">Confirm &amp; post</div>
</div>
```

Always 2–4 actions. Primary action is the most common next step. Destructive
actions use `--danger` background. Secondary actions are neutral.

---

## 15. Tab strip

The static visual tab strip below the strip, used to organize detail content.

```html
<div class="t-tabs">
  <div class="t-tab on">Overview</div>
  <div class="t-tab">Lines <span class="num">1</span></div>
  <div class="t-tab">Variance</div>
  <div class="t-tab">Source PO</div>
  <div class="t-tab">Notes</div>
</div>
```

```css
.t-tab { padding: 8px 14px; font: 500 12.5px 'Outfit'; color: var(--text-mute); cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.t-tab.on { color: var(--primary); font-weight: 600; border-bottom-color: var(--primary); }
.t-tab .num { font: 500 10.5px 'JetBrains Mono'; color: var(--text-dim); background: var(--surface-2); padding: 1px 5px; border-radius: 3px; }
.t-tab.on .num { background: var(--primary-soft); color: var(--primary); }
```

**Always static.** No JavaScript tab switching in mockups. Each tab is a layout
section, not a content panel that swaps.

### 15.1 Which tabs to use

| Tab         | When                                          |
|-------------|-----------------------------------------------|
| Overview    | Always (default)                              |
| Lines       | When the entity has 1+ line items             |
| Variance    | When receive/quantity can differ from order   |
| Source      | When the entity links to a predecessor doc    |
| Put-away    | When the entity involves physical location    |
| Notes       | Always (last position)                        |
| History     | When there's an audit trail                   |

**5–7 tabs max.** Fewer is better.

---

## 16. The reverse flow card (6 steps)

The signature pattern for **returns** (sales return, purchase return, RMA).

### 16.1 Sales return flow

```
SO  →  Return req  →  RMA  →  Receive  →  Inspect  →  Refund
```

### 16.2 Purchase return flow

```
QC  →  RMA  →  Credit note  →  Ship  →  Receive  →  Refund
```

### 16.3 Visual treatment

```html
<div class="reverse-flow danger">
  <div class="rf-step done"><div class="rf-icon">SO</div><div class="rf-lbl">Sales order</div><div class="rf-d">14 Aug</div></div>
  <div class="rf-step done"><div class="rf-icon">RR</div><div class="rf-lbl">Return req</div><div class="rf-d">18 Aug</div></div>
  <div class="rf-step done"><div class="rf-icon">RMA</div><div class="rf-lbl">RMA</div><div class="rf-d">20 Aug</div></div>
  <div class="rf-step active"><div class="rf-icon">REC</div><div class="rf-lbl">Receive</div><div class="rf-d">22 Aug</div></div>
  <div class="rf-step"><div class="rf-icon">QC</div><div class="rf-lbl">Inspect</div><div class="rf-d">—</div></div>
  <div class="rf-step"><div class="rf-icon gold">RF</div><div class="rf-lbl">Refund</div><div class="rf-d">—</div></div>
</div>
```

### 16.4 Step states

- **done**: solid red bg, white text, white checkmark icon
- **active**: pulsing red bg, white text, current step indicator
- **pending**: light gray bg, dim text, dashed connector
- **final** (refund): gold bg, white text, special endpoint treatment

### 16.5 Color treatment

- Steps 1–5: **red gradient** (`#c0392b → #7a1f1a`)
- Step 6 (refund): **gold gradient** (`#d4a017 → #a87c1e`)
- Card background: light red tint (`#fbe7e4`)
- Connector lines: dashed red on pending, solid red on done

### 16.6 Why red + gold combo

The flow is **red** because it's destructive (returning goods). The endpoint
is **gold** because money is going out (refund). The combination reads as
"this is a problem being resolved with a payment."

---

## 17. The approval flow card (4 steps)

Used for **expense reports, bill approval, journal review**.

```
Submitter  →  Manager  →  Finance  →  Controller
```

### 17.1 Visual treatment

- All 4 steps: **violet gradient** (`#6f7afc → #4a55e0`)
- Card background: light violet tint
- Same done / active / pending state machine as reverse flow
- Each step has a name + role pill (e.g. "E. Lindqvist · AP Lead")

### 17.2 When used

- **Expenses** (entity 33): E. Lindqvist → M. Hofmann → S. Vasquez → SEPA
- **Bills** (entity 19): AP clerk → AP lead → controller → CFO if > €50K
- **Journal entries** (entity 22, large amounts): preparer → controller → CFO → auditor

---

## 18. The 3-yr portfolio annotation

**Every detail view ends with one.** This is the trust signal.

### 18.1 Format

```html
<div class="cv-anno">
  3-yr receive portfolio: <b>96 receives</b> · <b>94% on-time</b> · <b>2.4% damage rate</b> · <span class="save">€48K saved</span> via early QC reject · <b>Helix MSA</b> 96% fill rate
</div>
```

### 18.2 Rules

- **Single line**, max ~140 characters.
- **Always starts with "3-yr"** + the noun (portfolio, history, record, summary).
- **3–6 concrete facts**, each with a number or named entity.
- **Highlight one fact in green** (`var(--success)`) for the positive surprise
  ("€48K saved", "99.2% on-time").
- **Use a named MSA / contract** at the end to anchor identity ("Helix MSA").

### 18.3 Examples by entity

| Entity    | Annotation                                                              |
|-----------|-------------------------------------------------------------------------|
| Vendor    | "3-yr spend: €8.4M · 96 receives · 94% on-time · €48K saved · Helix MSA 96% fill" |
| Customer  | "3-yr contract: €1.2M · 14 invoices · 99% paid · Helix MSA 3-yr · 248 POs" |
| Invoice   | "3-yr customer: €1.4M billed · 99% paid · 0 disputes · Helix MSA"      |
| Receive   | "3-yr receive: 96 receives · 94% on-time · 2.4% damage · €48K saved"   |
| Bill      | "3-yr vendor: €680K billed · 99% paid · 14d avg cycle · Helix MSA"     |
| Payment   | "3-yr batches: 184 runs · €8.4M paid · €84K saved · 99.2% on-time"     |
| Expense   | "3-yr reports: 142 filed · 99% approved · 6d avg cycle · 4d reimbursement" |
| QC        | "3-yr inspections: 248 runs · 4.2% fail · 96% AQL · €8K saved via early catch" |

---

## 19. Mobile patterns

### 19.1 Frame

iPhone 14 Pro: 390×844, 8px black bezel, 124×30 notch, 64px tab bar.
The frame is rendered on a dark background (`.stage { background: #1a1d22 }`)
so the bezel looks correct.

### 19.2 Status bar (mockup only)

```html
<div class="status-bar">
  <div class="left">9:41</div>
  <div class="right"><svg>...signal</svg>...<svg>...battery</svg></div>
</div>
```

44px height, JetBrains Mono / system.

### 19.3 App bar

54px sticky, surface background, with back button + title + action icon.

```html
<div class="m-appbar">
  <div class="m-back">←</div>
  <div class="m-title">Title</div>
  <div class="m-icon">⋯</div>
</div>
```

### 19.4 Tab bar (bottom)

64px fixed, glass-blur background, 5 tabs.

```html
<div class="tabbar">
  <div class="tab on">Home</div>
  <div class="tab">CRM</div>
  <div class="tab">Inv</div>
  <div class="tab">Settings</div>
  <div class="tab">Me</div>
</div>
```

Active tab: `--primary` color + 700 weight label.

### 19.5 Mobile card

```css
.m-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 12px;
  margin: 0 12px 10px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(15, 27, 45, 0.03);
}
.m-card-h { padding: 10px 14px; border-bottom: 1px solid var(--line-soft); font-size: 13px; font-weight: 600; display: flex; align-items: center; justify-content: space-between; }
.m-card-body { padding: 12px 14px; display: flex; flex-direction: column; gap: 9px; }
```

### 19.6 Mobile strip (3 cells)

The 5-cell desktop strip becomes a 3-cell mobile strip. The other 2 facts move
into a card below.

```css
.m-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  background: linear-gradient(135deg, #0f1b2d, #1a3554);
  border-bottom: 1px solid var(--line);
}
.m-strip-cell {
  padding: 10px 12px;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  color: #fff;
  display: flex; align-items: center; gap: 7px;
}
```

### 19.7 Mobile donut (90px)

Smaller than desktop (90px vs 120px), thinner stroke (10 vs 14).

### 19.8 Mobile list cards

List rows become full-width cards. Show 5–7 cards per screen.

```html
<div class="m-card on">
  <div class="m-row"><div class="m-num">REC-2026-08-0427</div><div class="m-when">today</div></div>
  <div class="m-mid"><div class="m-vendor">TKH Group NV</div><div class="m-amt">€7,580</div></div>
  <div class="m-pills"><span class="m-tag warn">Partial · 90%</span></div>
  <div class="m-bar"><div class="m-bar-fill warn" style="width:90%"></div></div>
</div>
```

### 19.9 Mobile fonts

**Inter** instead of Outfit. Same weight scale. Mono is **JetBrains Mono**.

```css
html, body { font-family: 'Inter', system-ui, sans-serif; }
```

### 19.10 Mobile form

Stacked single-column fields. `input` height 34px. Full-width inputs. No
multi-column on mobile.

### 19.11 On-card highlight (active state)

```css
.m-card.on {
  border-color: var(--primary);
  background: var(--primary-soft);
  box-shadow: 0 2px 6px -1px rgba(21, 118, 211, 0.2);
}
.m-card.on .m-num,
.m-card.on .m-amt { color: var(--primary); }
```

The selected card has primary border, primary-soft bg, and primary text on
key elements. Always show at least one selected card in the list to demo the
master-detail flow.

---

## 20. Motion & micro-interactions

### 20.1 Static mockups have no JS

All animations are CSS-only. No JS-driven tab switches, no JS-driven expansions,
no JS-driven graphs. The mockup is a **picture of the app**, not the app itself.

### 20.2 Hover states (CSS only)

```css
button:hover { background: var(--surface-2); }
.list-row:hover { background: var(--surface-2); }
.tab:hover { color: var(--text); }
```

Subtle, never animated. Hover is a 5% darken or 5% lighten.

### 20.3 Focus rings

```css
.input:focus, .select:focus, .textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-soft);
}
```

3px primary-soft ring + 1px primary border. Always 3px, never thicker.

### 20.4 Page entry (the production app)

For the production Vaadin app (not mockups), use a 180ms fade-in on cards:

```css
@keyframes card-in {
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0); }
}
.card { animation: card-in 180ms ease-out; }
```

Strip uses a 240ms slide-in from the left. Donut uses a 600ms stroke-dasharray animation.
No bouncy easing — `ease-out` only.

---

## 21. Empty states, errors, loading

### 21.1 Empty state

```html
<div class="empty">
  <svg width="48" height="48"><!-- icon --></svg>
  <h3>No invoices yet</h3>
  <p>Create your first invoice to start tracking receivables.</p>
  <button class="btn primary">+ New invoice</button>
</div>
```

Center-aligned, 48px icon in `--text-dim`, h3 in `--text`, body in `--text-mute`,
primary action button.

### 21.2 Loading state (skeleton)

```css
.skeleton {
  background: linear-gradient(90deg, var(--line-soft) 25%, var(--surface-2) 50%, var(--line-soft) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s infinite;
  border-radius: 4px;
}
@keyframes skeleton-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
```

### 21.3 Error state

Red banner at top of form, with retry button. Never a modal dialog.

```html
<div class="error-banner">
  <svg>!</svg>
  <span>Couldn't save invoice. PO-2026-0842 has 3 unmatched lines.</span>
  <button>Retry</button>
</div>
```

```css
.error-banner { background: var(--danger-soft); border: 1px solid var(--danger); color: var(--danger); padding: 9px 13px; border-radius: 6px; display: flex; align-items: center; gap: 9px; }
```

---

## 22. Voice & copy

### 22.1 Principles

- **Direct, not chatty.** "Confirm & post" not "Are you sure you want to post this?"
- **Specific, not vague.** "184 SEPA batches, 99.2% on-time" not "Reliable payment history"
- **No exclamation marks.** Even in success messages.
- **No emoji.** Anywhere in the UI.
- **No marketing words.** "Powerful", "seamless", "robust", "best-in-class" — banned.
- **Sentence case** for buttons: "Save & post", not "Save & Post".
- **Title case** for headings: "Purchase order", "Sales pipeline".

### 22.2 Microcopy examples

| Context           | ❌ Don't write                | ✅ Write                       |
|-------------------|------------------------------|--------------------------------|
| Empty list        | "Whoops, nothing here!"      | "No purchase orders yet"        |
| Confirm dialog    | "Are you sure?"              | "Post 8 bills, €86,420 wire"   |
| Success toast     | "Yay, you did it!"           | "Invoice INV-2026-0427 posted" |
| Error message     | "Oops, something went wrong" | "PO-2026-0842 has 3 unmatched lines" |
| Loading           | "Loading..."                 | "Loading 248 customers..."      |
| Help text         | "Click here to add stuff"    | "+ New customer"                |
| Pill (status)     | "In Progress"                | "Sent"                          |
| Pill (count)      | "12 things"                  | "12 invoices"                   |
| Filter label      | "Active filter"              | "Active · 18"                   |

### 22.3 Button labels

- **Primary**: verb + object. "Save & post", "Confirm receive", "Post batch"
- **Secondary**: verb only. "Save", "Cancel", "Print"
- **Destructive**: red background. "Reject", "Void", "Cancel order"
- **Never** "OK", "Yes", "No" as primary action.

### 22.4 Numbers in copy

- Currency: `€7,580` (comma thousands, no decimals unless cents matter)
- Percentages: `94%` (no space, no decimals)
- Dates: `27 Aug 2026` (DD MMM YYYY for desktop, `27 Aug` for mobile)
- Quantities: `180u` (number + lowercase unit, no space)
- Durations: `8h`, `2.4s`, `6 days`
- IDs: `INV-2026-0427` (uppercase, hyphenated)
- Phone: `+49 89 1234 5678`
- IBAN: `DE89 3704 0044 0532 0130 00` (groups of 4)

---

## 23. Accessibility

### 23.1 Color contrast

All text/background combinations meet **WCAG AA** (4.5:1 for body, 3:1 for large):

- `--text` on `--surface`: 14.8:1 ✅
- `--text-mute` on `--surface`: 7.2:1 ✅
- `--text-dim` on `--surface`: 4.6:1 ✅
- White on `--primary`: 5.1:1 ✅
- White on `--gold`: 3.4:1 ⚠️ (only for 14px+ bold, never body)
- White on `--success`/`--teal`: 4.3:1 ✅
- White on `--danger`: 5.6:1 ✅

### 23.2 Focus indicators

Every interactive element has a visible focus ring. Never `outline: none` without
a replacement.

### 23.3 Semantic HTML

- `<button>` for actions, `<a>` for navigation
- `<table>` for tabular data
- `<label>` always paired with `<input>` (for attribute)
- `<header>`, `<nav>`, `<main>`, `<aside>`, `<footer>` for layout regions

### 23.4 Keyboard navigation

- Tab order follows visual order
- Enter submits forms
- Esc closes modals
- Arrow keys navigate list rows

### 23.5 Screen reader labels

- All icons have `aria-label`
- Decorative SVGs have `aria-hidden="true"`
- Status pills have accessible text equivalents (not just color)

### 23.6 Reduced motion

```css
@media (prefers-reduced-motion: reduce) {
  * { animation: none !important; transition: none !important; }
}
```

---

## 24. Anti-patterns

**What we never do:**

- ❌ **AI suggestions / "What can I help with?" blocks.** Explicitly removed from all views.
- ❌ **Real JavaScript tab switching.** Always static visual tabs.
- ❌ **Build steps** (Babel, Vite, Webpack). Single-file HTML or nothing.
- ❌ **Drop shadows on every card.** Most depth from borders, not shadows.
- ❌ **Gray "neutral" hero cards.** Identity heroes are navy gradient or domain-colored.
- ❌ **Exclamation marks in any copy.** Even success messages.
- ❌ **Emoji anywhere in the UI.** Not even the checkmark.
- ❌ **Color-only status.** Pills always have a dot + text label, not just color.
- ❌ **Mixing color meanings.** Gold never means "warning", red never means "money".
- ❌ **Bootstrap-style blue buttons everywhere.** Use the gradient primary.
- ❌ **Generic stock icons.** Lucide-style stroke icons only.
- ❌ **"Lorem ipsum".** Every mockup has real-feeling data.
- ❌ **Vague 3-yr portfolio.** "Reliable vendor" is banned. Use specific numbers.
- ❌ **Two-line field labels.** If a label wraps, shorten it.
- ❌ **Skeleton loaders in mockups.** Mockups are pictures, not apps.
- ❌ **Modal dialogs for confirmations.** Use inline error banners or side panels.
- ❌ **Empty pages without empty states.** Every list has an empty state designed.
- ❌ **Lowercase anything except status codes (MSA, SEPA, etc.).** Sentence case UI.

---

## Appendix A — Sample full layout (entity 21 price list detail)

```html
<div class="app">
  <aside class="sidebar">...</aside>
  <main class="main">
    <div class="topbar">...</div>
    <div class="layout">
      <aside class="left-pane">
        <!-- master list -->
      </aside>
      <section class="right-pane">
        <div class="detail-head">...</div>
        <div class="strip">
          <div class="strip-cell">5 cells</div>
        </div>
        <div class="body">
          <div class="t-tabs">...</div>
          <div class="card">...</div>
          <div class="grid-2">
            <div class="card">...</div>
            <div class="card">...</div>
          </div>
          <div class="notes">...</div>
          <div class="cv-anno">3-yr portfolio: ...</div>
        </div>
        <div class="meta">...</div>
      </section>
    </div>
  </main>
</div>
```

## Appendix B — File template

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>Acme CRM — {Entity} (Mockup)</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="...?family=Outfit:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
  <style>
    /* tokens + layout + components */
  </style>
</head>
<body>
  <div class="app">
    <aside class="sidebar">...</aside>
    <main class="main">...</main>
  </div>
</body>
</html>
```

## Appendix C — Reference files

- **Master-detail source of truth**: `/workspace/attachments/b8cfe10d__22814ebb-2230-42e6-9764-9cdf54f7331c.html`
- **Quotes master-detail (gold/teal/line patterns)**: `/workspace/attachments/6d077710__c852cf1f-a334-4031-85a1-043dd6f4bced.html`
- **Coverage by entity**: see `vision.md` section 3 (entity matrix)
- **Naming convention**: see `vision.md` section 10
- **How to extend**: see `vision.md` section 12

---

*Design language: Acme CRM v3.4 · Maintained by Mavis (MiniMax-M3) · For designers, design-aware agents, and reviewers.*
