package com.data.collection.platform.service;

public record GitlabWebhookPreciseSyncTarget(
    String tableName,
    String lookupColumn,
    Object lookupValue) {
}
