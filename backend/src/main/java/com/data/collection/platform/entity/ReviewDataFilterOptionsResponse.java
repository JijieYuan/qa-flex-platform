package com.data.collection.platform.entity;

import java.util.List;

public record ReviewDataFilterOptionsResponse(
    List<OptionItemResponse> projectNames,
    List<OptionItemResponse> repositoryNames,
    List<OptionItemResponse> moduleNames,
    List<OptionItemResponse> reviewers,
    List<OptionItemResponse> templateCodes,
    List<OptionItemResponse> targetBranches,
    List<OptionItemResponse> recordStatuses) {
}
