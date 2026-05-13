# PokeScan Design System v1.0

Anchor: Apple Human Interface Guidelines (iOS 17+). Light mode primary. SF Pro typography. One ink-blue accent. Reduce decisions, surface answers, one action per screen.

---

## Tokens

### Color — Light

| Token | Hex | Use |
|---|---|---|
| `bg/canvas` | `#FFFFFF` | Root background |
| `bg/elevated` | `#FAFAFA` | Cards, sheets |
| `bg/sunken` | `#F2F2F7` | Inset surfaces, search field |
| `bg/translucent` | `rgba(255,255,255,0.72)` + 20px backdrop-blur | Nav bars, headers |
| `border/hairline` | `#E5E5EA` | Dividers, subtle separators |
| `border/default` | `#D1D1D6` | Card borders, controls |
| `text/primary` | `#0A0A0A` | Headlines, body |
| `text/secondary` | `#6E6E73` | Metadata, captions |
| `text/tertiary` | `#AEAEB2` | Disabled, placeholder |
| `accent/primary` | `#2563EB` | Primary CTA, links, focus |
| `accent/pressed` | `#1D4ED8` | Active/pressed |
| `accent/tint` | `#EFF4FE` | Accent backgrounds, badges |
| `success` | `#16A34A` | Authentic, gain, verified |
| `warning` | `#D97706` | Limit approached, attention |
| `danger` | `#DC2626` | Error, fake-flag |

Reserve: dark mode is a v1.1 deliverable — do NOT introduce dark surfaces in v1.

### Typography — SF Pro

| Style | Size / Line / Weight | Use |
|---|---|---|
| Display | 32 / 38 / 700 | Onboarding hero, paywall hero |
| Title 1 | 24 / 30 / 700 | Screen headers |
| Title 2 | 20 / 26 / 600 | Section headings |
| Headline | 17 / 22 / 600 | List rows, card titles |
| Body | 15 / 20 / 400 | Primary copy |
| Subhead | 13 / 18 / 500 | Secondary labels, chips |
| Caption | 12 / 16 / 400 | Metadata, footnotes |
| Tabular | 17 / 22 / 600 mono | Prices, numerics (`SF Mono`) |

Tracking: -0.4px on Display & Title 1; default elsewhere.

### Spacing — 4pt base, 8pt grid

| Token | Px | Common use |
|---|---|---|
| `xs` | 4 | Icon-to-text gap |
| `sm` | 8 | Inner padding tight |
| `md` | 12 | Stack gap default |
| `lg` | 16 | Card padding, screen gutter |
| `xl` | 24 | Section spacing |
| `2xl` | 32 | Hero block padding |
| `3xl` | 48 | Top-of-screen breathing room |

### Radius

| Token | Px |
|---|---|
| `control` | 10 (buttons, inputs) |
| `card` | 12 |
| `sheet` | 20 (top corners only) |
| `pill` | 999 |
| `icon` | 22% of side (squircle) |

### Elevation

`e1` `0 1px 2px rgba(0,0,0,0.04)` — list rows, chips
`e2` `0 4px 12px rgba(0,0,0,0.06)` — cards, sheets resting
`e3` `0 12px 32px rgba(0,0,0,0.10)` — modals, sheet open

Never combine border + shadow above e1 on the same element.

### Motion

Standard: `200ms cubic-bezier(0.2, 0, 0, 1)` — opacity, color, transform
Sheet present: `320ms cubic-bezier(0.32, 0.72, 0, 1)` — bottom sheet, modals
Skeleton fade: `1200ms ease-in-out` infinite — loading
No bounce, no spring overshoot, no decorative animation.

---

## Components

| Component | Spec |
|---|---|
| **Primary button** | bg `accent/primary`, text `#FFF`, weight 600, radius `control`, padding `12 / 20` |
| **Secondary button** | bg `bg/elevated`, border `border/default`, text `text/primary`, same padding |
| **Tertiary / link** | text `accent/primary`, weight 500, no bg, no border |
| **Card** | bg `bg/canvas`, border `border/hairline`, radius `card`, padding `lg`, elevation `e1` |
| **List row** | 60px tall, padding `lg / md`, divider `border/hairline` between rows |
| **Chip / badge** | radius `pill`, padding `4 / 10`, Subhead type, bg `accent/tint`, text `accent/primary` |
| **Input** | radius `control`, padding `12 / 14`, bg `bg/sunken`, border on focus only `accent/primary` |
| **Sheet** | radius `sheet` top, bg `bg/canvas`, elevation `e3`, drag handle `border/default` 36×4 pill |
| **Tab bar** | bg `bg/translucent`, height 56, hairline top border, active `accent/primary`, inactive `text/secondary` |
| **Status indicator** | dot 8×8, color = semantic token |

---

## Iconography

- Single subject, single metaphor — never compound.
- Squircle bg, radius 22%.
- 80% safe zone for the subject; 10% margin all sides.
- Max 3 colors + neutrals. Default: 1 ink blue + 1 dark text + 1 white.
- SF Symbols for in-app icons (`24×24` default, `20×20` in tab bars). 1.5px stroke.
- App icon must read at 60px (home screen), 120px (App Store list), 1024px (App Store hero).

---

## Information Architecture — Reduce Cognitive Load

1. **One primary action per screen.** Secondary actions live in overflow.
2. **Numbers before labels.** Price first, then "30-day avg," not the reverse.
3. **Progressive disclosure.** Show 3 fields by default; "More details" expands.
4. **Empty states are instructions**, not decoration. Tell the user what to do next in one sentence.
5. **System-native patterns.** Use iOS sheet, action sheet, alert — never custom modals when an HIG primitive exists.
6. **Latency masks**: skeleton 200ms+, spinner 800ms+, progress bar 4s+.

---

## Anti-patterns

- Multiple accent hues per screen
- Decorative animation (only functional motion)
- Pixel art, mascots, emoji as primary visual
- Drop shadows above 12% opacity
- Gradients except the icon bg vignette
- Border + shadow combined above e1
- More than 4 typographic styles per screen
- Bottom sheet with 3+ scrollable sections (use full screen instead)
- Confirm dialogs for reversible actions

---

## File outputs in this system

| File | Status |
|---|---|
| `app_icon.svg` | Generated this round |
| `prototype.html` | Pending — redo to match this guide next |
| `tokens.css` | Pending — extract :root variables |
