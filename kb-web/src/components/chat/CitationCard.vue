<script setup lang="ts">
import { computed } from 'vue';
import { NIcon, NTag } from 'naive-ui';
import { DocumentTextOutline } from '@vicons/ionicons5';
import type { Citation } from '@/api/types';
import { useUiStore } from '@/stores/ui';

const props = defineProps<{ citation: Citation }>();
const ui = useUiStore();

const scorePct = computed(() => Math.round(props.citation.score * 100));

const typeColor = computed(() => {
  const t = (props.citation.chunkType || '').toLowerCase();
  if (t.includes('head') || t.includes('title')) return 'info';
  if (t.includes('para') || t.includes('text')) return 'success';
  if (t.includes('table') || t.includes('sheet')) return 'warning';
  if (t.includes('slide')) return 'error';
  return 'default';
});
</script>

<template>
  <div class="citation-card" @click="ui.openChunkPreview(citation.id)">
    <div class="cite-head">
      <span class="cite-index">[{{ citation.index }}]</span>
      <span class="cite-title" :title="citation.title || citation.documentName">
        {{ citation.title || '未命名切片' }}
      </span>
      <NTag :type="typeColor" size="tiny" round :bordered="false" v-if="citation.chunkType">
        {{ citation.chunkType }}
      </NTag>
    </div>

    <div class="cite-doc">
      <NIcon :size="12"><DocumentTextOutline /></NIcon>
      <span class="doc-name">{{ citation.documentName }}</span>
      <span class="dot">·</span>
      <span class="score" :title="`相关度 ${(citation.score * 100).toFixed(2)}%`">
        相关度 {{ scorePct }}%
      </span>
    </div>

    <div class="cite-preview">{{ citation.preview }}</div>
  </div>
</template>

<style scoped>
.citation-card {
  display: flex; flex-direction: column; gap: 6px;
  padding: 10px 12px;
  background: var(--bg-subtle);
  border: 1px solid var(--border-default);
  border-left: 3px solid var(--brand-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast),
    border-color var(--transition-fast), background var(--transition-fast);
}
.citation-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  border-left-color: var(--brand-accent);
  background: var(--bg-elevated);
}

.cite-head {
  display: flex; align-items: center; gap: 8px;
  min-width: 0;
}
.cite-index {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  font-weight: 700;
  color: var(--brand-primary);
  flex-shrink: 0;
}
.cite-title {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.cite-doc {
  display: flex; align-items: center; gap: 5px;
  font-size: 11px;
  color: var(--text-tertiary);
}
.doc-name {
  max-width: 220px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.dot { opacity: 0.5; }
.score { font-variant-numeric: tabular-nums; }

.cite-preview {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
