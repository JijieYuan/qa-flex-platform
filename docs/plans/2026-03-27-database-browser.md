# Database Browser Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a simple database browser page under System Settings to view local database tables with Element Plus native table, search, sorting, refresh, and pagination.

**Architecture:** Keep the feature lightweight. Add a backend whitelist-driven table browser API and a frontend page under System Settings that uses a small reusable database browser component. Do not add editing, SQL execution, or complex filters.

**Tech Stack:** Spring Boot, MyBatis-Plus/JdbcTemplate for local DB reads, Vue 3, Element Plus

---

### Task 1: Define backend response models

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/entity/database/DatabaseTableOption.java`
- Create: `backend/src/main/java/com/data/collection/platform/entity/database/DatabaseTableRowsResponse.java`

**Step 1: Write the failing test**

Add controller/service tests that expect:
- table list returns structured options
- rows response includes `columns`, `rows`, `total`, `page`, `size`

**Step 2: Run test to verify it fails**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserControllerTest test
```

Expected:
- fail because models/controller do not exist yet

**Step 3: Write minimal implementation**

Create simple DTO/record classes:
- `DatabaseTableOption`
- `DatabaseTableRowsResponse`

**Step 4: Run test to verify it passes**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserControllerTest test
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/data/collection/platform/entity/database
git commit -m "feat: add database browser response models"
```

### Task 2: Add backend whitelist metadata service

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/service/DatabaseBrowserService.java`
- Create: `backend/src/test/java/com/data/collection/platform/service/DatabaseBrowserServiceTest.java`

**Step 1: Write the failing test**

Test:
- allowed table list is returned
- disallowed table throws business exception
- disallowed sort field throws business exception

**Step 2: Run test to verify it fails**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserServiceTest test
```

**Step 3: Write minimal implementation**

Implement:
- local whitelist table definitions
- searchable fields per table
- sortable fields per table

**Step 4: Run test to verify it passes**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserServiceTest test
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/data/collection/platform/service/DatabaseBrowserService.java backend/src/test/java/com/data/collection/platform/service/DatabaseBrowserServiceTest.java
git commit -m "feat: add database browser whitelist service"
```

### Task 3: Add backend controller for database browser

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/controller/DatabaseBrowserController.java`
- Create: `backend/src/test/java/com/data/collection/platform/controller/DatabaseBrowserControllerTest.java`

**Step 1: Write the failing test**

Cover:
- `GET /api/database-browser/tables`
- `GET /api/database-browser/rows`

**Step 2: Run test to verify it fails**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserControllerTest test
```

**Step 3: Write minimal implementation**

Expose:
- table options endpoint
- paged row query endpoint

**Step 4: Run test to verify it passes**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserControllerTest test
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/data/collection/platform/controller/DatabaseBrowserController.java backend/src/test/java/com/data/collection/platform/controller/DatabaseBrowserControllerTest.java
git commit -m "feat: add database browser controller"
```

### Task 4: Implement local database row query

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/DatabaseBrowserService.java`
- Test: `backend/src/test/java/com/data/collection/platform/service/DatabaseBrowserServiceTest.java`

**Step 1: Write the failing test**

Cover:
- pagination
- keyword search
- sorting
- dynamic columns

**Step 2: Run test to verify it fails**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserServiceTest test
```

**Step 3: Write minimal implementation**

Implement read-only SQL using local DB:
- select columns
- select count
- select rows with limit/offset

**Step 4: Run test to verify it passes**

Run:
```bash
mvn -q -Dtest=DatabaseBrowserServiceTest test
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/data/collection/platform/service/DatabaseBrowserService.java backend/src/test/java/com/data/collection/platform/service/DatabaseBrowserServiceTest.java
git commit -m "feat: add paged local database browsing"
```

### Task 5: Add frontend API definitions

**Files:**
- Modify: `frontend/src/api.ts`

**Step 1: Write the failing test**

If there is existing API test coverage, add:
- list tables request type
- row data request type

**Step 2: Run test to verify it fails**

Run:
```bash
npm run test
```

**Step 3: Write minimal implementation**

Add:
- table option types
- row response types
- `api.getDatabaseTables()`
- `api.getDatabaseTableRows()`

**Step 4: Run test to verify it passes**

Run:
```bash
npm run test
```

**Step 5: Commit**

```bash
git add frontend/src/api.ts
git commit -m "feat: add frontend database browser api"
```

### Task 6: Build reusable frontend database browser component

**Files:**
- Create: `frontend/src/components/DatabaseBrowserView.vue`

**Step 1: Write the failing test**

Cover:
- loads table options
- loads selected table rows
- sends search/sort/pagination params

**Step 2: Run test to verify it fails**

Run:
```bash
npm run test
```

**Step 3: Write minimal implementation**

Build with Element Plus native components:
- `el-select`
- `el-input`
- `el-button`
- `el-table`
- `el-pagination`

**Step 4: Run test to verify it passes**

Run:
```bash
npm run test
```

**Step 5: Commit**

```bash
git add frontend/src/components/DatabaseBrowserView.vue
git commit -m "feat: add reusable database browser view"
```

### Task 7: Add System Settings menu entry and page wiring

**Files:**
- Modify: `frontend/src/App.vue`

**Step 1: Write the failing test**

If page-level test coverage exists, verify:
- System Settings contains `数据库查看`
- clicking it renders the new browser component

**Step 2: Run test to verify it fails**

Run:
```bash
npm run test
```

**Step 3: Write minimal implementation**

Add:
- left menu item
- page routing condition
- mount `DatabaseBrowserView`

**Step 4: Run test to verify it passes**

Run:
```bash
npm run test
```

**Step 5: Commit**

```bash
git add frontend/src/App.vue
git commit -m "feat: add database browser page to system settings"
```

### Task 8: Final verification

**Files:**
- Verify only

**Step 1: Run backend tests**

```bash
mvn -q -Dtest=DatabaseBrowserServiceTest,DatabaseBrowserControllerTest test
```

Expected:
- PASS

**Step 2: Run backend compile**

```bash
mvn -q -DskipTests compile
```

Expected:
- PASS

**Step 3: Run frontend build**

```bash
npm run build
```

Expected:
- PASS

**Step 4: Manual verification**

Check:
- System Settings shows `数据库查看`
- can switch tables
- can sort
- can search
- can paginate

**Step 5: Commit**

```bash
git add .
git commit -m "feat: add local database browser module"
```
