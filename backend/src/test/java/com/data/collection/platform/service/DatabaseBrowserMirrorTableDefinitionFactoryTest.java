package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.data.collection.platform.common.JsonUtils;
import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.GitlabMirrorTableRegistry;
import com.data.collection.platform.entity.SourceTableColumn;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseBrowserMirrorTableDefinitionFactoryTest {

  private DatabaseBrowserMirrorTableDefinitionFactory factory;
  private JsonUtils jsonUtils;

  @BeforeEach
  void setUp() {
    jsonUtils = new JsonUtils(new ObjectMapper());
    factory = new DatabaseBrowserMirrorTableDefinitionFactory(jsonUtils);
  }

  @Test
  void shouldBuildMirrorTableDefinitionFromRegistry() {
    GitlabMirrorTableRegistry registry = new GitlabMirrorTableRegistry();
    registry.setSourceTableName("issues");
    registry.setUpdatedAtColumn("updated_at");
    registry.setColumnSnapshot(jsonUtils.toJson(List.of(
        new SourceTableColumn("id", "bigint", false, 1),
        new SourceTableColumn("title", "text", true, 2),
        new SourceTableColumn("updated_at", "timestamp", true, 3))));

    DatabaseBrowserTableDefinition definition = factory.buildMirrorTableDefinition(registry);

    assertThat(definition.label()).contains("issues");
    assertThat(definition.searchableFields()).contains("id", "title", "updated_at");
    assertThat(definition.columns()).extracting(column -> column.getKey()).contains("mirror_synced_at", "mirror_deleted");
    assertThat(definition.defaultSortField()).isEqualTo("updated_at");
  }

  @Test
  void shouldRejectRegistryWithoutColumnSnapshot() {
    GitlabMirrorTableRegistry registry = new GitlabMirrorTableRegistry();
    registry.setSourceTableName("issues");
    registry.setColumnSnapshot("[]");

    assertThatThrownBy(() -> factory.buildMirrorTableDefinition(registry))
        .isInstanceOf(BizException.class);
  }
}
