<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const title = computed(() => String(route.meta.title ?? '模块页面'));
const description = computed(() => String(route.meta.description ?? '当前模块暂未接入统计表。'));
</script>

<template>
  <section class="blank-stage">
    <el-empty :description="description">
      <template #image>
        <div class="placeholder-badge">{{ title }}</div>
      </template>
    </el-empty>
  </section>
</template>

<style scoped>
.placeholder-badge {
  min-width: 120px;
  padding: 12px 20px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  font-weight: 700;
  text-align: center;
}
</style>

