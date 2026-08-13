# Session Handover
_Generated: 2026-08-13T18:18:56Z_
_Branch: main_
_Trigger: auto | Context at compact: unknown%_
_Compact count this project: 0_

---

## 🎯 Active Task
**What we're building/fixing:**
project start

**Phase:** Phase 0 — Init
**Next action:** run /context-health to verify setup

---

## ✅ Completed This Session
- [ ] (track completed items here)

---

## 🔄 In Progress (Exact Resume Point)
**Branch:** `main`
**Last commit:** `988b8ac Fix iOS Screen Time entitlement & timer race conditions, and implement native Android Jetpack Compose version`
**Next immediate action:** run /context-health to verify setup

---

## 📋 Remaining Work
1. (add remaining work items here)

---

## 🏗 Architecture Decisions Made
| Decision | Rationale | Date |
|----------|-----------|------|
| (none yet) | — | — |

---

## 🔧 Commands to Resume

**This exact conversation** (SDK/CLI transcript resume):
```bash
# Same machine AND same directory it started in:
claude --resume db9d8c76-9068-476c-ae0b-9aea36cef8fa
```
- Session ID    : `db9d8c76-9068-476c-ae0b-9aea36cef8fa`
- Transcript    : `/Users/theranosis_dx/.claude/projects/-Users-theranosis-dx-projects-pomodoro/db9d8c76-9068-476c-ae0b-9aea36cef8fa.jsonl`
- Bound to cwd  : `/Users/theranosis_dx/projects/pomodoro`
- Stored at     : `~/.claude/projects/-Users-theranosis-dx-projects-pomodoro/db9d8c76-9068-476c-ae0b-9aea36cef8fa.jsonl`

> ⚠️ Transcript resume is **cwd-bound**. It only works from the same directory
> on the same machine. If this session started in a git **worktree**, that
> worktree's path is the cwd — resuming from `main` (or after the worktree is
> deleted) will silently start a *fresh* session. Per the Agent SDK docs, the
> robust cross-host / cross-worktree path is **not** transcript resume — it's
> this handover file: read it into a new session's prompt as application state.

**Project state** (any machine — the robust path):
```bash
git pull origin main
bash scripts/session_sync.sh --load

# In Claude Code:
# /context-health     — verify hooks are wired
# /handover           — review this file
# /token-status       — check context usage
```

---

## 📁 Files Modified This Session
| File | Status |
|------|--------|
| (none tracked yet) | — |

---

## 🌿 Git Context
```
Branch  : main
Commit  : 988b8ac Fix iOS Screen Time entitlement & timer race conditions, and implement native Android Jetpack Compose version
Status  : ?? .claude/
```

Recent commits:
```
988b8ac Fix iOS Screen Time entitlement & timer race conditions, and implement native Android Jetpack Compose version
4d6caef Fix focus lifecycle and persistence
d084356 Add Cozy Focus app with reliable persistence
```

---

## ⚠️ Critical Rules
- Never commit secrets or API keys
- Run /handover before switching devices

---

## 🧬 Bioinformatics Context (if applicable)
- Not configured for this project

---
_Auto-updated by `pre-compact.sh` hook and `/handover` skill._
_Read this at the start of every session. Update with `/handover`._
