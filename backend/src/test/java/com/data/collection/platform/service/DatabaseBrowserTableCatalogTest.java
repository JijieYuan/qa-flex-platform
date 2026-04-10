package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseBrowserTableCatalogTest {

  @Test
  void shouldMergeDefinitionsFromMultipleProviders() {
    assertThat(DatabaseBrowserTableCatalog.listDefinitions().keySet())
        .contains(
            "gitlab_sync_configs",
            "collect_form_records",
            "issue_fact",
            "merge_request_fact");
  }

  @Test
  void shouldResolveKnownDefinition() {
    DatabaseBrowserTableDefinition definition =
        DatabaseBrowserTableCatalog.findDefinition("merge_request_fact");

    assertThat(definition).isNotNull();
    assertThat(definition.defaultSortField()).isEqualTo("updated_at");
    assertThat(definition.columns()).extracting(column -> column.getKey()).contains("merge_request_iid");
  }
}
