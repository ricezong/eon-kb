<script setup lang="ts">
import { computed } from 'vue';
import type { KnowledgeBase } from '@/api/types';

const props = defineProps<{
  kb: Pick<KnowledgeBase, 'name' | 'color'>;
  size?: 'small' | 'medium';
  clickable?: boolean;
}>();
const emit = defineEmits<{ (e: 'click'): void }>();

/** 把 #rrggbb 变成带透明度的柔和背景，同时保证文字对比度 */
const style = computed(() => {
  const c = props.kb.color || '#6366f1';
  return {
    background: `color-mix(in srgb, ${c} 14%, transparent)`,
    color: c,
    borderColor: `color-mix(in srgb, ${c} 32%, transparent)`
  };
});

const sizeCls = computed(() => (props.size === 'small' ? 'is-small' : 'is-medium'));
</script>

<template>
  <span
    class="kb-tag" :class="[sizeCls, { clickable }]"
    :style="style"
    @click="clickable && emit('click')"
  >
    <span class="kb-dot" :style="{ background: kb.color || '#6366f1' }" />
    <span class="kb-name">{{ kb.name }}</span>
  </span>
</template>

<style scoped>
.kb-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid;
  border-radius: var(--radius-full);
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  transition: transform var(--transition-fast), filter var(--transition-fast);
  user-select: none;
}
.kb-tag.is-small { padding: 3px 9px; font-size: 11px; }
.kb-tag.is-medium { padding: 5px 11px; font-size: 12px; }
.kb-tag.clickable { cursor: pointer; }
.kb-tag.clickable:hover { transform: translateY(-1px); filter: brightness(1.06); }
.kb-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.kb-name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
