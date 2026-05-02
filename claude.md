# claude.md — Project Meta-Guide

## What This Project Is
iOS app: scan physical trading cards (Pokémon, MTG, sports cards) → real-time market valuation.
Stack: SwiftUI, FastAPI, PostgreSQL, TCGPlayer/eBay APIs, Apple Vision.

---

## Competitor Pain Points → Project Goals

Research source: App Store reviews across PokeScope, Dex, Acorn, Pokellector, TCGPlayer, TCG Card Scanner, Pokedata (2024–2025).

| # | Competitor Pain Point | Severity | Our Project Goal |
|---|---|---|---|
| P1 | **Reprint/variant misidentification** — apps confuse reprints (Celebrations Pikachu → original Base). Price delta can be 100×. | Critical | G1: Scan accuracy ≥97% on reprints/variants using set number + art hash disambiguation, not image-only |
| P2 | **Slow or unreliable scanner** — "kind of slow and the scanner doesn't work a lot of times." Fails in low-light or through binder sleeves. | High | G2: Scan-to-result latency ≤600ms on-device. Must work in dim light and through standard sleeves. |
| P3 | **Inaccurate pricing** — apps report $86 for a $0.20 card. Single-source pricing (TCGPlayer only) misses real market. | Critical | G3: Market price = weighted avg of TCGPlayer + eBay 30-day completed sales + CardMarket. Flag staleness if >24h. Never use listing price. |
| P4 | **No grading ROI at scan time** — users tab between 3+ sites to decide if a card is worth grading. No app delivers this in-flow. | High | G4: Grade ROI screen on every scan: condition slider → expected PSA grade → net profit after fee. Pop report inline. |
| P5 | **Paywalled scanner that doesn't work** — Dex charges $4/mo for a "beta-quality" scanner. Users call it fraud. | High | G5: Core scanner always free. Paywall only on portfolio analytics, Grade ROI, and price alerts. Never gate scan accuracy. |
| P6 | **No fake/counterfeit detection** — forgeries replicate holos. No app flags suspicious cards. SEA market particularly exposed. | Medium | G6: Fake-flag layer (premium): font weight + holo pattern hash + card number format check. "High risk" verdict triggers expert CTA. |
| P7 | **New sets added too slowly** — "the 2025 holiday calendar came out a month ago, still not in the app." | Medium | G7: Automated set ingestion pipeline. New sets live in app ≤48h after official Pokémon TCG API update. Zero manual step. |
| P8 | **No Japanese card support** — multiple apps English-only despite JP being 41.5% of TCG Pocket revenue. | Medium | G8: Japanese card support in v1.5. Same CV pipeline, JP card database via Pokémon TCG API + supplementary scrape. |
| P9 | **Collection data loss on crash/reinstall** — Dex and Pokellector users report hours of collection data wiped. | High | G9: Collection data persisted server-side (authenticated) with local cache. Never localStorage-only. Sync on every add. |
| P10 | **Aggressive paywall friction** — "constantly tries to get me to get premium insights." Users churn on push, not value gap. | Medium | G10: Single paywall moment: after scan #20 (free tier limit). No interstitials, no banner upsells, no nag screens. |

### Goal enforcement rule
Before any feature spec or code task: map it to G1–G10. If it doesn't address a goal, explicitly state why we're building it anyway or deprioritize.

---

## 6-Step Business Pipeline (Enforce on Every Decision)

Apply this pipeline whenever evaluating a feature, pricing decision, or scope change.

### Step 1 — Diagnose
Before building anything, define the problem precisely.
- What is the exact user failure mode? (Cite a review or data point.)
- Which competitor has this problem and why haven't they fixed it?
- What is the cost to the user of this problem existing? (Money lost, time wasted, trust broken.)
- Output: 1-sentence problem statement. No solution yet.

### Step 2 — Pick Tools
Choose the right tech for the diagnosed problem. Always evaluate:
- On-device vs server-side (privacy, latency, cost tradeoff)
- Build vs API (e.g. Vision framework vs OpenAI Vision — default to on-device unless accuracy gap >5%)
- Single source vs multi-source (pricing always multi-source per G3)
- Output: options table with tradeoffs + recommendation.

### Step 3 — Calculate ROI
Formula: `Hourly rate × hours/4 weeks × 12 months = annual value`
Apply to every feature build decision:
- Estimate engineering hours to build + maintain for 12 months
- Estimate revenue impact: conversion lift, churn reduction, or new tier unlock
- If ROI < 1× (costs more than it earns in year 1), defer or cut scope
- Example: Grade ROI screen — 40h build × $150/hr = $6,000 cost. If it converts 2% more free→Pro at 10K MAU ($4.99/mo) = $999/mo × 12 = $11,988 annual. ROI = 2×. Build it.

### Step 4 — Tiered Packaging
Every feature maps to a tier. Default logic:

| Tier | Price | What's in it |
|---|---|---|
| Free | $0 | 20 scans/mo, TCGPlayer price only, basic collection (50 cards) |
| Pro | $4.99/mo or $39/yr | Unlimited scans, all 3 markets, Grade ROI, price alerts, full collection |
| Shop | $29/mo | Bulk scan API, fake flag, branded reports, inventory export |

Rules:
- Core scanner accuracy (G1, G2) is NEVER paywalled — see G5.
- Grade ROI (G4) and fake detection (G6) are Pro/Shop gates.
- If a feature only matters to <5% of users, it's Shop tier or cut.

### Step 5 — Avoid Traps

**Underpricing:**
- Do not price Pro below $3.99/mo. At $2.99, conversion math breaks: you need 3× more users to hit the same MRR as a $8.99 app.
- Annual discount max 35% ($39/yr vs $59.88). Beyond that signals desperation.
- Never offer "lifetime" pricing before 1K paying users — no baseline churn data.

**Small retainers / scope creep:**
- Do not accept custom feature work for individual users or shops for <$500 flat or <$29/mo ongoing.
- Any custom integration (ERP, POS, custom grading API) = Shop Enterprise tier at $99+/mo minimum.
- If a user requests a feature not on the roadmap, log it. Build it when 10+ users request the same thing.

**Other traps:**
- Do not launch on Android before iOS v1.0 is profitable. Split focus kills both.
- Do not add a new TCG (MTG, One Piece) before Pokémon-only MAU exceeds 5K.
- Do not partner with PSA/CGC before community trust scores are verified — PSA scandal ongoing.

### Step 6 — Prototype & QA

**Prototype standard (Figma/ProtoPie):**
- Ship prototype to 5 target users before writing any Swift code for a new feature.
- Success threshold: ≥4/5 users reach the target action without prompting.
- If <4/5: iterate prototype. Do not proceed to build.

**QA gates before any release:**
- [ ] Scan accuracy ≥97% on reprint test set (30 cards: 10 reprints, 10 holos, 10 promos)
- [ ] Latency ≤600ms median on iPhone 13 (baseline device)
- [ ] Price delta vs manual TCGPlayer check ≤5% on 20 random cards
- [ ] Grade ROI output matches manual PSA pop + eBay calc within 10%
- [ ] No raw scan images written to disk or transmitted to server
- [ ] Collection data survives app kill + reinstall cycle
- [ ] Paywall appears exactly once at scan #21, not before

**Regression tests (run on every PR):**
- Scan 10 known cards from canonical test set. All 10 must match.
- Hit pricing endpoint for 5 cards. All must return within 800ms.
- Add 3 cards to collection, kill app, reopen — all 3 must be present.

---

## How to Use Claude Effectively Here

### Prompt Patterns That Work

**Architecture decision:**
```
Context: [current state]
Decision: [what needs deciding]
Constraints: [hard limits]
→ Give options table + recommendation
```

**Code task:**
```
File: [filename]
Goal: [exact behavior]
Existing code: [paste or describe]
→ Return complete function/class, no explanation
```

**Research/analysis:**
```
Question: [specific question]
Data available: [what I have]
Output needed: [table / bullets / number]
```

**Debugging:**
```
Error: [exact error]
Context: [what was expected]
Tried: [what failed]
```

**Pipeline check:**
```
Feature: [name]
Pipeline step: [1–6]
→ Output the required artifact for that step only
```

### Trigger Phrases
| Phrase | Claude behavior |
|---|---|
| `"options?"` | Return comparison table, pick a winner |
| `"quick"` | ≤5 bullets, no elaboration |
| `"deep dive"` | Full technical analysis, include edge cases |
| `"draft"` | First-pass doc/spec, I'll iterate |
| `"security check"` | Review for vulnerabilities + privacy violations |
| `"so what?"` | Force implication analysis on any topic |
| `"pipeline [1-6]"` | Run only that step of the 6-step pipeline |
| `"roi check"` | Run Step 3 only: hours × rate × 12 calculation |
| `"tier?"` | Map a feature to Free/Pro/Shop + justification |

---

## Context to Always Include

### For iOS/SwiftUI tasks
- Target iOS version (assume iOS 17+ unless stated)
- Whether feature is on-device or requires network
- Relevant existing models/services it integrates with

### For pricing/data tasks
- Card name + set + condition (PSA grade if known)
- Target API (TCGPlayer vs eBay vs PSA)
- Freshness requirement (real-time vs cached ok)

### For backend tasks
- Endpoint context (public vs authenticated)
- Expected load (scans/minute estimate)
- Whether response is sync or async/webhook

---

## What Claude Will Push Back On
- Storing raw scan images server-side
- Client-side API keys
- Pseudocode when real code was asked for
- Mixing foil/non-foil pricing without explicit reason
- Using listing price as "market price"
- Paywalling core scan accuracy (violates G5)
- Building a feature without mapping it to G1–G10 and Steps 1–6
- Pricing Pro below $3.99/mo (violates Step 5)
- Skipping prototype testing before Sprint 1 coding (violates Step 6)

---

## Project Glossary
| Term | Meaning |
|---|---|
| `card_sku` | Unique ID: name + set + language + variant (foil/non-foil) + condition |
| `market_price` | Weighted avg of last 30-day completed sales (TCGPlayer + eBay + CardMarket) |
| `grade` | PSA/BGS condition score 1–10, null if raw |
| `scan_session` | One user scan event — discarded after price returned |
| `price_staleness` | >24h since last market data refresh |
| `reprint_flag` | Card identified as potential reprint — requires set number confirmation |
| `grade_roi` | Net profit = (expected_grade_sell_price − raw_price − grading_fee) |
| `pipeline_step` | One of Steps 1–6 of the business pipeline |

---

## Anti-Patterns (Never Do)
- Never ask Claude to "build the whole app" in one prompt — break into components
- Never skip specifying card condition — prices swing 10x between PSA 7 and PSA 10
- Never treat eBay listing price as sold price — use completed sales filter only
- Never build a feature that doesn't survive Step 3 ROI check
- Never launch a tier without completing Step 4 packaging table for it
- Never gate core scan accuracy behind a paywall (G5, Step 5)