package com.data.collection.platform.service;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.GitlabSyncConfig;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GitlabWebhookService {
  private final JdbcTemplate jdbcTemplate;
  private final GitlabConfigService configService;
  private final GitlabMirrorSyncService syncService;

  public GitlabWebhookService(JdbcTemplate jdbcTemplate, GitlabConfigService configService, GitlabMirrorSyncService syncService) {
    this.jdbcTemplate = jdbcTemplate;
    this.configService = configService;
    this.syncService = syncService;
  }

  public void accept(String eventType, Map<String, Object> payload, String secret) {
    GitlabSyncConfig config = configService.getConfig();
    if (config.getWebhookSecret() != null && !config.getWebhookSecret().isBlank()) {
      if (secret == null || !config.getWebhookSecret().equals(secret)) {
        throw new BizException("Invalid webhook secret");
      }
    }
    jdbcTemplate.update("""
        insert into gitlab_webhook_events(config_id, event_type, project_id, object_kind, payload, processed)
        values (?, ?, ?, ?, ?, false)
        """,
        config.getId(),
        eventType,
        extractProjectId(payload),
        String.valueOf(payload.get("object_kind")),
        payload.toString());
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
