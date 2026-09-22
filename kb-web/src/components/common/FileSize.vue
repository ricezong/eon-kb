<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ bytes: number | null | undefined }>();

const display = computed(() => {
  const b = props.bytes;
  if (b == null || b <= 0) return '—';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let v = b;
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
  return `${v.toFixed(v >= 100 || i === 0 ? 0 : 1)} ${units[i]}`;
});
</script>

<template>
  <span class="filesize">{{ display }}</span>
</template>

<style scoped>
.filesize {
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  font-size: 12px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
</style>
