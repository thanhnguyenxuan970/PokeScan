---
name: process
description: Full delivery pipeline. Runs check_plan.md → implement → check_code.md → review → caveman-review & fix → close.md → caveman-commit in sequence.
---

Execute the full delivery pipeline for the current plan. Ask user which plan file if unclear.

## Phase 1 — VALIDATE PLAN

1. Run `/check_plan.md` on the plan file.
   - Do not proceed until check_plan.md reports ✅ PLAN CLEAN.
   - If `[NEEDS CONFIRMATION]` items remain, surface them to user and wait for resolution.
2. **Auto: run `/compact`** — execute immediately, no user prompt needed.

---

## Phase 2 — IMPLEMENT

Implement every step in the (now-clean) plan file, in order.

Rules:
- Follow the plan exactly — no scope additions, no design changes.
- After each step, verify the step's output matches what the plan describes before moving to the next.
- If a step is blocked (missing credential, external dependency), mark `[BLOCKED: reason]` inline in the plan and skip — continue remaining steps.
- Do not ask for confirmation between steps unless a step is marked `[NEEDS CONFIRMATION]`.

When all steps are done (or blocked with reason), state:
- Steps completed: N
- Steps blocked: M (with reasons)

**Auto: run `/compact`** — execute immediately, no user prompt needed.

---

## Phase 3 — VERIFY CODE

1. Run `/check_code.md` on all files created or modified during Phase 2.
   - Scope: only files touched in Phase 2 (not the entire codebase).
   - Do not proceed until check_code.md reports ✅ CODE CLEAN.
2. **Auto: run `/compact`** — execute immediately, no user prompt needed.

---

## Phase 4 — REVIEW & FIX

1. Run `/review` then `/caveman-review` on all files created or modified during Phase 2.
   - Fix every issue surfaced before proceeding.
   - Re-run `/caveman-review` after fixes until no issues remain.
   - Do not proceed to Phase 5 until review is clean.
2. **Auto: run `/compact`** — execute immediately, no user prompt needed.

---

## Phase 5 — CLOSE

1. Run `/close.md` to finalize the delivery.
   - Do not run close.md until Phase 4 review is clean.
2. **Auto: run `/compact`** — execute immediately, no user prompt needed.

---

## Phase 6 — COMMIT

Run `/caveman-commit` to generate and create the commit.

- Scope: all changes from Phases 2–5.
- Do not commit until close.md completes successfully.

---

## Final Report

After all six phases complete:

- Phase 1: cycles run + issues fixed
- Phase 2: steps completed / blocked
- Phase 3: cycles run + issues fixed
- Phase 4: review issues found + fixed
- Phase 5: close actions taken
- Phase 6: commit created
- Any `[NEEDS CONFIRMATION]` or `[BLOCKED]` items requiring user action
- Status: ✅ PROCESS COMPLETE — plan clean, implemented, code clean, reviewed, closed, committed
