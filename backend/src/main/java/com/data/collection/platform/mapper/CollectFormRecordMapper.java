package com.data.collection.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.data.collection.platform.entity.CollectFormRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CollectFormRecordMapper extends BaseMapper<CollectFormRecord> {

  @Select("""
      select *
        from collect_form_records
       where gitlab_base_url = #{gitlabBaseUrl}
         and project_id = #{projectId}
         and resource_type = #{resourceType}
         and resource_id = #{resourceId}
         and template_code = #{templateCode}
       limit 1
      """)
  CollectFormRecord selectByContext(
      @Param("gitlabBaseUrl") String gitlabBaseUrl,
      @Param("projectId") Long projectId,
      @Param("resourceType") String resourceType,
      @Param("resourceId") String resourceId,
      @Param("templateCode") String templateCode);

  @Insert("""
      insert into collect_form_records(
        gitlab_base_url,
        project_id,
        mr_iid,
        resource_type,
        resource_id,
        template_code,
        form_title,
        reviewer,
        review_duration_minutes,
        specification_score,
        logic_score,
        performance_score,
        design_score,
        other_score,
        remark,
        deleted,
        created_at,
        updated_at
      ) values (
        #{record.gitlabBaseUrl},
        #{record.projectId},
        #{record.mrIid},
        #{record.resourceType},
        #{record.resourceId},
        #{record.templateCode},
        #{record.formTitle},
        #{record.reviewer},
        #{record.reviewDurationMinutes},
        #{record.specificationScore},
        #{record.logicScore},
        #{record.performanceScore},
        #{record.designScore},
        #{record.otherScore},
        #{record.remark},
        #{record.deleted},
        current_timestamp,
        current_timestamp
      )
      on conflict (gitlab_base_url, project_id, resource_type, resource_id, template_code)
      do update set
        mr_iid = excluded.mr_iid,
        form_title = excluded.form_title,
        reviewer = excluded.reviewer,
        review_duration_minutes = excluded.review_duration_minutes,
        specification_score = excluded.specification_score,
        logic_score = excluded.logic_score,
        performance_score = excluded.performance_score,
        design_score = excluded.design_score,
        other_score = excluded.other_score,
        remark = excluded.remark,
        deleted = excluded.deleted,
        updated_at = current_timestamp
      """)
  void upsert(@Param("record") CollectFormRecord record);

  @Update("""
      update collect_form_records
         set deleted = true,
             updated_at = current_timestamp
       where gitlab_base_url = #{gitlabBaseUrl}
         and project_id = #{projectId}
         and resource_type = #{resourceType}
         and resource_id = #{resourceId}
         and template_code = #{templateCode}
      """)
  int logicalDelete(
      @Param("gitlabBaseUrl") String gitlabBaseUrl,
      @Param("projectId") Long projectId,
      @Param("resourceType") String resourceType,
      @Param("resourceId") String resourceId,
      @Param("templateCode") String templateCode);
}
