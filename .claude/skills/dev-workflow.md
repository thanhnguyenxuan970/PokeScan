---
name: dev-workflow
description: Master dev workflow — Stage 1 dispatches backend-agent + android-agent in PARALLEL (single message, two Agent calls), then Stage 2 runs test-runner → code-reviewer → pr-preparer SEQUENTIALLY. Invoke: /dev-workflow [backend|android|all]. Each stage receives prior stage output as context.
---

Announce: "dev-workflow: Stage 1 (parallel debug) → Stage 2 (test → review → PR)."

## Scope Parameter
Parse argument from invocation:
- No argument or `all` → scope = "full codebase scan"
- `backend` → scope = "backend/app/ only — skip Android agent, run backend + tests only"
- `android` → scope = "android/app/src/main/ only — skip backend agent, run Android + tests only"

---

## Stage 1 — PARALLEL (backend-agent + android-agent)

**CRITICAL:** Dispatch both agents in a SINGLE message using two Agent tool calls.
Do NOT send one, wait, then send the other — they MUST be parallel.

If scope = `backend`: dispatch backend-agent only.
If scope = `android`: dispatch android-agent only.
If scope = `all` or no arg: dispatch BOTH simultaneously.

**backend-agent prompt:**
```
You are the backend-agent for PokeScan.
Task: Debug and improve backend/app/. Scope: [SCOPE].
Read all files in backend/app/ per your read order.
Fix CRITICAL and WARNING issues. Return full findings report.
```

**android-agent prompt:**
```
You are the android-agent for PokeScan.
Task: Debug and improve android/app/src/main/java/com/pokescan/app/. Scope: [SCOPE].
Read all files per your read order.
Fix CRITICAL and WARNING issues. Return full findings report.
```

Wait for BOTH agents to complete. Store combined output as [STAGE1_OUTPUT]:
```
## Backend Findings
[backend-agent full output]

## Android Findings
[android-agent full output]
```

---

## Stage 2a — SEQUENTIAL: Test Runner

Dispatch test-runner with:
```
Run both test suites and report results.

## Context: Stage 1 Debug Findings
[STAGE1_OUTPUT]
```

Wait for completion. Store as [TEST_OUTPUT].

---

## Stage 2b — SEQUENTIAL: Code Reviewer

Dispatch code-reviewer with:
```
Review code quality for PokeScan.

## Context: Stage 1 Debug Findings
[STAGE1_OUTPUT]

## Context: Test Results
[TEST_OUTPUT]
```

Wait for completion. Store as [REVIEW_OUTPUT].

---

## Stage 2c — SEQUENTIAL: PR Preparer

Dispatch pr-preparer with:
```
Prepare PR documentation.

## Context: Code Review
[REVIEW_OUTPUT]
```

Wait for completion. Store as [PR_OUTPUT].

---

## Final Report

Print to user:

```
## Dev Workflow Complete ✓

### Stage 1: Debug
Backend: [N CRITICAL, M WARNING, K INFO] — [BACKEND DONE line]
Android: [N CRITICAL, M WARNING, K INFO] — [ANDROID DONE line]

### Stage 2a: Tests
[Overall PASS/FAIL — counts from TEST RESULTS]

### Stage 2b: Review
Verdict: [APPROVE / REQUEST CHANGES]
Risk: [HIGH / MEDIUM / LOW]

### Stage 2c: PR Artifacts
[Commit message subject line]
[PR title]
(See above for full PR body)

---
Next action:
[if APPROVE] → Copy commit message above → git add → git commit → git push → open PR
[if REQUEST CHANGES] → Fix blockers listed in Stage 2b before merging
```

---

## Error Handling
- Single Stage 1 agent fails → continue with partial [STAGE1_OUTPUT], note gap in downstream agents
- Both Stage 1 agents fail → abort, report "WORKFLOW ABORTED: both debug agents failed"
- test-runner blocked → propagate `[TEST DATA MISSING]` to Stage 2b context
- code-reviewer graph not built → agent falls back internally (see agent definition)
- pr-preparer NEEDS CONFIRMATION items → list them in Final Report, do not block
