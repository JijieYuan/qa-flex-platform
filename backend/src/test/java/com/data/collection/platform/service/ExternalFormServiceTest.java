package com.data.collection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.collection.platform.entity.ExternalFormContextRequest;
import com.data.collection.platform.entity.ExternalFormRecord;
import com.data.collection.platform.entity.ExternalFormResponse;
import com.data.collection.platform.entity.ExternalFormSaveRequest;
import com.data.collection.platform.mapper.ExternalFormRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExternalFormServiceTest {

  private ExternalFormRecordMapper externalFormRecordMapper;
  private ExternalFormService externalFormService;

  @BeforeEach
  void setUp() {
    externalFormRecordMapper = Mockito.mock(ExternalFormRecordMapper.class);
    externalFormService = new ExternalFormService(externalFormRecordMapper);
  }

  @Test
  void shouldReturnDefaultFormWhenNoRecordExists() {
    when(externalFormRecordMapper.selectOne(any())).thenReturn(null);

    ExternalFormContextRequest request = new ExternalFormContextRequest();
    request.setGitlabBaseUrl("http://172.22.10.233");
    request.setProjectId(88L);
    request.setMrIid(12L);
    request.setResourceType("merge_request");
    request.setResourceId("12");
    request.setTemplateCode("code_review");

    ExternalFormResponse response = externalFormService.getByContext(request);

    assertThat(response.isFound()).isFalse();
    assertThat(response.getFormTitle()).isEqualTo("代码走查表");
    assertThat(response.getReviewDurationMinutes()).isEqualTo(1);
    assertThat(response.getSpecificationScore()).isZero();
  }

  @Test
  void shouldUpsertAndReloadSavedRecord() {
    ExternalFormRecord savedRecord = new ExternalFormRecord();
    savedRecord.setId(1L);
    savedRecord.setGitlabBaseUrl("http://172.22.10.233");
    savedRecord.setProjectId(88L);
    savedRecord.setMrIid(12L);
    savedRecord.setResourceType("merge_request");
    savedRecord.setResourceId("12");
    savedRecord.setTemplateCode("code_review");
    savedRecord.setFormTitle("代码走查表");
    savedRecord.setReviewer("王会欢");
    savedRecord.setReviewDurationMinutes(5);
    savedRecord.setSpecificationScore(1);
    savedRecord.setLogicScore(2);
    savedRecord.setPerformanceScore(3);
    savedRecord.setDesignScore(4);
    savedRecord.setOtherScore(5);
    savedRecord.setDeleted(false);

    when(externalFormRecordMapper.selectOne(any())).thenReturn(savedRecord);

    ExternalFormSaveRequest request = new ExternalFormSaveRequest();
    request.setGitlabBaseUrl("http://172.22.10.233");
    request.setProjectId(88L);
    request.setMrIid(12L);
    request.setResourceType("merge_request");
    request.setResourceId("12");
    request.setTemplateCode("code_review");
    request.setFormTitle("代码走查表");
    request.setReviewer("王会欢");
    request.setReviewDurationMinutes(5);
    request.setSpecificationScore(1);
    request.setLogicScore(2);
    request.setPerformanceScore(3);
    request.setDesignScore(4);
    request.setOtherScore(5);
    request.setRemark("补充说明");

    ExternalFormResponse response = externalFormService.save(request);

    verify(externalFormRecordMapper).upsert(any(ExternalFormRecord.class));
    assertThat(response.isFound()).isTrue();
    assertThat(response.getReviewer()).isEqualTo("王会欢");
    assertThat(response.getOtherScore()).isEqualTo(5);
  }

  @Test
  void shouldMarkRecordDeleted() {
    when(externalFormRecordMapper.markDeleted(eq("http://172.22.10.233"), eq(88L), eq("merge_request"), eq("12"), eq("code_review")))
        .thenReturn(1);
    when(externalFormRecordMapper.selectOne(any())).thenReturn(null);

    ExternalFormContextRequest request = new ExternalFormContextRequest();
    request.setGitlabBaseUrl("http://172.22.10.233");
    request.setProjectId(88L);
    request.setMrIid(12L);
    request.setResourceType("merge_request");
    request.setResourceId("12");
    request.setTemplateCode("code_review");

    ExternalFormResponse response = externalFormService.delete(request);

    verify(externalFormRecordMapper).markDeleted("http://172.22.10.233", 88L, "merge_request", "12", "code_review");
    assertThat(response.isFound()).isFalse();
  }
}
