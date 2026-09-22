<script setup lang="ts">
import { computed } from 'vue';
import { NTag, NIcon } from 'naive-ui';
import {
  TimeOutline, CheckmarkCircleOutline, CloseCircleOutline, EllipseOutline
} from '@vicons/ionicons5';
import type { DocumentStatus } from '@/api/types';

const props = defineProps<{ status: DocumentStatus | null | string }>();

const meta = computed(() => {
  switch ((props.status || '').toUpperCase()) {
    case 'COMPLETED':
      return { type: 'success' as const, label: '已完成', icon: CheckmarkCircleOutline };
    case 'PROCESSING':
      return { type: 'info' as const, label: '处理中', icon: TimeOutline };
    case 'FAILED':
      return { type: 'error' as const, label: '失败', icon: CloseCircleOutline };
    case 'PENDING':
      return { type: 'warning' as const, label: '排队中', icon: EllipseOutline };
    default:
      return { type: 'default' as const, label: props.status || '未知', icon: EllipseOutline };
  }
});
</script>

<template>
  <NTag :type="meta.type" :bordered="false" round size="small">
    <template #icon>
      <NIcon :component="meta.icon" :class="{ spinning: status === 'PROCESSING' }" />
    </template>
    {{ meta.label }}
  </NTag>
</template>

<style scoped>
.spinning { animation: spin 1.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
