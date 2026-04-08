package com.data.collection.platform.service;

import java.time.LocalDateTime;

final class IssueLegacyRules {
  private IssueLegacyRules() {
  }

  static boolean isLegacy(boolean closed, LocalDateTime createdAt, LocalDateTime phaseStartAt) {
    return !closed && createdAt != null && phaseStartAt != null && createdAt.isBefore(phaseStartAt);
  }
}
