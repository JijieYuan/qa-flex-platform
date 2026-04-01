package com.data.collection.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.data.collection.platform.entity.ExternalFormContextRequest;
import com.data.collection.platform.entity.ExternalFormRecord;
import com.data.collection.platform.entity.ExternalFormResponse;
import com.data.collection.platform.entity.ExternalFormSaveRequest;
import com.data.collection.platform.mapper.ExternalFormRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExternalFormService {

  private final ExternalFormRecordMapper externalFormRecordMapper;

  public ExternalFormService(ExternalFormRecordMapper externalFormRecordMapper) {
    this.externalFormRecordMapper = externalFormRecordMapper;
  }

  public ExternalFormResponse getByContext(ExternalFormContextRequest request) {
    ExternalFormRecord record = externalFormRecordMapper.selectOne(
        new LambdaQueryWrapper<ExternalFormRecord>()
            .eq(ExternalFormRecord::getGitlabBaseUrl, normalize(request.getGitlabBaseUrl()))
            .eq(ExternalFormRecord::getProjectId, request.getProjectId())
            .eq(ExternalFormRecord::getResourceType, normalize(request.getResourceType()))
            .eq(ExternalFormRecord::getResourceId, normalize(request.getResourceId()))
            .eq(ExternalFormRecord::getTemplateCode, normalize(request.getTemplateCode()))
            .last("limit 1"));

    if (record == null) {
      return ExternalFormResponse.builder()
          .found(false)
          .gitlabBaseUrl(normalize(request.getGitlabBaseUrl()))
          .projectId(request.getProjectId())
          .mrIid(request.getMrIid())
          .resourceType(normalize(request.getResourceType()))
          .resourceId(normalize(request.getResourceId()))
          .templateCode(normalize(request.getTemplateCode()))
          .formTitle(defaultTitle(request.getTemplateCode()))
          .reviewDurationMinutes(1)
          .specificationScore(0)
          .logicScore(0)
          .performanceScore(0)
          .designScore(0)
          .otherScore(0)
          .deleted(false)
          .build();
    }

    return toResponse(record, true);
  }

  public ExternalFormResponse save(ExternalFormSaveRequest request) {
    ExternalFormRecord record = new ExternalFormRecord();
    record.setGitlabBaseUrl(normalize(request.getGitlabBaseUrl()));
    record.setProjectId(request.getProjectId());
    record.setMrIid(request.getMrIid());
    record.setResourceType(normalize(request.getResourceType()));
    record.setResourceId(normalize(request.getResourceId()));
    record.setTemplateCode(normalize(request.getTemplateCode()));
    record.setFormTitle(StringUtils.hasText(request.getFormTitle()) ? request.getFormTitle().trim() : defaultTitle(request.getTemplateCode()));
    record.setReviewer(request.getReviewer().trim());
    record.setReviewDurationMinutes(request.getReviewDurationMinutes());
    record.setSpecificationScore(request.getSpecificationScore());
    record.setLogicScore(request.getLogicScore());
    record.setPerformanceScore(request.getPerformanceScore());
    record.setDesignScore(request.getDesignScore());
    record.setOtherScore(request.getOtherScore());
    record.setRemark(trimToNull(request.getRemark()));

    externalFormRecordMapper.upsert(record);
    return getByContext(toContextRequest(request));
  }

  public ExternalFormResponse delete(ExternalFormContextRequest request) {
    externalFormRecordMapper.markDeleted(
        normalize(request.getGitlabBaseUrl()),
        request.getProjectId(),
        normalize(request.getResourceType()),
        normalize(request.getResourceId()),
        normalize(request.getTemplateCode()));
    return getByContext(request);
  }

  private ExternalFormContextRequest toContextRequest(ExternalFormSaveRequest request) {
    ExternalFormContextRequest contextRequest = new ExternalFormContextRequest();
    contextRequest.setGitlabBaseUrl(request.getGitlabBaseUrl());
    contextRequest.setProjectId(request.getProjectId());
    contextRequest.setMrIid(request.getMrIid());
    contextRequest.setResourceType(request.getResourceType());
    contextRequest.setResourceId(request.getResourceId());
    contextRequest.setTemplateCode(request.getTemplateCode());
    return contextRequest;
  }

  private String defaultTitle(String templateCode) {
    return "code_review".equalsIgnoreCase(normalize(templateCode)) ? "代码走查表" : "通用采集表";
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private ExternalFormResponse toResponse(ExternalFormRecord record, boolean found) {
    return ExternalFormResponse.builder()
        .found(found)
        .id(record.getId())
        .gitlabBaseUrl(record.getGitlabBaseUrl())
        .projectId(record.getProjectId())
        .mrIid(record.getMrIid())
        .resourceType(record.getResourceType())
        .resourceId(record.getResourceId())
        .templateCode(record.getTemplateCode())
        .formTitle(record.getFormTitle())
        .reviewer(record.getReviewer())
        .reviewDurationMinutes(record.getReviewDurationMinutes())
        .specificationScore(record.getSpecificationScore())
        .logicScore(record.getLogicScore())
        .performanceScore(record.getPerformanceScore())
        .designScore(record.getDesignScore())
        .otherScore(record.getOtherScore())
        .remark(record.getRemark())
        .deleted(Boolean.TRUE.equals(record.getDeleted()))
        .createdAt(record.getCreatedAt())
        .updatedAt(record.getUpdatedAt())
        .build();
  }
}
