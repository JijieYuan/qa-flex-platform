package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.SourceMode;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.data.collection.platform.entity.WhitelistMode;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("integration")
@SpringBootTest
@EnabledIfSystemProperty(named = "local.gitlab.it", matches = "true")
class GitlabWhitelistServiceIntegrationTest {

  @Autowired
  private GitlabWhitelistService whitelistService;

  @Test
  void shouldDiscoverRealGitlabTablesForAllModeAndUseRealMetadata() {
    GitlabSyncConfig config = new GitlabSyncConfig();
    config.setSourceMode(SourceMode.DOCKER);
    config.setDockerContainerName("gitlab-data-web-1");
    config.setDbName("gitlabhq_production");
    config.setDbUsername("gitlab");
    config.setWhitelistMode(WhitelistMode.ALL);

    List<TableWhitelistOption> allOptions = whitelistService.resolveOptions(config);

    assertThat(allOptions.size()).isGreaterThan(21);
    assertThat(allOptions).extracting(TableWhitelistOption::tableName).contains("users", "user_details", "issues");

    TableWhitelistOption userDetails = allOptions.stream()
        .filter(option -> option.tableName().equals("user_details"))
        .findFirst()
        .orElseThrow();

    assertThat(userDetails.primaryKey()).isEqualTo("user_id");
    assertThat(userDetails.updatedAtColumn()).isNull();
  }
}
