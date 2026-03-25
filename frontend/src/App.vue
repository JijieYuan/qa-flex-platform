<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="header">
          <span>QA Flex Platform</span>
          <el-tag type="success">Initialized</el-tag>
        </div>
      </template>
      <el-space direction="vertical" alignment="start" :size="16">
        <el-text size="large">Vue 3 + Element Plus frontend initialized successfully.</el-text>
        <el-text type="info">Backend default endpoint: http://localhost:18080/</el-text>
        <el-button type="primary">Element Plus Ready</el-button>
      </el-space>
    </el-card>
  </div>
</template>
