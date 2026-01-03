# Documentation

This directory contains all documentation related to the Quarkus migration project.

## Subdirectories

### `fixes/`
Documentation for bug fixes, security patches, and issues resolved.

**Contents:**
- Security fixes (JWT, authentication, etc.)
- Build fixes (dependency conflicts, compilation errors)
- Runtime fixes (performance issues, memory leaks)
- Configuration fixes

**Naming convention:** `<YYYY-MM-DD>-<short-description>.md`

**Example:**
- `2026-01-03-jwt-environment-variables.md`
- `security-hardcoded-secrets.md`
- `build-parent-pom-modules.md`

### `architecture/` (to be created)
Design decisions, patterns, and architectural documentation.

### `api/` (to be created)
API endpoint documentation and changes.

### `migration/` (to be created)
Migration-specific guides and notes.

## Quick Reference

```bash
# View all fixes
ls -la .ai/docs/fixes/

# Search for specific fix
grep -r "JWT" .ai/docs/fixes/
```
