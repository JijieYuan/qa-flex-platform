package com.data.collection.platform.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TextQuerySupportTest {
  @Test
  void shouldNormalizeAndMatchIgnoringCase() {
    assertEquals("hello", TextQuerySupport.normalizeForMatch("  HeLLo "));
    assertTrue(TextQuerySupport.equalsNormalized(" Owner ", "owner"));
    assertTrue(TextQuerySupport.containsIgnoreCase("Feature/MAIN", "main"));
    assertFalse(TextQuerySupport.containsIgnoreCase("release", "main"));
  }

  @Test
  void shouldNormalizeDisplaySafely() {
    assertEquals("", TextQuerySupport.normalizeDisplay("   "));
    assertEquals("Module A", TextQuerySupport.normalizeDisplay("  Module A "));
    assertNull(TextQuerySupport.trimToNull("   "));
  }
}
