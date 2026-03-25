package com.data.collection.platform.service;

import com.data.collection.platform.entity.GitlabSyncConfig;
import com.data.collection.platform.entity.TableWhitelistOption;
import com.data.collection.platform.entity.WhitelistMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GitlabWhitelistService {
  private final Map<String, TableWhitelistOption> options = new LinkedHashMap<>();

  public GitlabWhitelistService() {
    register("users", "用户", "id", "updated_at", true);
    register("user_details", "用户详情", "user_id", "updated_at", true);
    register("projects", "项目", "id", "updated_at", true);
    register("namespaces", "命名空间", "id", "updated_at", true);
    register("members", "成员关系", "id", "updated_at", true);
    register("issues", "缺陷 / Issue", "id", "updated_at", true);
    register("issue_assignees", "缺陷指派", "issue_id,user_id", null, true);
    register("issue_metrics", "缺陷指标", "issue_id", "updated_at", true);
    register("notes", "评论 / Notes", "id", "updated_at", true);
    register("labels", "标签", "id", "updated_at", true);
    register("label_links", "标签关联", "target_type,target_id,label_id", null, true);
    register("merge_requests", "合并请求", "id", "updated_at", true);
    register("merge_request_assignees", "MR 指派", "merge_request_id,user_id", null, true);
    register("merge_request_reviewers", "MR Reviewer", "merge_request_id,user_id", null, true);
    register("merge_request_metrics", "MR 指标", "merge_request_id", "updated_at", true);
    register("ci_pipelines", "流水线", "id", "updated_at", true);
    register("ci_builds", "构建任务", "id", "updated_at", true);
    register("deployments", "部署", "id", "updated_at", true);
    register("environments", "环境", "id", "updated_at", true);
    register("events", "事件", "id", "created_at", true);
    register("todos", "待办", "id", "updated_at", true);
  }

  public List<TableWhitelistOption> listOptions() {
    return new ArrayList<>(options.values());
  }

  public List<TableWhitelistOption> resolveOptions(GitlabSyncConfig config) {
    if (config == null || config.getWhitelistMode() == null || config.getWhitelistMode() == WhitelistMode.RECOMMENDED) {
      return options.values().stream().filter(TableWhitelistOption::recommended).toList();
    }
    if (config.getWhitelistMode() == WhitelistMode.ALL) {
      return new ArrayList<>(options.values());
    }
    List<String> tables = config.getWhitelistTables() == null ? List.of() : config.getWhitelistTables();
    return tables.stream().map(options::get).filter(java.util.Objects::nonNull).collect(Collectors.toList());
  }

  private void register(String tableName, String label, String primaryKey, String updatedAtColumn, boolean recommended) {
    options.put(tableName, new TableWhitelistOption(tableName, label, primaryKey, updatedAtColumn, recommended));
  }
}
