<script setup lang="ts">
import { ref, watch } from 'vue';
import {
  NDrawer, NDrawerContent, NSpin, NTag, NIcon, NEmpty, NButton,
  NDescriptions, NDescriptionsItem, useMessage
} from 'naive-ui';
import { DocumentTextOutline, CopyOutline } from '@vicons/ionicons5';
import { chunkApi } from '@/api';
import type { Chunk } from '@/api/types';
import { useUiStore } from '@/stores/ui';
import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue';

const ui = useUiStore();
const message = useMessage();

const loading = ref(false);
const chunk = ref<Chunk | null>(null);
const error = ref<string | null>(null);

watch(
  () => [ui.chunkPreviewOpen, ui.chunkPreviewId] as const,
  async ([open, id]) => {
    if (!open || !id) return;
    loading.value = true;
    error.value = null;
    try {
      chunk.value = await chunkApi.get(id);
    } catch (e) {
      error.value = (e as Error).message;
    } finally {
      loading.value = false;
    }
  },
  { immediate: true }
);

async function copyContent() {
  if (!chunk.value) return;
  try {
    await navigator.clipboard.writeText(chunk.value.content);
    message.success('已复制切片内容');
  } catch {
    message.error('复制失败');
  }
}
</script>

<template>
  <NDrawer
    :show="ui.chunkPreviewOpen"
    :width="560"
    placement="right"
    @update:show="(v: boolean) => !v && ui.closeChunkPreview()"
  >
    <NDrawerContent
      :native-scrollbar="false"
      closable
      title="切片详情"
    >
      <NSpin :show="loading">
        <div v-if="error" class="err">{{ error }}</div>
        <NEmpty v-else-if="!chunk && !loading" description="无切片数据" />
        <div v-else-if="chunk" class="chunk-view">
          <div class="chunk-header">
            <div class="chunk-title">
              <NIcon :size="18" color="var(--brand-primary)"><DocumentTextOutline /></NIcon>
              <span>{{ chunk.title || `切片 #${chunk.chunkIndex}` }}</span>
            </div>
            <div class="chunk-tags">
              <NTag v-if="chunk.chunkType" size="small" round :bordered="false" type="info">
                {{ chunk.chunkType }}
              </NTag>
              <NTag size="small" round :bordered="false">
                #{{ chunk.chunkIndex }}
              </NTag>
            </div>
          </div>

          <div v-if="chunk.documentName" class="doc-line">
            来自文档：<strong>{{ chunk.documentName }}</strong>
          </div>

          <div class="chunk-content">
            <MarkdownRenderer :content="chunk.content" />
          </div>

          <NDescriptions
            v-if="chunk.metadata && Object.keys(chunk.metadata).length"
            label-placement="left"
            :column="1"
            size="small"
            class="meta-block"
            title="元数据"
          >
            <NDescriptionsItem
              v-for="(v, k) in chunk.metadata"
              :key="String(k)"
              :label="String(k)"
            >
              <code class="meta-val">{{ JSON.stringify(v) }}</code>
            </NDescriptionsItem>
          </NDescriptions>
        </div>
      </NSpin>

      <template #footer>
        <div class="footer-actions">
          <NButton secondary size="small" @click="copyContent" :disabled="!chunk">
            <template #icon><NIcon :component="CopyOutline" /></template>
            复制内容
          </NButton>
        </div>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.chunk-view { display: flex; flex-direction: column; gap: 14px; }
.chunk-header {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; flex-wrap: wrap;
}
.chunk-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 600; color: var(--text-primary);
}
.chunk-tags { display: flex; gap: 6px; }
.doc-line {
  font-size: 12px;
  color: var(--text-secondary);
  padding: 8px 12px;
  background: var(--bg-subtle);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--brand-primary);
}
.chunk-content {
  padding: 14px 16px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  max-height: 50vh;
  overflow-y: auto;
}
.meta-block { margin-top: 4px; }
.meta-val {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--bg-subtle);
  padding: 1px 6px;
  border-radius: 4px;
  word-break: break-all;
}
.err {
  padding: 12px;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.08);
  border-radius: var(--radius-md);
}
.footer-actions { display: flex; justify-content: flex-end; gap: 8px; }
</style>
