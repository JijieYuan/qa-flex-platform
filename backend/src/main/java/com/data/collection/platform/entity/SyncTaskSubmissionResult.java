package com.data.collection.platform.entity;

public record SyncTaskSubmissionResult(
    GitlabSyncTask task,
    SyncSubmissionAction action) {
}
