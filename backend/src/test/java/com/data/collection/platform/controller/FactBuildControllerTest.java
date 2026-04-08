package com.data.collection.platform.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.data.collection.platform.entity.FactBuildResponse;
import com.data.collection.platform.service.FactBuildService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FactBuildControllerTest {

  @Mock
  private FactBuildService factBuildService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new FactBuildController(factBuildService)).build();
  }

  @Test
  void shouldRebuildAllFacts() throws Exception {
    when(factBuildService.rebuildAllFacts(false))
        .thenReturn(new FactBuildResponse("all", false, 12, "事实表构建完成"));

    mockMvc.perform(post("/api/facts/rebuild"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.scope").value("all"))
        .andExpect(jsonPath("$.data.affectedRows").value(12));
  }

  @Test
  void shouldRebuildIssueFactsInFullMode() throws Exception {
    when(factBuildService.rebuildIssueFacts(true))
        .thenReturn(new FactBuildResponse("issue", true, 6, "议题事实已全量构建"));

    mockMvc.perform(post("/api/facts/rebuild").param("scope", "issue").param("full", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.scope").value("issue"))
        .andExpect(jsonPath("$.data.full").value(true))
        .andExpect(jsonPath("$.data.affectedRows").value(6));
  }
}
