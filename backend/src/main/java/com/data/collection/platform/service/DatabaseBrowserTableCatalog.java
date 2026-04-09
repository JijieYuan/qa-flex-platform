package com.data.collection.platform.service;

import com.data.collection.platform.entity.database.DatabaseTableColumn;
import java.util.List;
import java.util.Map;

final class DatabaseBrowserTableCatalog {

  private static final Map<String, DatabaseBrowserTableDefinition> SYSTEM_TABLE_DEFINITIONS =
      Map.of(
          "gitlab_sync_configs",
          new DatabaseBrowserTableDefinition(
              "同步配置",
              List.of(
                  "name",
                  "source_mode",
                  "whitelist_mode",
                  "db_name",
                  "db_username",
                  "docker_container_name"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("name", "数据源名称", true),
                  new DatabaseTableColumn("source_mode", "读取方式", true),
                  new DatabaseTableColumn("whitelist_mode", "白名单模式", true),
                  new DatabaseTableColumn("updated_at", "更新时间", true),
                  new DatabaseTableColumn("created_at", "创建时间", true)),
              "id"),
          "gitlab_sync_logs",
          new DatabaseBrowserTableDefinition(
              "同步日志",
              List.of("sync_type", "status", "message"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("sync_type", "同步类型", true),
                  new DatabaseTableColumn("status", "状态", true),
                  new DatabaseTableColumn("table_count", "表数", true),
                  new DatabaseTableColumn("record_count", "记录数", true),
                  new DatabaseTableColumn("started_at", "开始时间", true),
                  new DatabaseTableColumn("finished_at", "结束时间", true)),
              "id"),
          "gitlab_sync_tasks",
          new DatabaseBrowserTableDefinition(
              "同步任务",
              List.of(
                  "run_id",
                  "task_type",
                  "trigger_type",
                  "scope_key",
                  "status",
                  "finished_reason"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("task_type", "任务类型", true),
                  new DatabaseTableColumn("trigger_type", "触发方式", true),
                  new DatabaseTableColumn("status", "任务状态", true),
                  new DatabaseTableColumn("queued_at", "排队时间", true),
                  new DatabaseTableColumn("started_at", "开始时间", true),
                  new DatabaseTableColumn("finished_at", "结束时间", true)),
              "id"),
          "gitlab_webhook_events",
          new DatabaseBrowserTableDefinition(
              "Webhook 事件",
              List.of("event_type", "project_path", "event_id", "delivery_id"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("event_type", "事件类型", true),
                  new DatabaseTableColumn("project_path", "项目路径", true),
                  new DatabaseTableColumn("received_at", "接收时间", true)),
              "id"),
          "gitlab_mirror_records",
          new DatabaseBrowserTableDefinition(
              "旧镜像记录",
              List.of("table_name", "record_key", "row_data"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("table_name", "源表名", true),
                  new DatabaseTableColumn("record_key", "主键值", true),
                  new DatabaseTableColumn("updated_at_source", "源更新时间", true),
                  new DatabaseTableColumn("synced_at", "镜像时间", true)),
              "id"),
          "collect_form_records",
          new DatabaseBrowserTableDefinition(
              "采集表单记录",
              List.of("template_code", "resource_type", "resource_id", "reviewer", "remark"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("gitlab_base_url", "GitLab 来源地址", true),
                  new DatabaseTableColumn("project_id", "Project ID", true),
                  new DatabaseTableColumn("request_iid", "请求类型 IID", true),
                  new DatabaseTableColumn("resource_type", "资源类型", true),
                  new DatabaseTableColumn("resource_id", "资源编号", true),
                  new DatabaseTableColumn("template_code", "模板编码", true),
                  new DatabaseTableColumn("form_title", "表单标题", true),
                  new DatabaseTableColumn("reviewer", "走查人", true),
                  new DatabaseTableColumn("review_duration_minutes", "走查时间(分钟)", true),
                  new DatabaseTableColumn("specification_score", "规范", true),
                  new DatabaseTableColumn("logic_score", "逻辑", true),
                  new DatabaseTableColumn("performance_score", "性能", true),
                  new DatabaseTableColumn("design_score", "设计", true),
                  new DatabaseTableColumn("other_score", "其他", true),
                  new DatabaseTableColumn("deleted", "是否作废", true),
                  new DatabaseTableColumn("updated_at", "更新时间", true),
                  new DatabaseTableColumn("created_at", "创建时间", true)),
              "updated_at"),
          "code_review_external_metrics",
          new DatabaseBrowserTableDefinition(
              "代码走查外部指标导入表",
              List.of(
                  "merge_request_iid",
                  "comment_rate_source",
                  "defect_count_source",
                  "source_summary",
                  "raw_payload"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("project_id", "Project ID", true),
                  new DatabaseTableColumn("merge_request_id", "MR ID", true),
                  new DatabaseTableColumn("merge_request_iid", "MR IID", true),
                  new DatabaseTableColumn("comment_rate", "代码注释率", true),
                  new DatabaseTableColumn("comment_rate_source", "注释率来源", true),
                  new DatabaseTableColumn("defect_count", "缺陷数", true),
                  new DatabaseTableColumn("defect_count_source", "缺陷来源", true),
                  new DatabaseTableColumn("source_summary", "来源说明", true),
                  new DatabaseTableColumn("raw_payload", "原始导入内容", true),
                  new DatabaseTableColumn("imported_at", "导入时间", true),
                  new DatabaseTableColumn("updated_at", "更新时间", true),
                  new DatabaseTableColumn("created_at", "创建时间", true)),
              "updated_at"),
          "issue_fact",
          new DatabaseBrowserTableDefinition(
              "议题事实表",
              List.of(
                  "source_system",
                  "source_instance",
                  "issue_iid",
                  "title",
                  "module_name",
                  "severity_level",
                  "priority_level"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("source_system", "来源系统", true),
                  new DatabaseTableColumn("source_instance", "来源实例", true),
                  new DatabaseTableColumn("ingest_channel", "接入通道", true),
                  new DatabaseTableColumn("project_id", "Project ID", true),
                  new DatabaseTableColumn("project_name", "项目名称", true),
                  new DatabaseTableColumn("issue_id", "Issue ID", true),
                  new DatabaseTableColumn("issue_iid", "Issue IID", true),
                  new DatabaseTableColumn("title", "标题", true),
                  new DatabaseTableColumn("issue_state", "状态", true),
                  new DatabaseTableColumn("issue_type", "类别", true),
                  new DatabaseTableColumn("milestone_title", "里程碑", true),
                  new DatabaseTableColumn("author_name", "提交人", true),
                  new DatabaseTableColumn("assignee_name", "处理人", true),
                  new DatabaseTableColumn("module_name", "模块名", true),
                  new DatabaseTableColumn("testing_phase", "测试阶段", true),
                  new DatabaseTableColumn("severity_level", "严重程度", true),
                  new DatabaseTableColumn("priority_level", "优先级", true),
                  new DatabaseTableColumn("urgency", "优先级(兼容字段)", true),
                  new DatabaseTableColumn("bug_status", "测试状态", true),
                  new DatabaseTableColumn("category", "缺陷原因", true),
                  new DatabaseTableColumn("system_test_label", "系统测试标签", true),
                  new DatabaseTableColumn("delay_issue", "是否延期", true),
                  new DatabaseTableColumn("delay_cause", "延期原因", true),
                  new DatabaseTableColumn("source_summary", "来源说明", true),
                  new DatabaseTableColumn("raw_payload", "原始载荷", true),
                  new DatabaseTableColumn("fact_refreshed_at", "事实刷新时间", true),
                  new DatabaseTableColumn("updated_at", "更新时间", true),
                  new DatabaseTableColumn("created_at", "创建时间", true)),
              "updated_at"),
          "merge_request_fact",
          new DatabaseBrowserTableDefinition(
              "合并请求事实表",
              List.of(
                  "source_system",
                  "source_instance",
                  "merge_request_iid",
                  "title",
                  "module_name",
                  "owner_name"),
              List.of(
                  new DatabaseTableColumn("id", "ID", true),
                  new DatabaseTableColumn("source_system", "来源系统", true),
                  new DatabaseTableColumn("source_instance", "来源实例", true),
                  new DatabaseTableColumn("ingest_channel", "接入通道", true),
                  new DatabaseTableColumn("project_id", "Project ID", true),
                  new DatabaseTableColumn("project_name", "项目名称", true),
                  new DatabaseTableColumn("repository_name", "仓库名称", true),
                  new DatabaseTableColumn("merge_request_id", "MR ID", true),
                  new DatabaseTableColumn("merge_request_iid", "MR IID", true),
                  new DatabaseTableColumn("title", "标题", true),
                  new DatabaseTableColumn("merge_request_state", "状态", true),
                  new DatabaseTableColumn("target_branch", "目标分支", true),
                  new DatabaseTableColumn("source_branch", "源分支", true),
                  new DatabaseTableColumn("author_name", "提交人", true),
                  new DatabaseTableColumn("merge_user_name", "合并人", true),
                  new DatabaseTableColumn("owner_name", "指派人", true),
                  new DatabaseTableColumn("reviewer_names", "评审人", true),
                  new DatabaseTableColumn("assignee_names", "处理人", true),
                  new DatabaseTableColumn("module_name", "模块名", true),
                  new DatabaseTableColumn("review_status", "走查状态", true),
                  new DatabaseTableColumn("review_duration_minutes", "走查时长(分钟)", true),
                  new DatabaseTableColumn("comment_rate", "代码注释率", true),
                  new DatabaseTableColumn("comment_rate_source", "注释率来源", true),
                  new DatabaseTableColumn("defect_count", "缺陷数", true),
                  new DatabaseTableColumn("defect_count_source", "缺陷数来源", true),
                  new DatabaseTableColumn("scan_status", "扫描状态", true),
                  new DatabaseTableColumn("scan_bug_count", "扫描问题数", true),
                  new DatabaseTableColumn("added_lines", "新增代码行数", true),
                  new DatabaseTableColumn("source_summary", "来源说明", true),
                  new DatabaseTableColumn("raw_payload", "原始载荷", true),
                  new DatabaseTableColumn("fact_refreshed_at", "事实刷新时间", true),
                  new DatabaseTableColumn("updated_at", "更新时间", true),
                  new DatabaseTableColumn("created_at", "创建时间", true)),
              "updated_at"));

  private DatabaseBrowserTableCatalog() {
  }

  static Map<String, DatabaseBrowserTableDefinition> listDefinitions() {
    return SYSTEM_TABLE_DEFINITIONS;
  }

  static DatabaseBrowserTableDefinition findDefinition(String tableName) {
    return SYSTEM_TABLE_DEFINITIONS.get(tableName);
  }
}
