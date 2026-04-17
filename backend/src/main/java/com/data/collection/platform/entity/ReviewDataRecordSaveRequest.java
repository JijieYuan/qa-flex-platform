package com.data.collection.platform.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record ReviewDataRecordSaveRequest(
    @NotBlank String projectName,
    @NotBlank String title,
    @NotBlank String moduleName,
    @NotBlank String reviewType,
    @NotNull LocalDate reviewDate,
    @NotBlank String reviewOwner,
    @NotEmpty List<String> reviewExperts,
    @NotNull @Min(0) Integer reviewScalePages,
    @NotBlank String reviewProduct,
    @NotBlank String authorName,
    @NotBlank String reviewVersion) {}
