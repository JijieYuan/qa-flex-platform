package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.database.DatabaseTableColumn;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseBrowserQuerySupportTest {

  private final DatabaseBrowserTableDefinition definition = new DatabaseBrowserTableDefinition(
      "table",
      List.of("name", "status"),
      List.of(
          new DatabaseTableColumn("id", "ID", true),
          new DatabaseTableColumn("name", "Name", true),
          new DatabaseTableColumn("status", "Status", true)),
      "id");

  @Test
  void shouldBuildSqlWithKeywordAndPaging() {
    DatabaseBrowserSqlBundle sqlBundle =
        DatabaseBrowserQuerySupport.buildSql(definition, "gitlab_sync_logs", "FAILED", "id", "desc", 2, 20);

    assertThat(sqlBundle.countSql()).contains("select count(*) from \"gitlab_sync_logs\"");
    assertThat(sqlBundle.rowsSql()).contains("order by \"id\" desc, \"id\" desc");
    assertThat(sqlBundle.rowsSql()).contains("limit 20 offset 20");
    assertThat(sqlBundle.arguments()).containsExactly("%FAILED%", "%FAILED%");
  }

  @Test
  void shouldRejectUnsupportedSortField() {
    assertThatThrownBy(() -> DatabaseBrowserQuerySupport.normalizeSortField(definition, "unknown"))
        .isInstanceOf(BizException.class);
  }
}
