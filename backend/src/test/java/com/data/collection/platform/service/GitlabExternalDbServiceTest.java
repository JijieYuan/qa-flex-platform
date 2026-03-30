package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.data.collection.platform.config.GitlabMirrorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitlabExternalDbServiceTest {

  private GitlabExternalDbService service;

  @BeforeEach
  void setUp() {
    service = new GitlabExternalDbService(new GitlabMirrorProperties(), new ObjectMapper());
  }

  @Test
  void shouldPreferUpdatedAtVariantsOverCreatedAtVariants() {
    String resolved = service.resolveUpdatedAtColumn(List.of("id", "CreatedAt", "UpdatedAt"));

    assertThat(resolved).isEqualTo("UpdatedAt");
  }

  @Test
  void shouldFallbackToCreatedAtVariantWhenUpdatedAtIsMissing() {
    String resolved = service.resolveUpdatedAtColumn(List.of("id", "createdAt", "name"));

    assertThat(resolved).isEqualTo("createdAt");
  }

  @Test
  void shouldRecognizeCommonSnakeCaseAndCamelCaseVariants() {
    assertThat(service.resolveUpdatedAtColumn(List.of("id", "updated_at"))).isEqualTo("updated_at");
    assertThat(service.resolveUpdatedAtColumn(List.of("id", "updatedAt"))).isEqualTo("updatedAt");
    assertThat(service.resolveUpdatedAtColumn(List.of("id", "gmt_modified"))).isEqualTo("gmt_modified");
    assertThat(service.resolveUpdatedAtColumn(List.of("id", "create_time"))).isEqualTo("create_time");
  }

  @Test
  void shouldReturnNullWhenNoCandidateExists() {
    assertThat(service.resolveUpdatedAtColumn(List.of("id", "name", "description"))).isNull();
  }
}
