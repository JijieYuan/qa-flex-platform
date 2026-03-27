# JSON Logging Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the backend logging system with logstash JSON logging and add full structured logging coverage to the GitLab sync pipeline.

**Architecture:** Add a single Logback JSON configuration, introduce a reusable MDC context helper for sync tasks, and refactor sync/webhook/controller paths to emit action-level structured logs with stack traces and persistent rolling files.

**Tech Stack:** Spring Boot, Logback, logstash-logback-encoder, SLF4J MDC, MyBatis-Plus, JUnit 5

---

### Task 1: Add centralized JSON Logback configuration

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/logback-spring.xml`

**Step 1: Write the configuration**

- Add file log path properties if needed
- Create JSON console appender
- Create JSON rolling file appender with immediate flush and size/time rolling
- Ensure required MDC fields are emitted

**Step 2: Verify build**

Run: `mvn -q -DskipTests compile`
Expected: PASS

### Task 2: Add reusable sync MDC context helper

**Files:**
- Create: `backend/src/main/java/com/data/collection/platform/common/logging/GitlabSyncLogContext.java`

**Step 1: Implement helper**

- Provide context open/close API
- Inject `traceId`, `taskId`, `scope`, `gitlabUrl`, `taskType`
- Ensure cleanup via `AutoCloseable`

**Step 2: Add minimal unit verification if useful**

### Task 3: Refactor sync execution logging

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabMirrorSyncService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabExternalDbService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabCompensationScheduler.java`
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabSyncTaskService.java`

**Step 1: Replace sparse messages with structured action logs**

- `Task_Start`
- `Data_Fetching`
- `Mirror_Writing`
- `Commit_Success`
- `Task_End`

**Step 2: Ensure exceptions retain stack traces**

**Step 3: Include compensation window / retry / skip counts where relevant**

### Task 4: Refactor webhook and controller logging

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/GitlabWebhookService.java`
- Modify: `backend/src/main/java/com/data/collection/platform/controller/GitlabSyncController.java`
- Modify: `backend/src/main/java/com/data/collection/platform/common/exception/GlobalExceptionHandler.java`

**Step 1: Add `Webhook_Received`**

**Step 2: Add request-level structured logs for manual actions**

**Step 3: Keep exception handling stack-safe**

### Task 5: Remove non-standard outputs and align all classes

**Files:**
- Scan and modify all backend source files as needed

**Step 1: Replace any `System.out.println` or `printStackTrace`**

**Step 2: Ensure every touched class uses standard logger**

### Task 6: Add tests and examples

**Files:**
- Modify/Add tests under `backend/src/test/java/...`
- Update docs if needed

**Step 1: Verify no non-standard outputs remain**

Run: search commands
Expected: no results

**Step 2: Run focused backend tests**

Run: `mvn -q "-Dtest=GitlabSyncTaskServiceTest,GitlabMirrorSyncServiceTest,GitlabCompensationSchedulerTest,GitlabSyncControllerTest" test`
Expected: PASS

**Step 3: Run real integration**

Run: `mvn -q '-Dlocal.gitlab.it=true' '-Dtest=GitlabMirrorIntegrationTest' test`
Expected: PASS

### Task 7: Produce sample log snippets

**Files:**
- Document in final handoff, optionally add under docs

**Step 1: Capture representative JSON entries**

- success
- network failure
- compensation after restart

**Step 2: Include them in response**

Plan complete and saved to `docs/plans/2026-03-27-json-logging.md`.
