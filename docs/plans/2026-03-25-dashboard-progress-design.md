# GitLab Mirror Dashboard And Progress Design

**Goal**

Add a lightweight home dashboard with an entry into the GitLab mirror settings page, show live sync progress visually, and stop the settings page from auto-refreshing when no sync is running.

**Decision**

Use a two-screen frontend inside the current Vue app:
- `dashboard` as the default landing page
- `settings` as the detailed configuration page

The backend will expose an in-memory runtime sync progress model from the sync service. The frontend will only poll while a sync is running.

**Why**

- The current settings page is the whole app, so there is no clear homepage.
- Users need a visible sync progress bar instead of only a log table.
- Unconditional polling makes the page feel buggy and can overwrite form edits.

**Approach**

1. Add a runtime `SyncProgress` model to the backend.
2. Extend `/api/gitlab-sync/status` to include progress data.
3. Update the frontend to:
   - show a dashboard first
   - navigate into the settings page
   - render a progress card and progress bar
   - poll only when `currentStatus === RUNNING`
4. Add backend tests for status payload/progress behavior.
5. Maintain a project progress document for the new platform.

**Non-goals**

- No authentication in this phase
- No redesign of the sync engine
- No cancellation support in this phase
