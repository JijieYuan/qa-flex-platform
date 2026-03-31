<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import StatisticBoardView from '../components/StatisticBoardView.vue';

const route = useRoute();

const boardKey = computed(() => {
  if (route.meta.pageKey === 'question-metrics-home') {
    return 'system-test-defect-summary';
  }
  return 'mirror-table-overview';
});
</script>

<template>
  <StatisticBoardView :board-key="boardKey" />
</template>
