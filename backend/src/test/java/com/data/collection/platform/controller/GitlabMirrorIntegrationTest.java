package com.data.collection.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.data.collection.platform.entity.GitlabSyncLog;
import com.data.collection.platform.entity.SyncStatus;
import com.data.collection.platform.entity.SyncType;
import com.data.collection.platform.service.GitlabConfigService;
import com.data.collection.platform.service.GitlabMirrorSyncService;
import com.data.collection.platform.service.GitlabSyncLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "local.gitlab.it", matches = "true")
class GitlabMirrorIntegrationTest {

  private static final String GITLAB_CONTAINER = "gitlab-data-web-1";
  private static final String PROJECT_PATH = "root/rocksdb";
  private static final String WEBHOOK_SECRET = "local-it-secret";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GitlabConfigService configService;

  @Autowired
  private GitlabSyncLogService logService;

  @Autowired
  private GitlabMirrorSyncService syncService;

  @BeforeEach
  void cleanPlatformTables() {
    jdbcTemplate.update("delete from gitlab_webhook_events");
    jdbcTemplate.update("delete from gitlab_mirror_records");
    jdbcTemplate.update("delete from gitlab_sync_logs");
    jdbcTemplate.update("delete from gitlab_sync_configs");
  }

  @Test
  void shouldRunFullSyncIncrementalWebhookAndCompensationAgainstLocalGitlab() throws Exception {
    String fullToken = uniqueToken("full");
    GitlabFixture fullFixture = createGitlabFixture(fullToken);

    saveDockerConfig();

    mockMvc.perform(post("/api/gitlab-sync/test-connection"))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/gitlab-sync/full-sync"))
        .andExpect(status().isOk());

    GitlabSyncLog fullLog = waitForSync(SyncType.FULL, Duration.ofMinutes(3));
    assertThat(fullLog.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertMirrorContains("issues", fullToken);
    assertMirrorContains("notes", fullToken);
    assertMirrorContains("labels", fullToken);

    String incrementalToken = uniqueToken("incremental");
    GitlabFixture incrementalFixture = createGitlabFixture(incrementalToken);

    Map<String, Object> webhookPayload = new LinkedHashMap<>();
    webhookPayload.put("object_kind", "issue");
    webhookPayload.put("project_id", fullFixture.projectId());
    webhookPayload.put("object_attributes", Map.of("id", incrementalFixture.issueId(), "title", incrementalFixture.issueTitle()));

    mockMvc.perform(post("/api/gitlab-sync/webhook")
            .header("X-Gitlab-Event", "Issue Hook")
            .header("X-Gitlab-Token", WEBHOOK_SECRET)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(webhookPayload)))
        .andExpect(status().isOk());

    GitlabSyncLog incrementalLog = waitForSync(SyncType.INCREMENTAL, Duration.ofMinutes(3));
    assertThat(incrementalLog.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertThat(countRows("select count(*) from gitlab_webhook_events")).isEqualTo(1);
    assertMirrorContains("issues", incrementalToken);
    assertMirrorContains("notes", incrementalToken);
    assertMirrorContains("labels", incrementalToken);

    String secondIncrementalToken = uniqueToken("incremental-second");
    GitlabFixture secondIncrementalFixture = createGitlabFixture(secondIncrementalToken);

    Map<String, Object> secondWebhookPayload = new LinkedHashMap<>();
    secondWebhookPayload.put("object_kind", "issue");
    secondWebhookPayload.put("project_id", fullFixture.projectId());
    secondWebhookPayload.put("object_attributes", Map.of("id", secondIncrementalFixture.issueId(), "title", secondIncrementalFixture.issueTitle()));

    mockMvc.perform(post("/api/gitlab-sync/webhook")
            .header("X-Gitlab-Event", "Issue Hook")
            .header("X-Gitlab-Token", WEBHOOK_SECRET)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(secondWebhookPayload)))
        .andExpect(status().isOk());

    GitlabSyncLog secondIncrementalLog = waitForNextSync(SyncType.INCREMENTAL, incrementalLog.getId(), Duration.ofMinutes(3));
    assertThat(secondIncrementalLog.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertMirrorContains("issues", secondIncrementalToken);
    assertMirrorContains("notes", secondIncrementalToken);
    assertMirrorContains("labels", secondIncrementalToken);

    String compensationToken = uniqueToken("compensation");
    createGitlabFixture(compensationToken);
    syncService.startCompensationSync();

    GitlabSyncLog compensationLog = waitForSync(SyncType.COMPENSATION, Duration.ofMinutes(3));
    assertThat(compensationLog.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertMirrorContains("issues", compensationToken);
    assertMirrorContains("notes", compensationToken);
    assertMirrorContains("labels", compensationToken);

    String statusPayload = mockMvc.perform(get("/api/gitlab-sync/status"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);

    Map<String, Object> statusMap = objectMapper.readValue(statusPayload, new TypeReference<>() {});
    assertThat(statusMap).containsEntry("success", true);
    assertThat(fullFixture.projectId()).isEqualTo(1L);
  }

  private void saveDockerConfig() throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", "GitLab 默认数据源");
    payload.put("enabled", true);
    payload.put("autoSyncEnabled", true);
    payload.put("sourceMode", "DOCKER");
    payload.put("whitelistMode", "CUSTOM");
    payload.put("whitelistTables", List.of("issues", "notes", "labels"));
    payload.put("dbHost", "localhost");
    payload.put("dbPort", 5432);
    payload.put("dbName", "gitlabhq_production");
    payload.put("dbUsername", "gitlab");
    payload.put("dbPassword", "");
    payload.put("dockerContainerName", GITLAB_CONTAINER);
    payload.put("webhookSecret", WEBHOOK_SECRET);
    payload.put("webhookProjectId", 1);
    payload.put("compensationIntervalMinutes", 10);

    mockMvc.perform(put("/api/gitlab-sync/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk());

    assertThat(configService.getConfig().getId()).isNotNull();
  }

  private GitlabSyncLog waitForSync(SyncType syncType, Duration timeout) throws InterruptedException {
    Instant deadline = Instant.now().plus(timeout);
    Long configId = configService.getConfig().getId();
    while (Instant.now().isBefore(deadline)) {
      List<GitlabSyncLog> logs = logService.listRecent(configId, 20);
      GitlabSyncLog target = logs.stream().filter(log -> log.getSyncType() == syncType).findFirst().orElse(null);
      if (target != null && target.getStatus() != SyncStatus.RUNNING) {
        return target;
      }
      Thread.sleep(1000L);
    }
    throw new AssertionError("Timed out waiting for sync type " + syncType);
  }

  private GitlabSyncLog waitForNextSync(SyncType syncType, Long afterId, Duration timeout) throws InterruptedException {
    Instant deadline = Instant.now().plus(timeout);
    Long configId = configService.getConfig().getId();
    while (Instant.now().isBefore(deadline)) {
      List<GitlabSyncLog> logs = logService.listRecent(configId, 20);
      GitlabSyncLog target = logs.stream()
          .filter(log -> log.getSyncType() == syncType && log.getId() > afterId)
          .findFirst()
          .orElse(null);
      if (target != null && target.getStatus() != SyncStatus.RUNNING) {
        return target;
      }
      Thread.sleep(1000L);
    }
    throw new AssertionError("Timed out waiting for next sync type " + syncType + " after log id " + afterId);
  }

  private void assertMirrorContains(String tableName, String token) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from gitlab_mirror_records where table_name = ? and cast(row_data as text) like ?",
        Integer.class,
        tableName,
        "%" + token + "%");
    assertThat(count).isNotNull();
    assertThat(count).isGreaterThan(0);
  }

  private int countRows(String sql) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
    return count == null ? 0 : count;
  }

  private GitlabFixture createGitlabFixture(String token) throws Exception {
    String ruby = """
        require 'json'
        project = Project.find_by_full_path('%s')
        user = User.find_by_username('root')
        issue = project.issues.create!(title: '%s issue', description: '%s description', author: user)
        label = project.labels.create!(title: '%s-label')
        issue.labels << label
        note = issue.notes.create!(note: '%s note', author: user, project: project)
        puts({project_id: project.id, issue_id: issue.id, issue_title: issue.title, note_id: note.id, label_id: label.id}.to_json)
        """.formatted(PROJECT_PATH, token, token, token, token);
    String output = runGitlabRailsScript(ruby);
    Map<String, Object> payload = objectMapper.readValue(output, new TypeReference<>() {});
    return new GitlabFixture(
        Long.parseLong(String.valueOf(payload.get("project_id"))),
        Long.parseLong(String.valueOf(payload.get("issue_id"))),
        String.valueOf(payload.get("issue_title")),
        Long.parseLong(String.valueOf(payload.get("note_id"))),
        Long.parseLong(String.valueOf(payload.get("label_id"))),
        token);
  }

  private String runGitlabRailsScript(String ruby) throws IOException, InterruptedException {
    Path tempFile = Files.createTempFile("gitlab-it-", ".rb");
    Files.writeString(tempFile, ruby, StandardCharsets.UTF_8);
    try {
      runCommand(List.of("docker", "cp", tempFile.toString(), GITLAB_CONTAINER + ":/tmp/gitlab-it.rb"));
      List<String> output = runCommand(List.of(
          "docker",
          "exec",
          GITLAB_CONTAINER,
          "bash",
          "-lc",
          "gitlab-rails runner /tmp/gitlab-it.rb"));
      return output.stream().filter(line -> line != null && !line.isBlank()).reduce((first, second) -> second).orElseThrow();
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  private List<String> runCommand(List<String> command) throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(true);
    Process process = builder.start();
    List<String> lines;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      lines = reader.lines().toList();
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IllegalStateException("Command failed: " + String.join(" ", command) + System.lineSeparator() + String.join(System.lineSeparator(), lines));
    }
    return lines;
  }

  private String uniqueToken(String prefix) {
    return "it-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private record GitlabFixture(
      long projectId,
      long issueId,
      String issueTitle,
      long noteId,
      long labelId,
      String token) {
  }
}
