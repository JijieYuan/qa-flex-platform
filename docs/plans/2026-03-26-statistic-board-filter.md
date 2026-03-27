# Statistic Board Filter Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add metadata-driven, reusable filtering to the abstract statistic board so users can build AND/OR conditions by field type and apply them consistently to board queries, details, and exports.

**Architecture:** Extend statistic board definition metadata with filter operators and typed filter groups, parse and validate filter JSON in the abstract backend service, and render a compact Element Plus filter builder in the shared frontend statistic board component. The first rollout only wires the existing `mirror-table-overview` board to the new framework using real GitLab mirror data.

**Tech Stack:** Spring Boot, MyBatis-Plus, PostgreSQL, Vue 3, Element Plus, Vitest, JUnit 5

---

### Task 1: Add backend filter metadata and request models

**Files:**
- Modify: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\entity\statistics\StatisticFilterField.java`
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\entity\statistics\StatisticFilterCondition.java`
- Create: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\entity\statistics\StatisticFilterGroup.java`

**Step 1: Write failing tests**
- Extend backend tests to expect operator metadata and parsed filter group support.

**Step 2: Implement metadata models**
- Add operators to `StatisticFilterField`
- Add filter condition/group records for typed request parsing

**Step 3: Run focused backend tests**
- Run targeted statistic controller/service tests

### Task 2: Add abstract backend filter parsing and validation

**Files:**
- Modify: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\service\statistics\AbstractStatisticBoardService.java`
- Modify: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\controller\StatisticBoardController.java`

**Step 1: Write failing tests**
- Add tests for invalid field/operator handling and request parsing

**Step 2: Implement parsing**
- Accept `filtersJson`
- Parse to condition group
- Validate against board definition

**Step 3: Re-run backend tests**

### Task 3: Wire real filter execution into mirror table board

**Files:**
- Modify: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\service\statistics\MirrorTableOverviewBoardService.java`
- Modify: `D:\projects\data_collection_platform\backend\src\main\java\com\data\collection\platform\mapper\MirrorTableOverviewMapper.java`

**Step 1: Write failing tests**
- Add controller tests for text/number/time filtering

**Step 2: Implement real filtering**
- Build SQL for table name, numeric aggregates, and date/time conditions
- Reuse same filters in board/detail/export paths

**Step 3: Run backend tests**

### Task 4: Add frontend filter builder to abstract statistic board

**Files:**
- Modify: `D:\projects\data_collection_platform\frontend\src\api.ts`
- Modify: `D:\projects\data_collection_platform\frontend\src\components\StatisticBoardView.vue`
- Modify: `D:\projects\data_collection_platform\frontend\src\styles.css`
- Create: `D:\projects\data_collection_platform\frontend\src\components\statistic-board-filters.ts`

**Step 1: Write failing tests**
- Add frontend tests for condition rows and request serialization

**Step 2: Implement filter builder**
- Render compact toolbar filter rows
- Add field/operator/value linkage
- Support AND/OR switching

**Step 3: Run frontend tests and build**

### Task 5: Verify first-board integration end to end

**Files:**
- Modify: `D:\projects\data_collection_platform\backend\src\test\java\com\data\collection\platform\controller\StatisticBoardControllerTest.java`
- Create: `D:\projects\data_collection_platform\frontend\src\components\statistic-board-filters.test.ts`

**Step 1: Add final assertions**
- Board query with filters
- Detail query with inherited filters
- Export with inherited filters

**Step 2: Run verification**
- `mvn -q "-Dtest=StatisticBoardControllerTest" test`
- `npm run test`
- `npm run build`

**Step 3: Review and clean up**
- Ensure no page-specific filter logic leaked outside the abstract component
