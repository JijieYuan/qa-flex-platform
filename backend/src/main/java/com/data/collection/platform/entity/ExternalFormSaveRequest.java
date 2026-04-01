package com.data.collection.platform.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExternalFormSaveRequest {
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

  @NotBlank
  private String formTitle;

  @NotBlank
  private String reviewer;

  @NotNull
  @Min(0)
  private Integer reviewDurationMinutes;

  @NotNull
  @Min(0)
  private Integer specificationScore;

  @NotNull
  @Min(0)
  private Integer logicScore;

  @NotNull
  @Min(0)
  private Integer performanceScore;

  @NotNull
  @Min(0)
  private Integer designScore;

  @NotNull
  @Min(0)
  private Integer otherScore;

  private String remark;
}
