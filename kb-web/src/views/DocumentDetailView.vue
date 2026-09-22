<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton, NIcon, NSpin, NTag, NEmpty, NDataTable, useMessage,
  NAlert
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import {
  ArrowBackOutline, LibraryOutline, TrashOutline, RefreshOutline,
  DocumentTextOutline
} from '@vicons/ionicons5';
import { documentApi, chunkApi } from '@/api';
import type { Document, Chunk } from '@/api/types';
import StatusBadge from '@/components/common/StatusBadge.vue';
import FileTypeIcon from '@/components/common/FileTypeIcon.vue';
import FileSize from '@/components/common/FileSize.vue';
import KbTag from '@/components/common/KbTag.vue';
import AssignKbDrawer from '@/components/document/AssignKbDrawer.vue';
import { useUiStore } from '@/stores/ui';

const route = useRoute();
const router = useRouter();
const msg = useMessage();
const ui = useUiStore();

const docId = computed(() => String(route.params.id));
const doc = ref<Document | null>(null);
const chunks = ref<Chunk[]>([]);
const loadingDoc = ref(false);
const loadingChunks = ref(false);
const assignOpen = ref(false);

async function loadDoc() {
  loadingDoc.value = true;
  try {
    doc.value = await documentApi.get(docId.value);
  } catch (e) {
    msg.error((e as Error).message);
  } finally { loadingDoc.value = false; }
}

async function loadChunks() {
  if (!doc.value || doc.value.status !== 'COMPLETED') return;
  loadingChunks.value = true;
  try {
    chunks.value = await chunkApi.listByDocument(docId.value);
  } catch (e) {
    msg.error((e as Error).message);
  } finally { loadingChunks.value = false; }
}

onMounted(async () => {
  await loadDoc();
  await loadChunks();
});

async function onDelete() {
  if (!doc.value) return;
  try {
    await documentApi.remove(doc.value.id);
    msg.success('已删除');
    router.push('/documents');
  } catch (e) { msg.error((e as Error).message); }
}

const chunkColumns = computed<DataTableColumns<Chunk>>(() => [
  {
    title: '#',
    key: 'chunkIndex',
    width: 60,
    render: (row) => h('span', { class: 'num' }, row.chunkIndex)
  },
  {
    title: '类型',
    key: 'chunkType',
    width: 110,
    render: (row) => row.chunkType
      ? h(NTag, { size: 'small', round: true, bordered: false, type: 'info' },
          { default: () => row.chunkType })
      : '—'
  },
  {
    title: '标题',
    key: 'title',
    width: 220,
    ellipsis: { tooltip: true },
    render: (row) => row.title || h('span', { class: 'muted' }, '无标题')
  },
  {
    title: '内容预览',
    key: 'content',
    ellipsis: { tooltip: false },
    render: (row) =>
      h('div', { class: 'content-preview' },
        (row.content || '').slice(0, 160) + ((row.content || '').length > 160 ? '…' : ''))
  },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    align: 'right',
    render: (row) =>
      h(NButton, {
        size: 'tiny', secondary: true,
        onClick: (e: MouseEvent) => {
          e.stopPropagation();
          ui.openChunkPreview(row.id);
        }
      }, { default: () => '查看' })
  }
]);
</script>

<template>
  <div class="doc-detail">
    <div class="page-header">
      <div class="header-left">
        <NButton quaternary circle size="small" @click="router.back()">
          <template #icon><NIcon :component="ArrowBackOutline" /></template>
        </NButton>
        <NSpin :show="loadingDoc && !doc" size="small">
          <div v-if="doc" class="title-block">
            <div class="title-row">
              <FileTypeIcon :file-type="doc.fileType" :file-name="doc.fileName" />
              <h2 class="page-heading">{{ doc.fileName }}</h2>
              <StatusBadge :status="doc.status" />
            </div>
            <div class="meta-row">
              <span>{{ doc.chunkCount }} 个切片</span>
              <span class="dot">·</span>
              <FileSize :bytes="doc.fileSize" />
              <span class="dot">·</span>
              <span>
                上传于
                {{ doc.createdAt ? new Date(doc.createdAt).toLocaleString('zh-CN', { hour12: false }) : '—' }}
              </span>
            </div>
          </div>
        </NSpin>
      </div>
      <div class="header-right" v-if="doc">
        <NButton secondary size="small" @click="assignOpen = true">
          <template #icon><NIcon :component="LibraryOutline" /></template>
          设置归属
        </NButton>
        <NButton quaternary circle size="small" @click="loadDoc(); loadChunks()">
          <template #icon><NIcon :component="RefreshOutline" /></template>
        </NButton>
        <NButton type="error" secondary size="small" @click="onDelete">
          <template #icon><NIcon :component="TrashOutline" /></template>
          删除
        </NButton>
      </div>
    </div>

    <div class="content app-scroll">
      <template v-if="doc">
        <!-- 错误信息 -->
        <NAlert
          v-if="doc.status === 'FAILED' && doc.errorMessage"
          type="error"
          title="处理失败"
          :bordered="false"
          class="alert"
        >
          {{ doc.errorMessage }}
        </NAlert>

        <!-- 归属知识库 -->
        <div class="section">
          <div class="section-header">
            <h3 class="section-title">归属知识库</h3>
          </div>
          <div v-if="doc.knowledgeBases?.length" class="kb-list">
            <KbTag
              v-for="kb in doc.knowledgeBases"
              :key="kb.id!"
              :kb="kb"
              size="medium"
              clickable
              @click="router.push(`/knowledge-bases/${kb.id}`)"
            />
          </div>
          <div v-else class="muted">未归属任何知识库</div>
        </div>

        <!-- 切片列表 -->
        <div class="section">
          <div class="section-header">
            <h3 class="section-title">
              切片列表
              <span v-if="chunks.length" class="count">（{{ chunks.length }}）</span>
            </h3>
          </div>

          <NEmpty
            v-if="!loadingChunks && !chunks.length"
            :description="doc.status === 'COMPLETED' ? '暂无切片' : '文档尚未处理完成，切片会在处理完成后出现'"
            class="empty"
          >
            <template #icon><NIcon :component="DocumentTextOutline" /></template>
          </NEmpty>

          <NDataTable
            v-else
            :columns="chunkColumns"
            :data="chunks"
            :loading="loadingChunks"
            :row-key="(r: Chunk) => r.id"
            :bordered="false"
            size="small"
            striped
            class="chunk-table"
            @row-click="(r: Chunk) => ui.openChunkPreview(r.id)"
          />
        </div>
      </template>
    </div>

    <AssignKbDrawer
      v-model:show="assignOpen"
      :document="doc"
      @saved="loadDoc"
    />
  </div>
</template>

<style scoped>
.doc-detail {
  display: flex; flex-direction: column;
  height: 100%; overflow: hidden;
}
.page-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: 16px; padding: 16px 24px;
  border-bottom: 1px solid var(--border-default);
  background: var(--bg-elevated);
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 12px; min-width: 0; flex: 1; }
.title-block { min-width: 0; }
.title-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.page-heading {
  margin: 0; font-size: 16px; font-weight: 600;
  color: var(--text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  max-width: 480px;
}
.meta-row {
  margin-top: 4px;
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--text-tertiary);
}
.meta-row .dot { opacity: 0.4; }
.header-right { display: flex; gap: 8px; flex-shrink: 0; }

.content {
  flex: 1; min-height: 0;
  padding: 16px 24px 24px;
  display: flex; flex-direction: column; gap: 20px;
}
.alert { border-radius: var(--radius-md); }

.section { display: flex; flex-direction: column; gap: 10px; }
.section-header { display: flex; align-items: center; justify-content: space-between; }
.section-title {
  margin: 0; font-size: 14px; font-weight: 600;
  color: var(--text-primary);
}
.section-title .count {
  color: var(--text-tertiary);
  font-weight: 400;
  font-size: 12px;
  margin-left: 2px;
}
.kb-list { display: flex; gap: 6px; flex-wrap: wrap; }
.muted { color: var(--text-tertiary); font-size: 12px; }
.empty { padding: 32px 0; }

.chunk-table {
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-default);
  overflow: hidden;
}
:deep(.num) {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--text-tertiary);
}
:deep(.content-preview) {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
:deep(.n-data-table-tr) { cursor: pointer; }
</style>
