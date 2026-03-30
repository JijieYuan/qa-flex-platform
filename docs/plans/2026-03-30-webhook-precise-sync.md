# Webhook Precise Sync Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a production-oriented webhook precise sync path that updates core GitLab business objects by event payload instead of scanning all source tables, while keeping manual incremental sync as a controlled fallback.

**Architecture:** Webhook events are normalized into object-level sync targets, then routed to precise source-table lookups and ODS writes. Unsupported or non-precise scenarios fall back to manual incremental/compensation logic, but the primary webhook path must avoid table-wide window scans.

**Tech Stack:** Spring Boot, MyBatis-Plus, PostgreSQL, JUnit 5, Mockito, MockMvc

---

### Task 1: Finalize precise target routing

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabWebhookPreciseSyncPlanner.java`
- Test: `backend/src/test/java/com/data_collection/platform/service/GitlabWebhookPreciseSyncPlannerTest.java`

**Intent:**
- Cover a broader business-object boundary for webhook precise sync.
- Keep unsupported events explicit and safe.

### Task 2: Wire webhook tasks into precise execution

**Files:**
- Modify: `backend/src/main/java/com/data_collection/platform/service/GitlabWebhookService.java`
- Modify: `backend/src/main/java/com/data_collection/platform/service/GitlabMirrorSyncService.java`
- Modify: `backend/src/main/java/com/data_collection/platform/service/GitlabExternalDbService.java`
- Create: `backend/src/main/java/com/data_collection/platform/service/GitlabWebhookPreciseSyncPlanner.java`
- Create: `backend/src/main/java/com/data_collection/platform/service/GitlabWebhookPreciseSyncTarget.java`

**Intent:**
- Store webhook payload in task payload.
- Execute exact table lookups when a precise target can be derived.
- Keep existing incremental fallback for unsupported events.

### Task 3: Define manual incremental sync semantics

**Files:**
- Modify: `backend/src/main/java/com/data_collection/platform/service/GitlabMirrorSyncService.java`
- Modify: `backend/src/main/java/com/data_collection/platform/controller/GitlabSyncController.java`
- Test: `backend/src/test/java/com/data_collection/platform/service/GitlabMirrorSyncServiceTest.java`

**Intent:**
- Keep “立即增量同步” as a manual recovery mechanism.
- Clearly separate:
  - `WEBHOOK`: precise object update first, fallback if unsupported
  - `INCREMENTAL`: manual/windowed sync fallback

### Task 4: Add unit and controller/service integration tests

**Files:**
- Create/Modify: `backend/src/test/java/com/data_collection/platform/service/GitlabWebhookServiceTest.java`
- Create/Modify: `backend/src/test/java/com/data_collection/platform/service/GitlabWebhookPreciseSyncPlannerTest.java`
- Modify: `backend/src/test/java/com/data_collection/platform/service/GitlabMirrorSyncServiceTest.java`
- Modify: `backend/src/test/java/com/data_collection/platform/service/GitlabExternalDbServiceTest.java`
- Modify: `backend/src/test/java/com/data_collection/platform/controller/GitlabSyncControllerTest.java`

**Intent:**
- Use simulated webhook payloads to validate routing and execution.
- Verify controller path accepts webhook and delegates correctly.

### Task 5: Validate against local GitLab with inserted test data

**Files:**
- No fixed code file; use local GitLab database and logs

**Intent:**
- Insert representative records into local GitLab source tables for at least:
  - issue / merge_request / note or another supported object
- Trigger webhook/manual sync
- Verify ODS writes and task/log behavior from real environment

