package com.data.collection.platform.service;

import java.util.List;

record CodeReviewIllegalRuleGroup(
    String key,
    String title,
    String description,
    List<String> ruleKeys) {
}
