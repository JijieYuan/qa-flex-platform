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
    assertTrue(TextQuerySupport.containsAbstractSearch("王强", "wq"));
    assertTrue(TextQuerySupport.containsAbstractSearch("王qiang", "wq"));
    assertTrue(TextQuerySupport.containsAbstractSearch("[草图模块] 算数功能设计说明书评审", "ct"));
    assertTrue(TextQuerySupport.containsAbstractSearch("Wang Qiang", "wangqiang"));
    assertFalse(TextQuerySupport.containsAbstractSearch("发布模块", "wq"));
  }

  @Test
  void shouldNormalizeDisplaySafely() {
    assertEquals("", TextQuerySupport.normalizeDisplay("   "));
    assertEquals("Module A", TextQuerySupport.normalizeDisplay("  Module A "));
    assertNull(TextQuerySupport.trimToNull("   "));
  }
}
