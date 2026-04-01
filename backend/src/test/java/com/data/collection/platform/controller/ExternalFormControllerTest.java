package com.data.collection.platform.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.data.collection.platform.entity.ExternalFormContextRequest;
import com.data.collection.platform.entity.ExternalFormResponse;
import com.data.collection.platform.entity.ExternalFormSaveRequest;
import com.data.collection.platform.service.ExternalFormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ExternalFormControllerTest {

  @Mock
  private ExternalFormService externalFormService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ExternalFormController(externalFormService)).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void shouldReturnDetailByContext() throws Exception {
    when(externalFormService.getByContext(any(ExternalFormContextRequest.class)))
        .thenReturn(ExternalFormResponse.builder()
            .found(true)
            .projectId(88L)
            .resourceType("merge_request")
            .resourceId("12")
            .templateCode("code_review")
            .formTitle("代码走查表")
            .reviewer("王会欢")
            .reviewDurationMinutes(5)
            .specificationScore(1)
            .logicScore(2)
            .performanceScore(3)
            .designScore(4)
            .otherScore(5)
            .build());

    mockMvc.perform(get("/api/external-forms/detail")
            .param("gitlabBaseUrl", "http://172.22.10.233")
            .param("projectId", "88")
            .param("resourceType", "merge_request")
            .param("resourceId", "12")
            .param("templateCode", "code_review"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.formTitle").value("代码走查表"))
        .andExpect(jsonPath("$.data.reviewer").value("王会欢"));
  }

  @Test
  void shouldSaveFormRecord() throws Exception {
    when(externalFormService.save(any(ExternalFormSaveRequest.class)))
        .thenReturn(ExternalFormResponse.builder()
            .found(true)
            .projectId(88L)
            .resourceType("merge_request")
            .resourceId("12")
            .templateCode("code_review")
            .formTitle("代码走查表")
            .reviewer("王会欢")
            .reviewDurationMinutes(5)
            .specificationScore(1)
            .logicScore(2)
            .performanceScore(3)
            .designScore(4)
            .otherScore(5)
            .build());

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

    mockMvc.perform(post("/api/external-forms/save")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.otherScore").value(5));
  }
}
