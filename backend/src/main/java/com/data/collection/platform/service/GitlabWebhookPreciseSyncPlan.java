package com.data.collection.platform.service;

import java.util.List;

public record GitlabWebhookPreciseSyncPlan(
    String objectKey,
    String objectId,
    List<GitlabWebhookPreciseSyncTarget> targets) {
}
