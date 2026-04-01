package com.data.collection.platform.entity;

public record CollectFormNotificationPayloadResponse(
    String sourceAddress,
    Long projectId,
    Long requestIid,
    String resourceType) {}
