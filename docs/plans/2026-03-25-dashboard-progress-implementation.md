# GitLab Mirror Dashboard And Progress Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a real home dashboard, a dedicated entry into the GitLab mirror settings page, visual sync progress, and running-only auto refresh behavior.

**Architecture:** Keep the current single Vue application but split it into two views with local state. Expose sync runtime progress from the Spring Boot sync service through the existing status endpoint so the frontend can render progress without inventing state locally.

**Tech Stack:** Vue 3, Element Plus, Spring Boot 3, Java 21, JUnit 5, Mockito, MockMvc

---

### Task 1: Add runtime sync progress model

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/entity/SyncProgress.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabMirrorSyncService.java`

**Step 1:** Add a `SyncProgress` model with phase, totalTables, completedTables, syncedRecords, currentTable, startedAt.

**Step 2:** Store an in-memory current progress object in `GitlabMirrorSyncService`.

**Step 3:** Update progress when sync starts, after each table, and when sync ends.

**Step 4:** Reset progress when sync completes or fails.

### Task 2: Expose progress via status API

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/entity/MirrorStatusResponse.java`
- Modify: `backend/src/main/java/com/data/collection/platform/controller/GitlabSyncController.java`

**Step 1:** Extend the response record with a `SyncProgress progress` field.

**Step 2:** Return the service progress in `/api/gitlab-sync/status`.

### Task 3: Add backend tests

**Files:**
- Create: `backend/src/test/java/com/data/collection/platform/controller/GitlabSyncControllerTest.java`

**Step 1:** Mock the controller dependencies.

**Step 2:** Verify `/api/gitlab-sync/status` returns progress data in JSON.

**Step 3:** Verify when there is no running sync the endpoint still returns a valid shape.

### Task 4: Create dashboard-first frontend flow

**Files:**
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/styles.css`

**Step 1:** Add a simple app state for `dashboard` vs `settings`.

**Step 2:** Make `dashboard` the default page.

**Step 3:** Add a clear action to enter the settings page.

**Step 4:** Add a back action from settings to dashboard.

### Task 5: Render sync progress and fix polling behavior

**Files:**
- Modify: `frontend/src/App.vue`

**Step 1:** Add a progress card on dashboard.

**Step 2:** Add a progress section in settings.

**Step 3:** Compute progress percentage from backend progress.

**Step 4:** Replace unconditional `setInterval` with running-only polling.

### Task 6: Maintain project progress documentation

**Files:**
- Create: `docs/project-progress.md`

**Step 1:** Record completed setup work.

**Step 2:** Record this dashboard/progress milestone.

**Step 3:** Note current limitations and next recommended steps.
