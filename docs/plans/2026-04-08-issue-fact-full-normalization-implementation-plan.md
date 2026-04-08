# Issue Fact Full Normalization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Land the full `issue_fact` business normalization model so system-test issue statistics read only fact fields for severity, exclusion, fixed status, delay, illegal rules, SLA, special level-one classification, module expansion, and legacy status.

**Architecture:** Keep the existing `ODS -> Fact -> Board` direction, but make `issue_fact` the single business truth source for issue rules. `FactBuildService` performs normalization from `ods_gitlab_*` plus issue notes and a lightweight phase calendar table, while `SystemTestDefectSummaryBoardService` degrades into a pure query-and-aggregate service.

**Tech Stack:** Spring Boot, JdbcTemplate, MyBatis-Plus, PostgreSQL, JUnit 5, MockMvc

---

### Task 1: Expand the documented target model

**Files:**
- Modify: `d:/projects/data_collection_platform/docs/plans/2026-04-08-data-decoupled-statistics-architecture-design.md`
- Modify: `d:/projects/data_collection_platform/docs/current-state/2026-04-07-current-project-state.md`

**Step 1: Update the architecture doc**

- Add the full normalized issue scope:
  - severity aliases: `P1/P2/P3/SUGGESTION`
  - exclusion rules
  - fixed rules
  - reason-category normalization
  - delay-reason normalization
  - regression/crash/other-level1 flags
  - illegal rules
  - SLA fields
  - module multi-value handling
  - `is_legacy`
  - `testing_phase_calendar`

**Step 2: Update the current-state doc**

- Record current reality before implementation:
  - fields already landed
  - fields still pending
  - board service still not fully reduced to fact-only decisions

**Step 3: Save both docs**

- Make sure wording clearly separates:
  - approved target design
  - current implemented state

### Task 2: Expand the formal schema

**Files:**
- Modify: `d:/projects/data_collection_platform/backend/src/main/resources/schema.sql`

**Step 1: Add missing issue_fact columns**

- Add or finalize:
  - `primary_module_name`
  - `module_names`
  - `is_level1_other`
  - `is_response_delayed`
  - `resolve_deadline_at`
  - `is_resolve_delayed`
  - `is_legacy`

**Step 2: Add the lightweight phase calendar table**

- Create `testing_phase_calendar`
- Include:
  - `project_id`
  - `testing_phase`
  - `phase_start_at`
  - `phase_end_at`
  - `enabled`
  - `created_at`
  - `updated_at`

**Step 3: Add indexes**

- Add only high-value indexes:
  - `issue_fact(project_id, severity_level, is_excluded, is_fixed)`
  - `issue_fact(issue_state, is_legacy, testing_phase)`
  - `testing_phase_calendar(project_id, testing_phase, enabled)`

### Task 3: Align entities and mappers

**Files:**
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/entity/IssueFact.java`
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/mapper/IssueFactMapper.java`
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/service/DatabaseBrowserService.java`

**Step 1: Add IssueFact fields**

- Match all schema columns that are used by build/query logic

**Step 2: Expand IssueFact upsert**

- Insert/update all new normalized business fields

**Step 3: Update DB browser metadata**

- Expose the most useful issue_fact columns for inspection

### Task 4: Centralize normalization rules

**Files:**
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/service/IssueFactNormalizationRules.java`
- Test: `d:/projects/data_collection_platform/backend/src/test/java/com/data/collection/platform/service/IssueFactNormalizationRulesTest.java`

**Step 1: Write failing unit tests**

- Cover:
  - severity mapping
  - exclusion rules
  - fixed rules
  - reason category mapping
  - delay reason mapping
  - regression/crash/other-level1
  - illegal rules
  - response detection
  - SLA day normalization

**Step 2: Implement the normalization helpers**

- Keep all business aliases in one class
- Do not scatter rule strings across services

**Step 3: Run the unit tests**

- Verify the normalization class is the sole rule mapping entry point

### Task 5: Build full issue facts from ODS + notes + phase calendar

**Files:**
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/service/FactBuildService.java`
- Test: `d:/projects/data_collection_platform/backend/src/test/java/com/data/collection/platform/controller/FactBuildControllerTest.java`

**Step 1: Extend issue source SQL**

- Aggregate:
  - labels
  - module labels
  - issue notes
  - phase calendar match

**Step 2: Populate all normalized fields**

- Write all final fact fields in `mapIssueFact`

**Step 3: Preserve incremental behavior**

- Continue using `ods_updated_at`
- Keep incremental rebuild working

**Step 4: Add tests**

- Verify rebuild succeeds with the new issue_fact shape

### Task 6: Reduce the board service to a pure aggregator

**Files:**
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/service/statistics/SystemTestDefectSummaryBoardService.java`
- Test: `d:/projects/data_collection_platform/backend/src/test/java/com/data/collection/platform/service/statistics/SystemTestDefectSummaryRuleExplanationTest.java`
- Test: `d:/projects/data_collection_platform/backend/src/test/java/com/data/collection/platform/controller/StatisticBoardControllerTest.java`

**Step 1: Expand fact query projection**

- Read the normalized fact fields directly

**Step 2: Remove duplicated business logic**

- Use fact booleans and normalized enums instead of recomputing rules

**Step 3: Update rule explanation**

- Explain the user-facing meaning from fact fields
- Keep QA-readable language

**Step 4: Run board tests**

- Ensure stats and explanation endpoints still work

### Task 7: Apply schema and verify local runtime

**Files:**
- Modify: `d:/projects/data_collection_platform/docs/current-state/2026-04-07-current-project-state.md`

**Step 1: Apply schema to local PostgreSQL**

Run:

```powershell
Get-Content d:\projects\data_collection_platform\backend\src\main\resources\schema.sql | docker exec -i qa-flex-postgres psql -U postgres -d data_collection_platform
```

**Step 2: Rebuild facts**

Run:

```powershell
Invoke-RestMethod -Method Post "http://localhost:18080/api/facts/rebuild?scope=issue&full=true"
```

**Step 3: Verify runtime**

- Check:
  - `GET /api/statistic-boards/system-test-defect-summary`
  - `GET /api/statistic-boards/system-test-defect-summary/rule-explanation`

**Step 4: Update current-state doc with actual completion**

- Record what is truly implemented
- Record any remaining gaps honestly

### Task 8: Compile and test before close-out

**Files:**
- No file changes; verification only

**Step 1: Run compile**

```powershell
mvn -DskipTests compile
```

**Step 2: Run focused tests**

```powershell
mvn "-Dtest=IssueFactNormalizationRulesTest,FactBuildControllerTest,StatisticBoardControllerTest,SystemTestDefectSummaryRuleExplanationTest" test
```

**Step 3: Review diff**

- Check that docs and code match
- Confirm no duplicate rule logic remains in board service

**Step 4: Final doc sync**

- Make sure:
  - architecture plan is current
  - current-state doc is current
## 最新进展补充

本计划中的两项基础治理已经进入代码：

- `FactBuildService` 已补 `batch upsert`
  - `issue_fact` / `merge_request_fact` 当前按 `200` 条分批写入
- `IssueFactNormalizationRules` 已按领域拆分
  - 标签规则、分类规则、SLA 规则、历史遗留规则已拆成独立类

因此，当前后续重点可以从“先把结构理顺”转向：

1. 继续补真实项目标签别名
2. 导入真实评论模板数据
3. 用真实阶段日历验证 `is_legacy`
