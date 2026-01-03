# Migration Plans

This directory contains all planning documents for the Quarkus migration.

## Purpose

Plans enable:
- **Task breakdown** - Split complex migrations into manageable chunks
- **Parallel work** - Multiple agents work on independent services
- **Coordination** - Shared understanding of approach and dependencies
- **Progress tracking** - Clear milestones and completion criteria

## Plan Types

### 1. Overall Migration Plan
**File:** `overall-migration.md`

Master plan covering:
- Project goals and timeline
- All services to migrate
- Technology stack decisions
- Risk assessment
- Success metrics

### 2. Service Migration Plans
**Pattern:** `<service-name>-migration.md`

Per-service plans:
- Service overview and complexity
- Dependencies (DB, Redis, RabbitMQ)
- Migration steps (Repository → Service → Controller → Events)
- Testing strategy
- Completion criteria

**Examples:**
- `auth-service-migration.md`
- `patient-service-migration.md`
- `appointment-service-migration.md` (includes snapshot pattern)
- `facility-service-migration.md` (includes Saga pattern)

### 3. Feature Specification Plans
**Pattern:** `feature-<name>-spec.md`

For complex features spanning multiple services:
- API Gateway routing and JWT validation
- Event-driven snapshot updates
- Saga pattern for distributed transactions
- Native compilation strategy

**Examples:**
- `feature-api-gateway-spec.md`
- `feature-snapshot-pattern-spec.md`
- `feature-saga-admission-spec.md`

### 4. Agent Task Assignments
**File:** `task-assignments.md`

Coordinates parallel work:
- Which agent works on which service
- Dependencies between tasks
- Blocking vs. non-blocking work
- Communication checkpoints

## Plan Structure Template

Each plan should include:

```markdown
# [Service/Feature Name] Migration Plan

## Overview
- Description
- Complexity: Low/Medium/High
- Estimated effort: X days

## Dependencies
- Database: PostgreSQL
- Cache: Redis
- Messaging: RabbitMQ
- Other services: [list]

## Migration Steps
1. **Repository Layer**
   - Spring Data JPA → Hibernate Reactive Panache
   - Blocking → Uni/Multi returns

2. **Service Layer**
   - @Service → @ApplicationScoped
   - Business logic → Reactive chains

3. **Controller Layer**
   - @RestController → @Path
   - ResponseEntity → Uni/Response

4. **Event Publishing/Listening**
   - RabbitTemplate → Emitter<T>
   - @RabbitListener → @Incoming

5. **Testing**
   - @SpringBootTest → @QuarkusTest
   - Reactive assertions

## Files to Migrate
- List all files with paths

## Completion Criteria
- [ ] All endpoints work
- [ ] Events publish correctly
- [ ] Cache works
- [ ] Tests pass
- [ ] Native build succeeds

## Risks
- Potential issues and mitigations

## Notes
- Special considerations
```

## Parallel Work Strategy

### Phase 1: Independent Services (Can run in parallel)
- Auth Service (Agent A)
- Patient Service (Agent B)
- Doctor Service (Agent C)

### Phase 2: Dependent Services (Sequential dependencies)
- Appointment Service (requires Patient + Doctor snapshots)
- Medical Records Service
- Facility Service (Saga pattern)

### Phase 3: Cross-Cutting (Requires coordination)
- Notification Service (listens to all events)
- Audit Service (logs all operations)

## Quick Reference

```bash
# View all plans
ls -la .ai/plans/

# Copy overall plan to agent-specific directory
cp .ai/plans/overall-migration.md ~/.claude/plans/

# Create new service plan
cat .ai/plans/template-service-migration.md > .ai/plans/new-service-migration.md
```

## Current Plans

- `overall-migration.md` - Master migration plan (exists at ~/.claude/plans/)
- Service-specific plans to be created as needed
- Agent assignments to be created when parallel work starts

---

**Tip:** Before starting work on a service, always read its plan and check `task-assignments.md` to avoid conflicts.
