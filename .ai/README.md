# AI Workspace - Clinic-Mgmt-Quarkus Migration

This directory contains structured documentation and task management for AI agents working on the Quarkus migration project.

## Directory Structure

```
.ai/
├── docs/              # Documentation and guides
│   └── fixes/         # Fix documentation (issues resolved, PRs, patches)
├── plans/             # Migration plans and feature specifications
└── journals/          # Progress logs, completed work, roadmaps
```

## Purpose

This workspace enables:
- **Parallel development** by multiple AI agents
- **Task isolation** - agents work on independent features without conflicts
- **Progress tracking** - maintain records of completed work
- **Knowledge sharing** - document fixes, patterns, and decisions
- **Planning coordination** - split complex features into agent-specific tasks

## Usage Guidelines

### For AI Agents

#### 1. Starting a New Task
1. Check `plans/` for feature specifications
2. Create a work log in `journals/current-work/`
3. Document any fixes in `docs/fixes/`
4. Update completion status in `journals/completed/`

#### 2. Parallel Work (Multiple Agents)
- Each agent works on a separate service/feature
- Use `plans/<feature-name>.md` for task breakdown
- Coordinate via shared plan files (read-only for non-assigned tasks)
- Log progress in `journals/<agent-id>-<feature>.md`

#### 3. Documentation
- **Bug Fixes:** `docs/fixes/<issue-description>.md`
- **Architecture Decisions:** `docs/architecture/<topic>.md`
- **API Changes:** `docs/api/<service-name>.md`

#### 4. Completion
- Move work log from `journals/current-work/` to `journals/completed/`
- Update roadmap in `journals/roadmap.md`
- Document lessons learned

### Directory Details

#### `docs/` - Documentation
Stores all documentation related to the project:
- `fixes/` - Issues resolved, security patches, bug fixes
- `architecture/` - Design decisions, patterns used
- `api/` - API documentation, endpoint changes
- `migration/` - Migration-specific guides and notes

#### `plans/` - Plans and Specifications
Contains planning documents for features and migrations:
- `overall-migration.md` - Master migration plan
- `<service-name>-migration.md` - Per-service migration plans
- `<feature-name>-spec.md` - Feature specifications for parallel work
- `task-assignments.md` - Agent task assignments for coordination

#### `journals/` - Progress Logs and Roadmaps
Tracks progress and maintains historical records:
- `current-work/` - Active work logs (one per agent/task)
- `completed/` - Completed work records
- `roadmap.md` - Future work and priorities
- `daily-progress.md` - Daily standup notes

## Workflow Example

### Scenario: Two agents migrating services in parallel

**Agent A - Patient Service Migration:**
1. Reads `plans/patient-service-migration.md`
2. Creates `journals/current-work/agent-a-patient-service.md`
3. Works on migration, documents fixes in `docs/fixes/`
4. On completion, moves log to `journals/completed/`

**Agent B - Doctor Service Migration:**
1. Reads `plans/doctor-service-migration.md`
2. Creates `journals/current-work/agent-b-doctor-service.md`
3. Works independently (no conflicts with Agent A)
4. On completion, moves log to `journals/completed/`

**Both agents can work simultaneously without stepping on each other's toes.**

## File Naming Conventions

### Plans
- `<service-name>-migration.md` - Service migration plan
- `<feature-name>-spec.md` - Feature specification
- `overall-migration.md` - Master plan

### Fixes
- `<YYYY-MM-DD>-<short-description>.md` - Dated fix logs
- `security-<issue-name>.md` - Security-related fixes
- `build-<issue-name>.md` - Build/dependency fixes

### Journals
- `<agent-id>-<task-name>.md` - Work log format
- `YYYY-MM-DD-standup.md` - Daily progress
- `completed-<task-name>.md` - Completion record

## Integration with CLAUDE.md

All AI agents must follow the guidelines in `/CLAUDE.md` which includes:
- How to use this `.ai/` workspace
- Code standards and conventions
- Communication protocols

## Quick Start

```bash
# View current plans
ls -la .ai/plans/

# Check active work
ls -la .ai/journals/current-work/

# Review completed work
ls -la .ai/journals/completed/

# Find fix documentation
ls -la .ai/docs/fixes/
```

## Templates

Templates for common documents are available in `.ai/templates/`:
- `plan-template.md` - Feature plan template
- `work-log-template.md` - Work journal template
- `fix-template.md` - Fix documentation template

---

**Last Updated:** 2026-01-03
**Maintained By:** AI Agents working on Quarkus migration
