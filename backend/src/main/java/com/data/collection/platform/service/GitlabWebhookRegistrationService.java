package com.data.collection.platform.service;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.common.logging.GitlabSyncLogContext;
import com.data.collection.platform.config.GitlabMirrorProperties;
import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.GitlabWebhookRegistrationStatus;
import com.data.collection.platform.entity.GitlabWebhookRegistrationStatus.RegisteredGitlabWebhook;
import com.data.collection.platform.entity.SourceMode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GitlabWebhookRegistrationService {
  private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {};

  private final GitlabMirrorProperties properties;
  private final ObjectMapper objectMapper;

  public GitlabWebhookRegistrationService(GitlabMirrorProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public GitlabWebhookRegistrationStatus getStatus(GitlabSyncConfig config, String webhookUrl) {
    if (config.getSourceMode() != SourceMode.DOCKER) {
      return new GitlabWebhookRegistrationStatus(
          false,
          config.getWebhookProjectId() != null,
          false,
          config.getWebhookProjectId(),
          webhookUrl,
          "当前仅支持 Docker 模式下自动管理 GitLab Webhook",
          List.of());
    }
    if (config.getWebhookProjectId() == null) {
      return new GitlabWebhookRegistrationStatus(
          true,
          false,
          false,
          null,
          webhookUrl,
          "请先填写 GitLab Project ID",
          List.of());
    }
    List<RegisteredGitlabWebhook> hooks = listHooks(config, webhookUrl);
    return new GitlabWebhookRegistrationStatus(
        true,
        true,
        !hooks.isEmpty(),
        config.getWebhookProjectId(),
        webhookUrl,
        hooks.isEmpty() ? "当前项目尚未注册平台 Webhook" : "GitLab Webhook 已注册",
        hooks);
  }

  public GitlabWebhookRegistrationStatus ensureRegistered(GitlabSyncConfig config, String webhookUrl) {
    if (config.getSourceMode() != SourceMode.DOCKER) {
      throw new BizException("当前仅支持 Docker 模式下自动注册 GitLab Webhook");
    }
    if (config.getWebhookProjectId() == null) {
      throw new BizException("请先配置 GitLab Project ID");
    }
    List<String> command = buildDockerExecCommand(
        config,
        webhookUrl,
        config.getWebhookSecret(),
        buildEnsureHookScript());
    String output = runCommand(command, "Webhook_Register");
    try {
      objectMapper.readTree(output);
    } catch (Exception e) {
      throw new BizException("GitLab Webhook 注册返回无法解析: " + output);
    }
    return getStatus(config, webhookUrl);
  }

  private List<RegisteredGitlabWebhook> listHooks(GitlabSyncConfig config, String webhookUrl) {
    List<String> command = buildDockerExecCommand(config, webhookUrl, config.getWebhookSecret(), buildListHooksScript());
    String output = runCommand(command, "Webhook_Status");
    try {
      List<Map<String, Object>> rows = objectMapper.readValue(output, LIST_TYPE);
      List<RegisteredGitlabWebhook> result = new ArrayList<>(rows.size());
      for (Map<String, Object> row : rows) {
        result.add(new RegisteredGitlabWebhook(
            asLong(row.get("id")),
            asString(row.get("url")),
            asBoolean(row.get("issues_events")),
            asBoolean(row.get("merge_requests_events")),
            asBoolean(row.get("note_events")),
            asBoolean(row.get("pipeline_events")),
            asBoolean(row.get("job_events")),
            asBoolean(row.get("releases_events")),
            asBoolean(row.get("enable_ssl_verification"))));
      }
      return result;
    } catch (Exception e) {
      throw new BizException("GitLab Webhook 状态解析失败: " + output);
    }
  }

  private List<String> buildDockerExecCommand(
      GitlabSyncConfig config,
      String webhookUrl,
      String webhookSecret,
      String rubyScript) {
    return List.of(
        properties.getDockerCommand(),
        "exec",
        "-e",
        "PROJECT_ID=" + config.getWebhookProjectId(),
        "-e",
        "WEBHOOK_URL=" + webhookUrl,
        "-e",
        "WEBHOOK_SECRET=" + (webhookSecret == null ? "" : webhookSecret),
        config.getDockerContainerName(),
        "gitlab-rails",
        "runner",
        rubyScript);
  }

  private String runCommand(List<String> command, String actionName) {
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
               new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.isBlank()) {
            output.append(line.trim());
          }
        }
      }
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new BizException("GitLab Webhook 命令执行失败: " + output);
      }
      return output.toString().trim();
    } catch (BizException e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action(actionName)) {
        log.error("GitLab webhook operation failed", e);
      }
      throw e;
    } catch (Exception e) {
      try (GitlabSyncLogContext.Scope action = GitlabSyncLogContext.action(actionName)) {
        log.error("GitLab webhook operation failed", e);
      }
      throw new BizException("GitLab Webhook 操作失败: " + e.getMessage());
    }
  }

  private String buildListHooksScript() {
    return """
        project_id = Integer(ENV.fetch('PROJECT_ID'))
        webhook_url = ENV.fetch('WEBHOOK_URL')
        project = Project.find(project_id)
        hooks = project.hooks.select { |h| h.url == webhook_url }.map do |h|
          {
            id: h.id,
            url: h.url,
            issues_events: h.issues_events,
            merge_requests_events: h.merge_requests_events,
            note_events: h.note_events,
            pipeline_events: h.pipeline_events,
            job_events: h.job_events,
            releases_events: h.releases_events,
            enable_ssl_verification: h.enable_ssl_verification
          }
        end
        puts hooks.to_json
        """;
  }

  private String buildEnsureHookScript() {
    return """
        project_id = Integer(ENV.fetch('PROJECT_ID'))
        webhook_url = ENV.fetch('WEBHOOK_URL')
        webhook_secret = ENV.fetch('WEBHOOK_SECRET', '')
        project = Project.find(project_id)
        hook = project.hooks.detect { |existing_hook| existing_hook.url == webhook_url }
        hook ||= project.hooks.build(url: webhook_url)
        hook.issues_events = true
        hook.merge_requests_events = true
        hook.note_events = true
        hook.pipeline_events = true
        hook.job_events = true
        hook.releases_events = true
        hook.enable_ssl_verification = false
        hook.token = webhook_secret
        hook.save!
        puts({
          id: hook.id,
          url: hook.url,
          issues_events: hook.issues_events,
          merge_requests_events: hook.merge_requests_events,
          note_events: hook.note_events,
          pipeline_events: hook.pipeline_events,
          job_events: hook.job_events,
          releases_events: hook.releases_events,
          enable_ssl_verification: hook.enable_ssl_verification
        }.to_json)
        """;
  }

  private Long asLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return value == null ? null : Long.parseLong(String.valueOf(value));
  }

  private String asString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private boolean asBoolean(Object value) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    return value != null && Boolean.parseBoolean(String.valueOf(value));
  }
}
