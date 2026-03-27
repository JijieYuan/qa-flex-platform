# GitLab Sync Task Governance Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace log-driven import state handling with a task-table-based sync governance model that supports dedupe, scope serialization, cancellation, timeout recovery, bounded compensation, and stable frontend polling.

**Architecture:** Add a lightweight `gitlab_sync_tasks` table as the only task state source of truth. Refactor sync execution, scheduler decisions, and the import page to consume task status instead of inferring state from logs or in-memory flags. Preserve `gitlab_sync_logs` for audit only.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, PostgreSQL, Vue 3, Element Plus, JUnit 5, MockMvc.

---

### Task 1: Add task entity, mapper, schema, and enums

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/entity/GitlabSyncTask.java`
- Create: `backend/src/main/java/com/data/collection/platform/entity/SyncTaskStatus.java`
- Create: `backend/src/main/java/com/data/collection/platform/entity/SyncTriggerType.java`
- Create: `backend/src/main/java/com/data/collection/platform/mapper/GitlabSyncTaskMapper.java`
- Modify: `backend/src/main/resources/schema.sql`

**Steps:**
1. Write failing mapper/entity-oriented tests for task persistence and status mapping.
2. Add schema for `gitlab_sync_tasks` including indexes and version column.
3. Create Lombok entity and enums.
4. Add mapper interface.
5. Run targeted backend tests.

### Task 2: Build task service as the only state source

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/service/GitlabSyncTaskService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/entity/MirrorStatusResponse.java`
- Modify: `backend/src/main/java/com/data/collection/platform/controller/GitlabSyncController.java`

**Steps:**
1. Write failing tests for task creation, dedupe, active-task lookup, and cancellation request.
2. Implement scope key and dedupe key generation.
3. Implement create-or-return-existing logic for manual and webhook triggers.
4. Expose latest task in status response.
5. Remove controller dependence on running log as current state source.

### Task 3: Refactor sync executor to task-driven execution

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabMirrorSyncService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabSyncLogService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabConfigService.java`

**Steps:**
1. Write failing tests for task lifecycle: pending -> running -> success/failed/cancelled.
2. Remove `AtomicBoolean` as the authoritative state model.
3. Use task service for task claiming, start, heartbeat, finish, cancellation checks, and queued follow-up execution.
4. Keep logs as audit-only side effects.
5. Verify sync timestamps still update correctly.

### Task 4: Add cancellation, timeout recovery, and heartbeat governance

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabMirrorSyncService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/controller/GitlabSyncController.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabCompensationScheduler.java`

**Steps:**
1. Write failing tests for cancel request, stale heartbeat timeout, and compensation gating.
2. Add cancel endpoint.
3. Add safe cancellation checkpoints in full/incremental/compensation execution.
4. Add heartbeat update logic and stale task timeout recovery.
5. Change compensation scheduler to use task table state and cooldown/backoff logic only.

### Task 5: Replace frontend polling and add cancel interaction

**Files:**
- Modify: `frontend/src/api.ts`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/styles.css`

**Steps:**
1. Write failing frontend tests for polling behavior and cancel action contract.
2. Extend API types to include task state.
3. Switch page polling to task-based terminal/non-terminal states.
4. Add cancel button and status feedback.
5. Remove or simplify any logic that derives run state from logs.

### Task 6: Remove obsolete code and tests tied to the old model

**Files:**
- Modify/Delete: old tests that assert log-driven running-state behavior
- Modify/Delete: stale reconciliation helpers that only mutate logs

**Steps:**
1. Identify tests asserting old behavior.
2. Delete or rewrite them to task-table behavior.
3. Remove dead code paths that infer current state from logs.
4. Run targeted tests to confirm no stale assumptions remain.

### Task 7: Add stable real-chain verification coverage

**Files:**
- Modify: `backend/src/test/java/com/data/collection/platform/service/GitlabMirrorSyncServiceTest.java`
- Modify: `backend/src/test/java/com/data/collection/platform/service/GitlabCompensationSchedulerTest.java`
- Modify: `backend/src/test/java/com/data/collection/platform/controller/GitlabSyncControllerTest.java`
- Modify: `backend/src/test/java/com/data/collection/platform/controller/GitlabMirrorStressIntegrationTest.java`
- Create if needed: `backend/src/test/java/com/data/collection/platform/service/GitlabSyncTaskServiceTest.java`

**Steps:**
1. Add unit coverage for dedupe, serialization, queued follow-up, cancellation, timeout recovery.
2. Add controller/API coverage for cancel endpoint and task status payload.
3. Extend real GitLab integration test to cover duplicate trigger and cancellation path where feasible.
4. Run backend tests.
5. Run frontend tests and build.

