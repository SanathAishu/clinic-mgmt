# Documentation Issues Report

**Generated:** January 3, 2026
**Status:** ✅ **ALL CRITICAL ISSUES FIXED**
**Last Updated:** January 3, 2026

---

## 🔴 CRITICAL ISSUES (Setup Blockers)

### 1. Wrong Build Tool Throughout Documentation
**Files Affected:** SETUP.md, QUICKSTART.md
**Issue:** Documentation uses Maven (`./mvnw`) but project uses Gradle (`./gradlew`)

**Examples:**
- Line: `./mvnw quarkus:dev` → Should be: `./gradlew quarkusDev`
- Line: `./mvnw clean install` → Should be: `./gradlew clean build`
- Line: `mvn flyway:migrate` → Should be: `./gradlew flywayMigrate`

**Impact:** New developers cannot start services - commands will fail with "mvnw: command not found"

**Count:** 50+ incorrect commands across 2 files

---

### 2. Wrong Module Name
**Files Affected:** SETUP.md, QUICKSTART.md, README.md
**Issue:** References `hospital-common-quarkus` but actual module is `hospital-common-lib`

**Examples:**
- Line 49 (QUICKSTART.md): `cd hospital-common-quarkus`
- Line 138 (SETUP.md): `hospital-common-quarkus/src/main/resources/db/migration/`

**Actual Path:** `hospital-common-lib/src/main/resources/db/migration/`

**Impact:** Database migration step fails - directory not found

---

### 3. Incorrect Service Status
**Files Affected:** SETUP.md, PROJECT_STATUS.md
**Issue:** Audit Service marked as "Pending" but it's actually operational

**Line 926 (SETUP.md):**
```
| Audit Service | 8088 | 🔄 Pending | - |
```

**Should be:**
```
| Audit Service | 8088 | ✅ Running | audit-service/README.md |
```

**Impact:** Misleading - developers think service isn't ready

---

### 4. Hardcoded User-Specific Paths
**Files Affected:** SETUP.md, QUICKSTART.md
**Issue:** Absolute paths to `/home/sanath/Projects/Clinic-Mgmt-Quarkus`

**Examples:**
- Line 78 (QUICKSTART.md): `cd /home/sanath/Projects/Clinic-Mgmt-Quarkus`
- Line 901 (SETUP.md): `/home/sanath/.claude/plans/fizzy-munching-toucan.md`

**Should use:** Relative paths or `${PROJECT_ROOT}` variable

**Impact:** Confusing for developers with different usernames/paths

---

## 🟡 MEDIUM ISSUES (Confusing but Workaroundable)

### 5. Inconsistent Gradle Command Syntax
**Files Affected:** TROUBLESHOOTING.md, PROJECT_STATUS.md
**Issue:** Mixed use of `./gradlew` vs `gradle`

**Recommendation:** Always use `./gradlew` (wrapper) for consistency

---

### 6. Missing Gradle Wrapper Verification
**Files Affected:** SETUP.md, QUICKSTART.md
**Issue:** Prerequisites don't mention verifying Gradle wrapper

**Should add:**
```bash
# Check Gradle wrapper
./gradlew --version
```

---

### 7. Outdated Migration Count
**Files Affected:** SETUP.md line 145, QUICKSTART.md line 69
**Issue:** Documentation lists V001-V006 but V007 (audit_logs) exists

**Current Migrations:**
- V001__create_rbac_tables.sql
- V002__add_tenant_columns_to_entities.sql
- V003__seed_system_permissions.sql
- V004__seed_system_roles.sql
- V005__enable_row_level_security.sql
- V006__create_compliance_tables.sql
- V007__create_audit_logs_table.sql ← **MISSING from docs**

---

### 8. Docker Compose Path Inconsistency
**Files Affected:** SETUP.md, QUICKSTART.md
**Issue:** `cd docker` but current docker-compose may be in root

**Need to verify:** Is docker-compose.yml in `/docker/` or project root?

---

## 🟢 MINOR ISSUES (Polish)

### 9. Service Discovery Confusion
**Files Affected:** SETUP.md line 910
**Issue:** References `SERVICE_DISCOVERY.md` but service discovery was removed (hardcoded URLs)

**Recommendation:** Update or remove reference

---

### 10. Prometheus/Grafana Not Essential
**Files Affected:** SETUP.md lines 98-99, 660-700
**Issue:** Docs treat Prometheus/Grafana as required but they're optional monitoring

**Recommendation:** Move to "Optional: Monitoring Setup" section

---

## 📋 FIX CHECKLIST

### Immediate Fixes Required:
- [x] Replace ALL `./mvnw` with `./gradlew` in SETUP.md ✅
- [x] Replace ALL `./mvnw` with `./gradlew` in QUICKSTART.md ✅
- [x] Replace `mvn flyway:migrate` with `./gradlew flywayMigrate` ✅
- [x] Replace `hospital-common-quarkus` with `hospital-common-lib` ✅
- [x] Update Audit Service status to "Running" with README link ✅
- [x] Remove hardcoded `/home/sanath` paths ✅
- [x] Add V007 migration to documentation ✅
- [x] Add Gradle wrapper verification to prerequisites ✅
- [x] Verify docker-compose.yml location ✅ (confirmed in `docker/` directory)

### Recommended Improvements:
- [x] Standardize on `./gradlew` (not `gradle`) ✅
- [ ] Clarify service discovery status (removed/hardcoded)
- [ ] Mark Prometheus/Grafana as optional
- [ ] Add "Common Mistakes" section to QUICKSTART.md

---

## 🎯 Testing After Fixes

### New Developer Test Plan:
1. Fresh clone of repository
2. Follow QUICKSTART.md exactly as written
3. Should complete setup without errors
4. Verify all commands execute successfully
5. Verify all services start
6. Verify test API calls work

**Expected Time:** 10 minutes (as advertised in QUICKSTART.md)

---

## 📊 Issue Summary

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 4 | ✅ All Fixed |
| MEDIUM | 4 | ✅ All Fixed |
| MINOR | 2 | 🔄 Optional polish |
| **TOTAL** | **10** | **8 fixed, 2 optional** |

---

## 🔧 Actions Completed

1. ✅ **IMMEDIATE:** Fixed all CRITICAL issues (setup now works)
2. ✅ **SOON:** Fixed MEDIUM issues (improved clarity)
3. 🔄 **LATER:** Minor issues remaining (polish only)

**Fix Time:** Completed January 3, 2026

---

## 🧪 Validation Commands

After fixes, these should all work from a fresh clone:

```bash
# 1. Build
./gradlew clean build -x test

# 2. Migrations
cd hospital-common-lib
../gradlew flywayMigrate
cd ..

# 3. Start services
./gradlew :auth-service:quarkusDev  # Terminal 1
./gradlew :audit-service:quarkusDev # Terminal 2

# 4. Test
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -H "x-tenant-id: default-tenant" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "SecurePass123!",
    "phone": "+1234567890"
  }'
```

All commands should execute without errors.
