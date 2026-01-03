# Journals - Progress Logs and Roadmaps

This directory tracks ongoing work, completed tasks, and future roadmaps.

## Purpose

Journals provide:
- **Real-time progress tracking** - What's currently being worked on
- **Historical record** - What was completed and when
- **Roadmap visibility** - What's next and priorities
- **Agent coordination** - Multiple agents can see each other's progress

## Directory Structure

```
journals/
├── current-work/      # Active work logs (one per agent/task)
├── completed/         # Completed work records
├── roadmap.md         # Future work and priorities
└── daily-standup/     # Daily progress snapshots
```

## Current Work Logs

**Location:** `current-work/`

**Naming:** `<agent-id>-<task-name>.md`

**Purpose:**
- Track what an agent is currently working on
- Log blockers, decisions, and progress
- Provide visibility for coordination

**Template:**
```markdown
# [Agent ID] - [Task Name]

**Started:** YYYY-MM-DD HH:MM
**Status:** In Progress / Blocked / Review
**Agent:** agent-xyz

## Objective
Brief description of what's being built/migrated

## Progress
- [x] Step 1 completed
- [ ] Step 2 in progress
- [ ] Step 3 pending

## Blockers
- None / [Describe blockers]

## Decisions Made
- Decision 1: Reasoning
- Decision 2: Reasoning

## Next Steps
1. Next immediate task
2. Following task

## Notes
Any important observations or learnings
```

**Example files:**
- `agent-a-auth-service-migration.md`
- `agent-b-patient-service-migration.md`
- `agent-c-api-gateway-implementation.md`

## Completed Work

**Location:** `completed/`

**Naming:** `<YYYY-MM-DD>-<task-name>.md`

**Purpose:**
- Historical record of completed work
- Reference for similar future tasks
- Lessons learned documentation

**Template:**
```markdown
# [Task Name] - Completed

**Completed:** YYYY-MM-DD HH:MM
**Duration:** X hours/days
**Agent:** agent-xyz

## Summary
Brief summary of what was accomplished

## Work Done
- Repository layer migrated
- Service layer made reactive
- Controllers updated to JAX-RS
- Events integrated with RabbitMQ
- Tests written and passing

## Challenges Faced
1. Challenge: Description
   Solution: How it was resolved

2. Challenge: Description
   Solution: How it was resolved

## Lessons Learned
- Lesson 1
- Lesson 2

## Files Modified
- `/path/to/file1.java`
- `/path/to/file2.java`

## Testing
- Unit tests: X passing
- Integration tests: Y passing
- Native build: Success/Failed

## References
- Related plans: `.ai/plans/service-migration.md`
- Related fixes: `.ai/docs/fixes/issue.md`
```

## Roadmap

**File:** `roadmap.md`

**Purpose:**
- Upcoming features and priorities
- Long-term vision
- Dependencies and sequencing

**Sections:**
1. **Immediate (This Week)**
2. **Short-term (This Month)**
3. **Long-term (This Quarter)**
4. **Future Considerations**

## Daily Standups

**Location:** `daily-standup/`

**Naming:** `<YYYY-MM-DD>-standup.md`

**Purpose:**
- Quick snapshot of daily progress
- Cross-agent coordination
- Identify blockers early

**Template:**
```markdown
# Daily Standup - YYYY-MM-DD

## Agent A
**Yesterday:** Completed auth service repository layer
**Today:** Working on service layer reactive chains
**Blockers:** None

## Agent B
**Yesterday:** Started patient service migration planning
**Today:** Implementing reactive Panache repositories
**Blockers:** Waiting for hospital-common-quarkus v1.0.1 release

## Agent C
**Yesterday:** Researching API Gateway patterns
**Today:** Implementing Vert.x routing
**Blockers:** Need clarity on JWT validation approach

## Decisions Needed
- Should we use Consul or Kubernetes DNS for service discovery?
- Native compilation: Enable from start or test JVM first?

## Announcements
- hospital-common-quarkus v1.0.0 released
- New fix documentation template available
```

## Workflow

### Starting a New Task
```bash
# 1. Create work log
cat > .ai/journals/current-work/agent-a-task-name.md

# 2. Update roadmap
vim .ai/journals/roadmap.md  # Move task from "Upcoming" to "In Progress"

# 3. Work on task...

# 4. Update work log regularly
```

### Completing a Task
```bash
# 1. Finalize work log
vim .ai/journals/current-work/agent-a-task-name.md

# 2. Move to completed/
mv .ai/journals/current-work/agent-a-task-name.md \
   .ai/journals/completed/2026-01-03-task-name.md

# 3. Update roadmap
vim .ai/journals/roadmap.md  # Mark complete, move next task up
```

### Daily Standup
```bash
# Create today's standup
cat > .ai/journals/daily-standup/2026-01-03-standup.md

# Fill in each agent's update
```

## Quick Reference

```bash
# View active work
ls -la .ai/journals/current-work/

# Check completed work (last 5)
ls -lt .ai/journals/completed/ | head -5

# View roadmap
cat .ai/journals/roadmap.md

# Today's standup
cat .ai/journals/daily-standup/$(date +%Y-%m-%d)-standup.md
```

## Metrics

Track these metrics in completed work:
- Time to complete
- Number of files modified
- Number of tests written
- Issues encountered
- Native build status

This helps estimate future work and improve processes.

---

**Tip:** Keep work logs updated in real-time. Future agents (and your future self) will thank you!
