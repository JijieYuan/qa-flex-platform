# GitLab Sync Task Governance Design

## Background

The platform already uses a two-stage data flow:

1. Read from the GitLab source database
2. Persist into the local mirror database
3. Serve statistic pages from the local mirror database

This means the import/sync chain is a real task-execution problem rather than a simple page-triggered fetch. The old implementation still mixes three different concepts:

- current execution status
- execution history
- page polling state

That mixing leads to unstable behavior such as repeated compensation syncs, stale `RUNNING` states, and the import page refreshing longer than expected.

## Design Goals

- Make sync task state explicit and durable
- Prevent concurrent writes for the same sync scope
- Support safe user-initiated cancellation
- Keep compensation scheduling bounded by cooldown and retry backoff
- Make the frontend depend only on task status, not on logs
- Preserve current lightweight mirror architecture without introducing full batch-switching

## Non-Goals

- No change to statistic rules or mirror table query semantics
- No batch-level atomic visibility switching in this iteration
- No general workflow engine or distributed scheduler platform

## Hard Constraints

### 1. Task table is the only state source of truth

Everything below must read task state from the sync task table only:

- whether a sync is running
- whether a new sync can be started
- whether cancellation is allowed
- whether compensation should run
- whether the page should keep polling

`gitlab_sync_logs` remains an audit and troubleshooting table only. It must not be used to infer the current task state.

### 2. `scope_key` must be explicit

The concurrency boundary is defined by `scope_key`.

`scope_key` is composed from:

- data source
- sync type family
- sync object scope
- environment

For the current project, the first implementation can normalize scope as:

`<configId>:<scopeFamily>:<whitelistMode>:<whitelistHash>:<sourceMode>`

Where:

- `configId` identifies the data source
- `scopeFamily` collapses current sync requests into one mirror-write scope
- `whitelistMode` + `whitelistHash` represent the target range
- `sourceMode` distinguishes Docker vs Direct source access

This definition is intentionally strict to avoid ambiguity when different triggers try to write the same mirror range.

### 3. Read/write isolation is lightweight only

This iteration does **not** implement batch switching or atomic batch publish.

The guarantee in this iteration is:

- same scope does not write concurrently
- pages continue reading the current stable mirror dataset
- unfinished work is not treated as a completed visible snapshot

This is a controlled improvement over the previous gap-prone behavior, but not a full snapshot isolation design.

## Proposed Model

### New table: `gitlab_sync_tasks`

Recommended fields:

- `id`
- `run_id`
- `config_id`
- `task_type`
- `trigger_type`
- `source_mode`
- `scope_key`
- `dedupe_key`
- `status`
- `cancel_requested`
- `pending_resync`
- `retry_count`
- `cooldown_until`
- `heartbeat_at`
- `queued_at`
- `run_after`
- `started_at`
- `finished_at`
- `finished_reason`
- `lock_owner`
- `version`
- `payload_json`
- `created_at`
- `updated_at`

### Task status set

- `PENDING`
- `QUEUED`
- `RUNNING`
- `CANCELLING`
- `CANCELLED`
- `SUCCESS`
- `FAILED`
- `TIMEOUT`

## Trigger and Execution Flow

### Webhook trigger

1. Receive webhook event
2. Validate secret
3. Persist webhook audit event
4. Build `scope_key` and `dedupe_key`
5. Run task creation with dedupe and scope concurrency checks
6. If no active task for the same scope exists:
   - create `PENDING` task
   - dispatch executor
7. If an active task already exists for the same scope:
   - do not run concurrently
   - mark `pending_resync = true` on the active scope task or create `QUEUED` task only when required

### Manual full/incremental trigger

1. Persist current config if needed
2. Build `scope_key` and short-window `dedupe_key`
3. Prevent duplicate task creation on repeated clicks
4. Return existing task if a duplicate request is already in progress

### Compensation trigger

Compensation must not be inferred from logs. It must be decided from:

- latest finished task for the scope
- cooldown window
- recent failure backoff
- recent cancellation protection
- current active task existence

Compensation should create a new task only when the scope is currently idle and the cooldown/backoff gates pass.

## Concurrency Rules

### Scope-level serialization

For the same `scope_key`, only one active task may exist in:

- `PENDING`
- `QUEUED`
- `RUNNING`
- `CANCELLING`

No concurrent mirror writes are allowed for the same scope.

### Request-level dedupe

Repeated requests with the same `dedupe_key` inside a short window must return the existing task rather than creating a new one.

### Overlap handling

When a new trigger arrives while the same scope is active:

- exact duplicate trigger: ignore / return existing task
- repeated auto trigger needing catch-up: set `pending_resync = true`
- explicit preserved next-run case: create `QUEUED` task

## Cancellation Model

Cancellation is cooperative, not force-kill.

1. Frontend requests cancel
2. Backend sets:
   - `status = CANCELLING`
   - `cancel_requested = true`
3. Executor checks cancel at safe boundaries:
   - before table scan
   - between table scans
   - between mirror write batches
4. Executor stops safely and finishes task as:
   - `CANCELLED`
   - with a clear `finished_reason`

If the cancellation request fails due to network or backend error, the frontend must show that the request result is unknown and continue polling task state until a terminal status is observed.

## Heartbeat and Timeout Recovery

The executor updates `heartbeat_at` during long-running work.

Recovery rules:

- stale `RUNNING` / `CANCELLING` tasks with expired heartbeat are marked `TIMEOUT`
- timeout tasks are eligible for controlled compensation later
- timeout recovery uses task table only

## Frontend Polling Rules

The import page polls only when the current task status is:

- `PENDING`
- `QUEUED`
- `RUNNING`
- `CANCELLING`

Polling stops immediately for:

- `SUCCESS`
- `FAILED`
- `CANCELLED`
- `TIMEOUT`

The page should restore state from the latest task query after refresh or re-entry.

## Logging Strategy

`gitlab_sync_logs` remains for:

- task created
- execution started
- phase progress
- cancellation requested
- cancellation completed
- timeout recovery
- success/failure summaries

Logs are not allowed to drive current-state decisions.

## Backend Refactor Boundary

The following legacy patterns should be removed or replaced:

- controller/status logic that infers state from `RUNNING` logs
- scheduler decisions using logs as primary status signal
- sync service state controlled only by process-local `AtomicBoolean`
- stale-running recovery that mutates logs without task ownership semantics

## Testing Strategy

### Unit tests

- task dedupe
- scope serialization
- queued / pending_resync behavior
- cancellation state transition
- heartbeat timeout recovery
- compensation cooldown and backoff
- idempotent manual double submit

### Integration tests

- manual full sync creates exactly one task
- webhook duplicate trigger does not create duplicate running tasks
- second trigger during long-running task becomes queued or merged
- cancel request moves task to `CANCELLING` then `CANCELLED`
- timeout recovery marks stale task correctly
- frontend polling terminal-state contract is satisfied by API

### Real validation

Use the local real GitLab source and local mirror database.

No mock result should be used to fake pass/fail outcomes for the core sync chain.

