package com.data.collection.platform.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExternalFormContextRequest {
  @NotBlank
  private String gitlabBaseUrl;

  @NotNull
  private Long projectId;

  private Long mrIid;

  @NotBlank
  private String resourceType;

  @NotBlank
  private String resourceId;

  @NotBlank
  private String templateCode;
}
