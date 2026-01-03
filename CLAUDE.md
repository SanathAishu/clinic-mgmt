# Claude Code Instructions - Quarkus Migration Project

## Terminal Commands - RUST CLI STACK

When running terminal commands, **ALWAYS use Rust-based alternatives**:

| Instead of | Use |
|------------|-----|
| `find . -name "*.java"` | `fd -e java` |
| `find . -type f -name "*Service*"` | `fd "Service" --type f` |
| `grep -r "pattern" .` | `rg "pattern"` |
| `grep -rn "pattern" --include="*.java"` | `rg "pattern" -t java` |
| `cat file.java` | `bat file.java` |
| `head -50 file.java` | `bat --line-range=1:50 file.java` |
| `ls -la` | `eza -la --icons` |
| `tree` | `eza --tree --level=2` |
| `du -sh *` | `dust -d 1` |
| `time command` | `hyperfine 'command'` |

**Why?** 10-100x faster, respects .gitignore, better output formatting, parallel processing.

---

## Git Commit Guidelines

Do **NOT** add AI attribution to commit messages:
- No "Generated with Claude Code" lines
- No "Co-Authored-By: Claude" lines

Write clean, conventional commit messages only.

---

## AI Workspace - Task Management & Collaboration

This project uses a structured `.ai/` directory for effective multi-agent collaboration and task management.

### Directory Structure

```
.ai/
├── docs/              # Documentation and guides
│   ├── fixes/         # Fix documentation (issues resolved, security patches)
│   ├── architecture/  # Design decisions, patterns
│   ├── api/           # API documentation
│   └── migration/     # Migration-specific guides
├── plans/             # Migration plans and feature specifications
│   ├── overall-migration.md
│   ├── <service>-migration.md
│   ├── feature-<name>-spec.md
│   └── task-assignments.md
└── journals/          # Progress logs and roadmaps
    ├── current-work/  # Active work logs (one per agent/task)
    ├── completed/     # Completed work records
    ├── daily-standup/ # Daily progress snapshots
    └── roadmap.md     # Future work and priorities
```

### Usage Guidelines for AI Agents

#### 1. Starting a New Task

```bash
# Read the plan
cat .ai/plans/<service-name>-migration.md

# Check for conflicts
ls .ai/journals/current-work/

# Create work log
cat > .ai/journals/current-work/<agent-id>-<task-name>.md
```

**Work log template:**
```markdown
# [Agent ID] - [Task Name]

**Started:** YYYY-MM-DD HH:MM
**Status:** In Progress
**Agent:** <agent-identifier>

## Objective
Brief description of what's being built/migrated

## Progress
- [x] Completed step
- [ ] In progress step
- [ ] Pending step

## Blockers
None / [Describe blockers]

## Decisions Made
- Decision 1: Reasoning

## Next Steps
1. Next task
```

#### 2. Parallel Work (Multiple Agents)

**Rules:**
- Each agent works on a separate service/feature
- Read `.ai/plans/task-assignments.md` to check assignments
- Update your work log in real-time
- Don't modify another agent's assigned service

**Example parallel work:**
- **Agent A:** auth-service (no dependencies)
- **Agent B:** patient-service (no dependencies)
- **Agent C:** doctor-service (no dependencies)

All three can work simultaneously without conflicts.

#### 3. Documentation Standards

**Bug Fixes:** `.ai/docs/fixes/<YYYY-MM-DD>-<description>.md`
```markdown
# Fix: [Short Description]

**Date:** YYYY-MM-DD
**Issue:** Brief description
**Solution:** What was changed
**Files Modified:** List of files
**Testing:** How fix was verified
```

**Architecture Decisions:** `.ai/docs/architecture/<topic>.md`
```markdown
# Architecture: [Topic]

**Decision:** What was decided
**Context:** Why it was needed
**Alternatives:** What else was considered
**Consequences:** Impact of decision
```

#### 4. Completing a Task

```bash
# Finalize work log
vim .ai/journals/current-work/<agent-id>-<task>.md

# Move to completed
mv .ai/journals/current-work/<agent-id>-<task>.md \
   .ai/journals/completed/$(date +%Y-%m-%d)-<task>.md

# Update roadmap
vim .ai/journals/roadmap.md
```

#### 5. Daily Coordination

Create daily standup (if multiple agents working):

```bash
cat > .ai/journals/daily-standup/$(date +%Y-%m-%d)-standup.md
```

**Template:**
```markdown
# Daily Standup - YYYY-MM-DD

## Agent A
**Yesterday:** [Completed work]
**Today:** [Current work]
**Blockers:** [Any blockers]

## Agent B
...

## Decisions Needed
- [List decisions requiring coordination]
```

### Migration-Specific Guidelines

#### Code Templates
Always reference templates in hospital-common-quarkus README:
- Template 1: Entity Migration (minimal changes)
- Template 2: Repository Migration (Panache)
- Template 3: Service Layer (Mutiny Uni/Multi)
- Template 4: REST Controller (JAX-RS)
- Template 5: RabbitMQ Event Publishing
- Template 6: RabbitMQ Event Listening
- Template 7: GraalVM Native Configuration
- Template 8: Service pom.xml

#### Centralized Configurations

**Always use:**
- `CacheKeys.*` for cache key constants
- `RabbitMQConfig.*` for queue/exchange names
- `ConsulConfig.*` for service names
- Event classes from `com.hospital.common.event.*`

**Never:**
- Hard-code cache keys ("patient:123")
- Hard-code queue names ("patient.updates")
- Hard-code service URLs ("http://localhost:8082")

#### Exception Handling

**Always:**
```java
import com.hospital.common.exception.*;

// Use appropriate exception
throw new NotFoundException("Patient", id.toString());
throw new ValidationException("email", "Invalid format");
throw new ConflictException("Patient", "email", email);
```

**Never:**
- Return null for missing resources
- Throw generic `RuntimeException`
- Return error codes in DTOs

#### JWT & Security

**Environment Variables:**
```properties
# Never hard-code secrets
jwt.secret=${JWT_SECRET:default-dev-only}
```

**In Code:**
```java
@Inject
JwtService jwtService;

// Generate token
String token = jwtService.generateToken(userId, email, name, role);
```

#### Health Checks

**Every service must have:**
```java
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {
    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        // Check DB connectivity
    }
}
```

### Workflow Summary

```
┌─────────────────┐
│  Read Plan      │  .ai/plans/<service>-migration.md
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Create Work Log │  .ai/journals/current-work/agent-<task>.md
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Implement       │  Use templates, centralized configs
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Document Fixes  │  .ai/docs/fixes/<date>-<issue>.md
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Complete        │  Move log to .ai/journals/completed/
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Update Roadmap  │  .ai/journals/roadmap.md
└─────────────────┘
```

---

## Quick Reference

### File Locations
- **Master Plan:** `~/.claude/plans/fizzy-munching-toucan.md`
- **Roadmap:** `.ai/journals/roadmap.md`
- **Current Work:** `.ai/journals/current-work/`
- **Templates:** `hospital-common-quarkus/README.md`
- **Fix Docs:** `.ai/docs/fixes/`

### Common Commands
```bash
# View roadmap
bat .ai/journals/roadmap.md

# Check active work
eza -la .ai/journals/current-work/

# View completed work
eza -lt .ai/journals/completed/ | head -10

# Find a fix
rg "JWT" .ai/docs/fixes/

# List all plans
eza .ai/plans/
```

### Service URLs (Consul-based)
Instead of hardcoded URLs, use Stork:
```java
// Good
String url = "stork://patient-service/api/patients";

// Bad
String url = "http://localhost:8082/api/patients";
```

### Cache Keys (Centralized)
```java
// Good
import com.hospital.common.cache.CacheKeys;
String key = CacheKeys.patientById(id.toString());

// Bad
String key = "patient:" + id;
```

### Events (Centralized)
```java
// Good
import com.hospital.common.event.PatientEvents.*;
PatientCreatedEvent event = new PatientCreatedEvent(...);

// Bad
// Creating custom event classes in each service
```

---

## Critical Rules

1. **Never hard-code secrets** - Always use environment variables
2. **Always use centralized configs** - CacheKeys, RabbitMQConfig, Events
3. **Document fixes** - Create .md file in `.ai/docs/fixes/`
4. **Update work logs** - Keep `.ai/journals/current-work/` current
5. **Check for conflicts** - Read task assignments before starting
6. **Use templates** - Follow migration templates for consistency
7. **Native compilation** - Add `@RegisterForReflection` for event classes
8. **Health checks** - Every service needs liveness + readiness probes

---

## Success Criteria

Before marking a service migration complete:
- [ ] All endpoints work reactively (Uni/Multi)
- [ ] Events publish to RabbitMQ
- [ ] Cache works with Redis
- [ ] Tests pass (unit + integration)
- [ ] Native build succeeds
- [ ] Health checks return 200 OK
- [ ] Service registers with Consul
- [ ] Work log moved to completed/
- [ ] Roadmap updated

---

**Remember:** This workspace enables effective collaboration. Keep it organized, and future agents (including your future self) will benefit!
