package com.data.collection.platform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.data.collection.platform.entity.CollectFormRecord;
import com.data.collection.platform.mapper.CollectFormRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectFormServiceTest {

  @Mock
  private CollectFormRecordMapper collectFormRecordMapper;

  @InjectMocks
  private CollectFormService collectFormService;

  @Test
  void saveShouldUpsertPlatformRecordWithNormalizedContext() {
    CollectFormRecord saved = new CollectFormRecord();
    saved.setId(7L);
    saved.setGitlabBaseUrl("http://172.22.10.233");
    saved.setProjectId(88L);
    saved.setMrIid(12L);
    saved.setResourceType("merge_request");
    saved.setResourceId("12");
    saved.setTemplateCode("code_review");
    saved.setFormTitle("代码走查表");
    saved.setReviewer("王小欢");
    saved.setReviewDurationMinutes(5);
    saved.setSpecificationScore(1);
    saved.setLogicScore(2);
    saved.setPerformanceScore(3);
    saved.setDesignScore(4);
    saved.setOtherScore(5);

    when(collectFormRecordMapper.selectByContext(
        eq("http://172.22.10.233"),
        eq(88L),
        eq("merge_request"),
        eq("12"),
        eq("code_review"))).thenReturn(saved);

    collectFormService.save(
        "http://172.22.10.233",
        88L,
        12L,
        "merge_request",
        "12",
        "code_review",
        "代码走查表",
        "王小欢",
        5,
        1,
        2,
        3,
        4,
        5,
        "本地测试");

    ArgumentCaptor<CollectFormRecord> captor = ArgumentCaptor.forClass(CollectFormRecord.class);
    verify(collectFormRecordMapper).upsert(captor.capture());
    CollectFormRecord record = captor.getValue();
    assertEquals("http://172.22.10.233", record.getGitlabBaseUrl());
    assertEquals(88L, record.getProjectId());
    assertEquals(12L, record.getMrIid());
    assertEquals("merge_request", record.getResourceType());
    assertEquals("12", record.getResourceId());
    assertEquals("code_review", record.getTemplateCode());
    assertEquals("王小欢", record.getReviewer());
    assertEquals(5, record.getReviewDurationMinutes());
    assertTrue(!record.isDeleted());
  }

  @Test
  void deleteShouldDelegateLogicalDeleteByUniqueContext() {
    when(collectFormRecordMapper.logicalDelete(
        eq("http://172.22.10.233"),
        eq(1L),
        eq("merge_request"),
        eq("2"),
        eq("code_review"))).thenReturn(1);

    boolean deleted = collectFormService.delete(
        "http://172.22.10.233",
        1L,
        "merge_request",
        "2",
        "code_review");

    assertTrue(deleted);
  }
}
