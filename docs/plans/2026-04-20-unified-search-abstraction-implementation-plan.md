# Unified Search Abstraction Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 统一前后端业务搜索的抽象匹配能力，让 `SmartSelect`、列表关键字搜索和条件筛选复用同一套首字母/拼音模糊规则。

**Architecture:** 前端抽出共享 matcher，保留 `SmartSelect` 作为消费方；后端补充通用搜索支持类，统一提供原文、紧凑文本、全拼和首字母匹配，并让 ReviewData 与 CodeReviewIllegalRecord 共用。数据库浏览器不纳入本次服务端统一范围。

**Tech Stack:** Vue 3、TypeScript、Vitest、Spring Boot、Java 21、Hutool、JUnit 5

---

### Task 1: Frontend Shared Matcher

**Files:**
- Modify: `frontend/src/components/base/smart-select-search.ts`
- Test: `frontend/src/components/base/smart-select-search.test.ts`

**Step 1: Write the failing test**

补充测试覆盖以下场景：

- `王qiang` 可被 `wq` 命中
- `[草图模块] 算数功能设计说明书评审` 可被 `ct` 命中
- 英文和中文混合文本仍支持紧凑匹配

**Step 2: Run test to verify it fails**

Run: `npm test -- --run src/components/base/smart-select-search.test.ts`

**Step 3: Write minimal implementation**

把现有 `smart-select-search.ts` 提炼为更通用的文本 token 匹配逻辑，统一生成：

- normalized text
- compact text
- full pinyin
- initials

并对混合中英文输入做稳定规整。

**Step 4: Run test to verify it passes**

Run: `npm test -- --run src/components/base/smart-select-search.test.ts`

**Step 5: Commit**

暂不提交，待整个搜索链路一起验证后统一处理。

### Task 2: Backend Shared Search Support

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/TextQuerySupport.java`
- Create: `backend/src/test/java/com/data/collection/platform/service/TextQuerySupportTest.java`

**Step 1: Write the failing test**

新增测试覆盖：

- `王强` 被 `wq` 命中
- `王qiang` 被 `wq` 命中
- `[草图模块] 算数功能设计说明书评审` 被 `ct` 命中
- 原始英文大小写无关匹配仍然成立

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=TextQuerySupportTest test`

**Step 3: Write minimal implementation**

在 `TextQuerySupport` 中新增统一 matcher 能力，依赖 Hutool 拼音工具生成：

- normalized text
- compact text
- pinyin text
- initials text

并提供统一的 `containsAbstractSearch` 类方法。

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=TextQuerySupportTest test`

**Step 5: Commit**

暂不提交，待业务接入后统一处理。

### Task 3: Wire Code Review Search

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/CodeReviewIllegalRecordQuerySupport.java`
- Test: `backend/src/test/java/com/data/collection/platform/service/CodeReviewIllegalRecordQuerySupportTest.java`

**Step 1: Write the failing test**

验证关键字 `ct`、`wq` 可命中对应中文/混合字段。

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=CodeReviewIllegalRecordQuerySupportTest test`

**Step 3: Write minimal implementation**

让 `matchesKeyword` 改为复用 `TextQuerySupport` 的抽象搜索匹配。

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=CodeReviewIllegalRecordQuerySupportTest test`

**Step 5: Commit**

暂不提交，待业务接入后统一处理。

### Task 4: Wire Review Data Search

**Files:**
- Modify: `backend/src/main/java/com/data/collection/platform/service/ReviewDataRecordService.java`
- Test: `backend/src/test/java/com/data/collection/platform/service/ReviewDataRecordServiceTest.java`

**Step 1: Write the failing test**

验证：

- 关键字搜索支持拼音首字母
- `contains` 支持抽象搜索
- `notContains` 支持抽象搜索取反

**Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ReviewDataRecordServiceTest test`

**Step 3: Write minimal implementation**

把 `ReviewDataRecordService` 中相关 `contains` / `notContains` 判断改为统一 matcher。

**Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ReviewDataRecordServiceTest test`

**Step 5: Commit**

暂不提交，待全部验证后统一处理。

### Task 5: End-to-End Verification

**Files:**
- Verify only

**Step 1: Run frontend targeted tests**

Run: `npm test -- --run src/components/base/smart-select-search.test.ts src/router.test.ts`

**Step 2: Run backend targeted tests**

Run: `mvn -Dtest=TextQuerySupportTest,CodeReviewIllegalRecordQuerySupportTest,ReviewDataRecordServiceTest test`

**Step 3: Review diffs**

Run: `git diff -- frontend/src/components/base/smart-select-search.ts backend/src/main/java/com/data/collection/platform/service/TextQuerySupport.java backend/src/main/java/com/data/collection/platform/service/CodeReviewIllegalRecordQuerySupport.java backend/src/main/java/com/data/collection/platform/service/ReviewDataRecordService.java`

**Step 4: Final check**

确认没有重复实现拼音逻辑，没有把数据库浏览器错误纳入本次统一范围。

**Step 5: Commit**

暂不提交，等待用户确认。
