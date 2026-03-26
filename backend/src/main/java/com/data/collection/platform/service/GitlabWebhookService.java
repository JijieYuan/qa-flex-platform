package com.data.collection.platform.service;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabWebhookEvent;
import com.data.collection.platform.mapper.GitlabWebhookEventMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GitlabWebhookService {
  private final GitlabWebhookEventMapper webhookEventMapper;
  private final GitlabConfigService configService;
  private final GitlabMirrorSyncService syncService;
  private final JsonUtils jsonUtils;

  public GitlabWebhookService(
      GitlabWebhookEventMapper webhookEventMapper,
      GitlabConfigService configService,
      GitlabMirrorSyncService syncService,
      JsonUtils jsonUtils) {
    this.webhookEventMapper = webhookEventMapper;
    this.configService = configService;
    this.syncService = syncService;
    this.jsonUtils = jsonUtils;
  }

  public void accept(String eventType, Map<String, Object> payload, String secret) {
    GitlabSyncConfig config = configService.getConfig();
    if (config.getWebhookSecret() != null && !config.getWebhookSecret().isBlank()) {
      if (secret == null || !config.getWebhookSecret().equals(secret)) {
        throw new BizException("Invalid webhook secret");
      }
    }

    GitlabWebhookEvent event = new GitlabWebhookEvent();
    event.setConfigId(config.getId());
    event.setEventType(eventType);
    event.setProjectId(extractProjectId(payload));
    event.setObjectKind(String.valueOf(payload.get("object_kind")));
    event.setPayload(jsonUtils.toJson(payload));
    event.setProcessed(false);
    event.setReceivedAt(LocalDateTime.now());
    webhookEventMapper.insert(event);

    if (config.isAutoSyncEnabled()) {
      syncService.startIncrementalSync("Triggered by webhook: " + eventType);
    }
  }

  private Long extractProjectId(Map<String, Object> payload) {
    Object value = payload.get("project_id");
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value == null) {
      return null;
    }
    return Long.parseLong(String.valueOf(value));
  }
}
