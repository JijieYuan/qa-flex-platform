# Realtime Mirror Workspace Shell Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a reusable realtime workspace shell that keeps table pages fast on first paint while silently refreshing mirror-backed data in the background.

**Architecture:** Keep base record/statistic tables focused on rendering. Add a reusable realtime shell layer above them that exposes freshness state, manual refresh, and silent-refresh orchestration. Split backend endpoints into read-only query endpoints plus explicit refresh/status endpoints so weak-network behavior is predictable.

**Tech Stack:** Vue 3, Element Plus, Spring Boot, JdbcTemplate/MyBatis, PostgreSQL mirror tables.

---

### Task 1: Backend realtime status models and controller endpoints

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/entity/RealtimeMirrorStatusResponse.java`
- Modify: `backend/src/main/java/com/data/collection/platform/controller/CodeReviewController.java`
- Modify: `backend/src/main/java/com/data/collection/platform/controller/StatisticBoardController.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/CodeReviewIllegalRecordService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/statistics/SystemTestDefectSummaryBoardService.java`

**Step 1:** Add a reusable response model carrying `status`, `message`, `lastSyncedAt`, `running`, and optional `taskType`.

**Step 2:** Refactor code review/statistic services so normal data reads do not force on-demand refresh.

**Step 3:** Add explicit refresh methods that trigger on-demand mirror refresh for the page-specific table set.

**Step 4:** Add explicit status methods that derive freshness from current task/log state.

### Task 2: Frontend reusable realtime shell

**Files:**
- Create: `frontend/src/components/realtime/RealtimeDataShell.vue`
- Create: `frontend/src/types/realtime.ts`
- Modify: `frontend/src/api.ts`

**Step 1:** Create a reusable shell component that renders title/meta, freshness state, refresh button, and a content slot.

**Step 2:** Add API methods for code review refresh/status and statistic board refresh/status.

**Step 3:** Add client-side state types for realtime shell payloads.

### Task 3: Integrate code review illegal records page

**Files:**
- Modify: `frontend/src/views/CodeReviewIllegalRecordsView.vue`

**Step 1:** Wrap the page in `RealtimeDataShell`.

**Step 2:** Load table data first, then load status, then trigger a silent refresh once per page entry.

**Step 3:** Keep old data visible during refresh and show non-blocking success/failure feedback.

### Task 4: Integrate statistic board page

**Files:**
- Modify: `frontend/src/components/StatisticBoardView.vue`

**Step 1:** Wrap board content in `RealtimeDataShell` when board supports realtime mirror refresh.

**Step 2:** Use board-specific refresh/status APIs for `system-test-defect-summary`.

**Step 3:** Ensure detail drawers continue to work and page load remains fast.

### Task 5: Verification

**Files:**
- Modify: any impacted backend tests if needed

**Step 1:** Run frontend build.

**Step 2:** Run backend tests.

**Step 3:** Verify local APIs for read/status/refresh.

**Step 4:** Verify code review illegal records and system test defect summary still load with weak-network-friendly behavior.
