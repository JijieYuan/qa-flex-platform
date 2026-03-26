package com.data.collection.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
class GitlabMirrorStressIntegrationTest {

  private static final String GITLAB_CONTAINER = "gitlab-data-web-1";
  private static final String PROJECT_PATH = "root/rocksdb";
  private static final String WEBHOOK_SECRET = "local-stress-secret";

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
  void shouldHandleHigherLoadForFullIncrementalAndCompensationSync() throws Exception {
    saveDockerConfig();

    BulkFixture fullFixture = createBulkFixture(uniqueToken("stress-full"), 60);
    mockMvc.perform(post("/api/gitlab-sync/full-sync")).andExpect(status().isOk());
    GitlabSyncLog fullLog = waitForSync(SyncType.FULL, Duration.ofMinutes(5));
    assertThat(fullLog.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertMirrorContains("issues", fullFixture.prefix(), fullFixture.created());
    assertMirrorContains("notes", fullFixture.prefix(), fullFixture.created());
    assertMirrorContains("labels", fullFixture.prefix(), fullFixture.created());

    BulkFixture incrementalFixture = createBulkFixture(uniqueToken("stress-incremental"), 30);
    Map<String, Object> webhookPayload = new LinkedHashMap<>();
    webhookPayload.put("object_kind", "issue");
    webhookPayload.put("project_id", fullFixture.projectId());
    webhookPayload.put("object_attributes", Map.of("id", incrementalFixture.firstIssueId(), "title", incrementalFixture.prefix() + "-issue-0"));

    mockMvc.perform(post("/api/gitlab-sync/webhook")
            .header("X-Gitlab-Event", "Issue Hook")
            .header("X-Gitlab-Token", WEBHOOK_SECRET)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(webhookPayload)))
        .andExpect(status().isOk());
    GitlabSyncLog incrementalLog = waitForSync(SyncType.INCREMENTAL, Duration.ofMinutes(5));
    assertThat(incrementalLog.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertMirrorContains("issues", incrementalFixture.prefix(), incrementalFixture.created());
    assertMirrorContains("notes", incrementalFixture.prefix(), incrementalFixture.created());
    assertMirrorContains("labels", incrementalFixture.prefix(), incrementalFixture.created());

    BulkFixture compensationFixture = createBulkFixture(uniqueToken("stress-comp"), 20);
    syncService.startCompensationSync();
    GitlabSyncLog compensationLog = waitForSync(SyncType.COMPENSATION, Duration.ofMinutes(5));
    assertThat(compensationLog.getStatus()).isEqualTo(SyncStatus.SUCCESS);
    assertMirrorContains("issues", compensationFixture.prefix(), compensationFixture.created());
    assertMirrorContains("notes", compensationFixture.prefix(), compensationFixture.created());
    assertMirrorContains("labels", compensationFixture.prefix(), compensationFixture.created());

    System.out.printf(
        "STRESS_RESULTS full=%sms incremental=%sms compensation=%sms%n",
        durationMs(fullLog),
        durationMs(incrementalLog),
        durationMs(compensationLog));
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

  private void assertMirrorContains(String tableName, String tokenPrefix, int expectedMinimum) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from gitlab_mirror_records where table_name = ? and cast(row_data as text) like ?",
        Integer.class,
        tableName,
        "%" + tokenPrefix + "%");
    assertThat(count).isNotNull();
    assertThat(count).isGreaterThanOrEqualTo(expectedMinimum);
  }

  private BulkFixture createBulkFixture(String prefix, int amount) throws Exception {
    String ruby = """
        require 'json'
        project = Project.find_by_full_path('%s')
        user = User.find_by_username('root')
        first_issue_id = nil
        %d.times do |index|
          issue = project.issues.create!(title: '%s' + "-issue-#{index}", description: '%s' + "-desc-#{index}", author: user)
          first_issue_id ||= issue.id
          label = project.labels.create!(title: '%s' + "-label-#{index}")
          issue.labels << label
          issue.notes.create!(note: '%s' + "-note-#{index}", author: user, project: project)
        end
        puts({project_id: project.id, prefix: '%s', created: %d, first_issue_id: first_issue_id}.to_json)
        """.formatted(PROJECT_PATH, amount, prefix, prefix, prefix, prefix, prefix, amount);
    String output = runGitlabRailsScript(ruby);
    Map<String, Object> payload = objectMapper.readValue(output, new TypeReference<>() {});
    return new BulkFixture(
        Long.parseLong(String.valueOf(payload.get("project_id"))),
        String.valueOf(payload.get("prefix")),
        Integer.parseInt(String.valueOf(payload.get("created"))),
        Long.parseLong(String.valueOf(payload.get("first_issue_id"))));
  }

  private String runGitlabRailsScript(String ruby) throws IOException, InterruptedException {
    Path tempFile = Files.createTempFile("gitlab-stress-", ".rb");
    Files.writeString(tempFile, ruby, StandardCharsets.UTF_8);
    try {
      runCommand(List.of("docker", "cp", tempFile.toString(), GITLAB_CONTAINER + ":/tmp/gitlab-stress.rb"));
      List<String> output = runCommand(List.of(
          "docker",
          "exec",
          GITLAB_CONTAINER,
          "bash",
          "-lc",
          "gitlab-rails runner /tmp/gitlab-stress.rb"));
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

  private long durationMs(GitlabSyncLog log) {
    if (log.getStartedAt() == null || log.getFinishedAt() == null) {
      return -1L;
    }
    return Duration.between(log.getStartedAt(), log.getFinishedAt()).toMillis();
  }

  private String uniqueToken(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private record BulkFixture(long projectId, String prefix, int created, long firstIssueId) {
  }
}
