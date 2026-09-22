<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton, NIcon, NSpin, NEmpty, useMessage, useDialog, NTag, NDataTable
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import {
  ArrowBackOutline, CloudUploadOutline, CreateOutline, TrashOutline,
  DocumentTextOutline
} from '@vicons/ionicons5';
import { knowledgeBaseApi, documentApi } from '@/api';
import type { Document, KnowledgeBase } from '@/api/types';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';
import StatusBadge from '@/components/common/StatusBadge.vue';
import FileTypeIcon from '@/components/common/FileTypeIcon.vue';
import FileSize from '@/components/common/FileSize.vue';
import KbFormModal from '@/components/knowledge/KbFormModal.vue';
import UploadDialog from '@/components/document/UploadDialog.vue';

const route = useRoute();
const router = useRouter();
const msg = useMessage();
const dialog = useDialog();
const kbStore = useKnowledgeBaseStore();

const kbId = computed(() => String(route.params.id));
const kb = ref<KnowledgeBase | null>(null);
const docs = ref<Document[]>([]);
const loading = ref(false);
const editOpen = ref(false);
const uploadOpen = ref(false);

async function loadAll() {
  loading.value = true;
  try {
    const [detail, list] = await Promise.all([
      knowledgeBaseApi.get(kbId.value),
      knowledgeBaseApi.documents(kbId.value)
    ]);
    kb.value = detail;
    docs.value = list;
    kbStore.upsert(detail);
  } catch (e) {
    msg.error((e as Error).message);
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);

async function onDeleteDoc(doc: Document) {
  dialog.warning({
    title: '删除文档',
    content: `确定要删除「${doc.fileName}」吗？该操作会级联删除其所有切片与向量索引，不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await documentApi.remove(doc.id);
        docs.value = docs.value.filter((d) => d.id !== doc.id);
        msg.success('已删除');
      } catch (e) { msg.error((e as Error).message); }
    }
  });
}

async function onUnassign(doc: Document) {
  try {
    // 全量替换：从当前归属中剔除本知识库
    const rest = (doc.knowledgeBases || [])
      .map((k) => k.id)
      .filter((id): id is string => !!id && id !== kbId.value);
    await documentApi.assignKnowledgeBases(doc.id, rest);
    docs.value = docs.value.filter((d) => d.id !== doc.id);
    msg.success('已移出当前知识库');
  } catch (e) { msg.error((e as Error).message); }
}

const columns = computed<DataTableColumns<Document>>(() => [
  {
    title: '文件名',
    key: 'fileName',
    ellipsis: { tooltip: true },
    render: (row) =>
      h('div', { class: 'file-cell' }, [
        h(FileTypeIcon, { fileType: row.fileType, fileName: row.fileName }),
        h('a', {
          class: 'file-link',
          onClick: (e: MouseEvent) => {
            e.stopPropagation();
            router.push(`/documents/${row.id}`);
          }
        }, row.fileName)
      ])
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) => h(StatusBadge, { status: row.status })
  },
  { title: '切片', key: 'chunkCount', width: 70, align: 'right' },
  {
    title: '大小',
    key: 'fileSize',
    width: 90,
    render: (row) => h(FileSize, { bytes: row.fileSize })
  },
  {
    title: '上传时间',
    key: 'createdAt',
    width: 160,
    render: (row) => row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN', { hour12: false }) : '—'
  },
  {
    title: '操作',
    key: 'actions',
    width: 130,
    align: 'right',
    render: (row) =>
      h('div', { class: 'action-cell' }, [
        h(NButton, {
          size: 'tiny', quaternary: true,
          onClick: (e: MouseEvent) => { e.stopPropagation(); onUnassign(row); }
        }, { default: () => '移出' }),
        h(NButton, {
          size: 'tiny', quaternary: true, type: 'error',
          onClick: (e: MouseEvent) => { e.stopPropagation(); onDeleteDoc(row); }
        }, { icon: () => h(NIcon, { component: TrashOutline }) })
      ])
  }
]);
</script>

<template>
  <div class="kb-detail-view">
    <div class="page-header">
      <div class="header-left">
        <NButton quaternary circle size="small" @click="router.push('/knowledge-bases')">
          <template #icon><NIcon :component="ArrowBackOutline" /></template>
        </NButton>
        <NSpin :show="loading && !kb" size="small">
          <div v-if="kb" class="title-block">
            <div class="title-row">
              <span class="kb-avatar" :style="{ background: kb.color }">
                {{ kb.name.slice(0, 1).toUpperCase() }}
              </span>
              <h2 class="page-heading">{{ kb.name }}</h2>
              <NTag round :bordered="false" size="small">
                {{ kb.documentCount }} 篇
              </NTag>
            </div>
            <p v-if="kb.description" class="page-desc">{{ kb.description }}</p>
          </div>
        </NSpin>
      </div>
      <div class="header-right">
        <NButton secondary size="small" @click="editOpen = true" :disabled="!kb">
          <template #icon><NIcon :component="CreateOutline" /></template>
          编辑
        </NButton>
        <NButton type="primary" size="small" @click="uploadOpen = true" :disabled="!kb">
          <template #icon><NIcon :component="CloudUploadOutline" /></template>
          上传文档
        </NButton>
      </div>
    </div>

    <div class="content app-scroll">
      <NEmpty
        v-if="!loading && !docs.length"
        description="这个知识库还没有文档"
        class="empty-block"
      >
        <template #icon>
          <NIcon :component="DocumentTextOutline" />
        </template>
        <NButton type="primary" size="small" @click="uploadOpen = true">
          上传第一个文档
        </NButton>
      </NEmpty>

      <NDataTable
        v-else
        :columns="columns"
        :data="docs"
        :loading="loading"
        :row-key="(r: Document) => r.id"
        :bordered="false"
        size="small"
        striped
        class="doc-table"
        @row-click="(r: Document) => router.push(`/documents/${r.id}`)"
      />
    </div>

    <KbFormModal v-model:show="editOpen" :editing="kb" @saved="loadAll" />
    <UploadDialog
      v-model:show="uploadOpen"
      :preset-kb-ids="[kbId]"
      @uploaded="loadAll"
    />
  </div>
</template>

<style scoped>
.kb-detail-view {
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
.title-row { display: flex; align-items: center; gap: 10px; }
.kb-avatar {
  width: 28px; height: 28px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; color: #fff;
}
.page-heading {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
.page-desc {
  margin: 4px 0 0 38px;
  font-size: 12px;
  color: var(--text-tertiary);
  overflow: hidden; text-overflow: ellipsis;
}
.header-right { display: flex; gap: 8px; flex-shrink: 0; }

.content { flex: 1; min-height: 0; padding: 16px 24px 24px; }
.empty-block { padding: 60px 0; }
.doc-table {
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-default);
  overflow: hidden;
}
:deep(.file-cell) {
  display: flex; align-items: center; gap: 8px;
  min-width: 0;
}
:deep(.file-link) {
  color: var(--text-primary);
  cursor: pointer;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-weight: 500;
  transition: color var(--transition-fast);
}
:deep(.file-link:hover) { color: var(--brand-primary); }
:deep(.action-cell) { display: inline-flex; gap: 2px; }
:deep(.n-data-table-tr) { cursor: pointer; }
</style>
