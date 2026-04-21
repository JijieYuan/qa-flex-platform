package com.data.collection.platform.service;

import com.data.collection.platform.entity.ReviewDataRecordRowResponse;

final class ReviewDataSearchSupport {
  private ReviewDataSearchSupport() {
  }

  static boolean matchesKeyword(ReviewDataRecordRowResponse row, String keyword) {
    return TextQuerySupport.containsAbstractSearch(row.title(), keyword)
        || TextQuerySupport.containsAbstractSearch(row.projectName(), keyword)
        || TextQuerySupport.containsAbstractSearch(row.moduleName(), keyword)
        || TextQuerySupport.containsAbstractSearch(row.reviewOwner(), keyword)
        || TextQuerySupport.containsAbstractSearch(row.reviewType(), keyword)
        || TextQuerySupport.containsAbstractSearch(row.reviewExpertsSummary(), keyword);
  }

  static boolean matchesContains(String source, String keyword) {
    return TextQuerySupport.containsAbstractSearch(source, keyword);
  }
}
