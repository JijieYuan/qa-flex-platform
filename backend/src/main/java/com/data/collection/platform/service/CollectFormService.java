package com.data.collection.platform.service;

import com.data.collection.platform.common.exception.BizException;
import com.data.collection.platform.entity.CollectFormDetailResponse;
import com.data.collection.platform.entity.CollectFormRecord;
import com.data.collection.platform.mapper.CollectFormRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CollectFormService {

  private final CollectFormRecordMapper collectFormRecordMapper;

  public CollectFormService(CollectFormRecordMapper collectFormRecordMapper) {
    this.collectFormRecordMapper = collectFormRecordMapper;
  }

  public CollectFormDetailResponse getDetail(
      String gitlabBaseUrl,
      Long projectId,
      String resourceType,
      String resourceId,
      String templateCode) {
    validateContext(gitlabBaseUrl, projectId, resourceType, resourceId, templateCode);
    CollectFormRecord record = collectFormRecordMapper.selectByContext(
        gitlabBaseUrl.trim(),
        projectId,
        resourceType.trim(),
        resourceId.trim(),
        templateCode.trim());
    return record == null ? null : CollectFormDetailResponse.from(record);
  }

  public CollectFormDetailResponse save(
      String gitlabBaseUrl,
      Long projectId,
      Long mrIid,
      String resourceType,
      String resourceId,
      String templateCode,
      String formTitle,
      String reviewer,
      Integer reviewDurationMinutes,
      Integer specificationScore,
      Integer logicScore,
      Integer performanceScore,
      Integer designScore,
      Integer otherScore,
      String remark) {
    validateContext(gitlabBaseUrl, projectId, resourceType, resourceId, templateCode);
    CollectFormRecord record = new CollectFormRecord();
    record.setGitlabBaseUrl(gitlabBaseUrl.trim());
    record.setProjectId(projectId);
    record.setMrIid(mrIid);
    record.setResourceType(resourceType.trim());
    record.setResourceId(resourceId.trim());
    record.setTemplateCode(templateCode.trim());
    record.setFormTitle(StringUtils.hasText(formTitle) ? formTitle.trim() : defaultTitle(record.getTemplateCode()));
    record.setReviewer(StringUtils.hasText(reviewer) ? reviewer.trim() : "");
    record.setReviewDurationMinutes(normalizeScore(reviewDurationMinutes));
    record.setSpecificationScore(normalizeScore(specificationScore));
    record.setLogicScore(normalizeScore(logicScore));
    record.setPerformanceScore(normalizeScore(performanceScore));
    record.setDesignScore(normalizeScore(designScore));
    record.setOtherScore(normalizeScore(otherScore));
    record.setRemark(StringUtils.hasText(remark) ? remark.trim() : "");
    record.setDeleted(false);
    collectFormRecordMapper.upsert(record);
    return getDetail(
        record.getGitlabBaseUrl(),
        record.getProjectId(),
        record.getResourceType(),
        record.getResourceId(),
        record.getTemplateCode());
  }

  public boolean delete(
      String gitlabBaseUrl,
      Long projectId,
      String resourceType,
      String resourceId,
      String templateCode) {
    validateContext(gitlabBaseUrl, projectId, resourceType, resourceId, templateCode);
    return collectFormRecordMapper.logicalDelete(
            gitlabBaseUrl.trim(),
            projectId,
            resourceType.trim(),
            resourceId.trim(),
            templateCode.trim())
        > 0;
  }

  private void validateContext(
      String gitlabBaseUrl,
      Long projectId,
      String resourceType,
      String resourceId,
      String templateCode) {
    if (!StringUtils.hasText(gitlabBaseUrl)) {
      throw new BizException("GitLab 来源地址不能为空");
    }
    if (projectId == null || projectId <= 0) {
      throw new BizException("Project ID 无效");
    }
    if (!StringUtils.hasText(resourceType)) {
      throw new BizException("资源类型不能为空");
    }
    if (!StringUtils.hasText(resourceId)) {
      throw new BizException("资源编号不能为空");
    }
    if (!StringUtils.hasText(templateCode)) {
      throw new BizException("模板编码不能为空");
    }
  }

  private Integer normalizeScore(Integer value) {
    return value == null || value < 0 ? 0 : value;
  }

  private String defaultTitle(String templateCode) {
    return "code_review".equals(templateCode) ? "代码走查表" : "采集表单";
  }
}
