# Two Board Gap Fixes Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close the remaining functional gaps for `system-test-defect-summary` and `code-review-illegal-records` without introducing rule editing.

**Architecture:** Keep the current `ODS -> Fact -> Board -> UI` structure. Fix the remaining gaps by reusing existing fact fields first, then tightening service aggregation logic, and finally aligning the QA-facing rule explanation entry behavior.

**Tech Stack:** Spring Boot, JdbcTemplate, Vue 3, Element Plus, JUnit 5, MockMvc

---

### Task 1: Align the target scope in docs

**Files:**
- Modify: `d:/projects/data_collection_platform/docs/current-state/2026-04-07-current-project-state.md`
- Modify: `d:/projects/data_collection_platform/docs/plans/2026-04-08-data-decoupled-statistics-architecture-design.md`

**Steps:**
1. Record the remaining gaps confirmed against the handover docs.
2. State that this round focuses only on the two implemented tables.
3. Mark the expected fixes:
   - `已修复/未更新` real semantics
   - `新发议题` real semantics
   - fuller code review illegal-rule coverage
   - visible rule explanation entry even when explanation fetch fails

### Task 2: Fix system-test summary business semantics

**Files:**
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/service/statistics/SystemTestDefectSummaryBoardService.java`
- Modify: `d:/projects/data_collection_platform/backend/src/test/java/com/data/collection/platform/controller/StatisticBoardControllerTest.java`
- Modify: `d:/projects/data_collection_platform/backend/src/test/java/com/data/collection/platform/service/statistics/SystemTestDefectSummaryRuleExplanationTest.java`

**Steps:**
1. Replace the current `新发议题 = !历史遗留` approximation with an explicit board-level definition and explanation consistent with current fact capability.
2. Fix `已修复/未更新` so it no longer pretends to be pure fixed count.
3. Decide and encode whether illegal issues remain included or excluded, then make explanation text match behavior.
4. Update tests to assert the corrected column and explanation behavior.

### Task 3: Expand code-review illegal rules

**Files:**
- Modify: `d:/projects/data_collection_platform/backend/src/main/java/com/data/collection/platform/service/CodeReviewIllegalRecordService.java`
- Modify: `d:/projects/data_collection_platform/backend/src/test/java/com/data_collection/platform/controller/CodeReviewControllerTest.java`

**Steps:**
1. Keep current missing-field checks.
2. Add the next layer of illegal checks that can be supported by current fact/source data without inventing new tables.
3. Make filter option lists derive from illegal rows so filters are not misleading.
4. Update rule explanation text and tests.

### Task 4: Fix rule explanation entry behavior

**Files:**
- Modify: `d:/projects/data_collection_platform/frontend/src/components/StatisticBoardView.vue`
- Modify: `d:/projects/data_collection_platform/frontend/src/views/CodeReviewIllegalRecordsView.vue`

**Steps:**
1. Stop hiding the rule explanation button behind `v-if="ruleExplanation?.supported"`.
2. Keep the button visible and show a clear message when explanation loading fails or is unsupported.
3. Preserve current QA-friendly explanation layout.

### Task 5: Verify and sync docs

**Files:**
- Modify: `d:/projects/data_collection_platform/docs/current-state/2026-04-07-current-project-state.md`

**Steps:**
1. Run backend compile and targeted tests.
2. Run frontend build.
3. Update current-state doc with what is truly fixed in this round and what still remains.
