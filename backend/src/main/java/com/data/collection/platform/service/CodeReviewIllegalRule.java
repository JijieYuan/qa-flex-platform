package com.data.collection.platform.service;

import java.util.function.Predicate;

record CodeReviewIllegalRule(
    String key,
    String label,
    Predicate<CodeReviewIllegalRecordSource> matcher) {

  boolean matches(CodeReviewIllegalRecordSource source) {
    return matcher.test(source);
  }
}
