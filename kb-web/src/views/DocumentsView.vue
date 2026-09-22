<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue';
import { useRouter } from 'vue-router';
import {
  NButton, NIcon, NInput, NDataTable, NSelect, useMessage, useDialog,
  NTag, NDropdown
} from 'naive-ui';
import type { DataTableColumns, DataTableRowKey } from 'naive-ui';
import {
  CloudUploadOutline, SearchOutline, RefreshOutline, TrashOutline,
  EllipsisVerticalOutline, LibraryOutline, EyeOutline
} from '@vicons/ionicons5';
import { documentApi } from '@/api';
import type { Document, DocumentStatus } from '@/api/types';
import StatusBadge from '@/components/common/StatusBadge.vue';
import FileTypeIcon from '@/components/common/FileTypeIcon.vue';
import FileSize from '@/components/common/FileSize.vue';
import KbTag from '@/components/common/KbTag.vue';
import UploadDialog from '@/components/document/UploadDialog.vue';
import AssignKbDrawer from '@/components/document/AssignKbDrawer.vue';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';

const router = useRouter();
const msg = useMessage();
const dialog = useDialog();
const kbStore = useKnowledgeBaseStore();

const docs = ref<Document[]>([]);
const loading = ref(false);
const search = ref('');
const statusFilter = ref<DocumentStatus | null>(null);
const kbFilter = ref<string | null>(null);
const checkedRowKeys = ref<DataTableRowKey[]>([]);

const uploadOpen = ref(false);
const assignDoc = ref<Document | null>(null);
const assignOpen = ref(false);

async function loadDocs() {
  loading.value = true;
  try {
    const [d] = await Promise.all([documentApi.list(), kbStore.load()]);
    docs.value = d;
  } catch (e) {
    msg.error((e as Error).message);
  } finally {
    loading.value = false;
  }
}

onMounted(loadDocs);

/** 状态选项 */
const statusOptions: Array<{ label: string; value: DocumentStatus | null }> = [
  { label: '全部状态', value: null },
  { label: '已完成', value: 'COMPLETED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '失败', value: 'FAILED' },
  { label: '排队中', value: 'PENDING' }
];

const kbOptions = computed<Array<{ label: string; value: string | null }>>(() => [
  { label: '全部知识库', value: null },
  ...kbStore.items.map((kb) => ({ label: kb.name, value: kb.id! }))
]);

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase();
  return docs.value.filter((d) => {
    if (q && !d.fileName.toLowerCase().includes(q)) return false;
    if (statusFilter.value && d.status !== statusFilter.value) return false;
    if (kbFilter.value && !(d.knowledgeBases || []).some((k) => k.id === kbFilter.value)) return false;
    return true;
  });
});

function onDelete(doc: Document) {
  dialog.warning({
    title: '删除文档',
    content: `确定要删除「${doc.fileName}」吗？会级联删除全部切片与向量索引。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await documentApi.remove(doc.id);
        docs.value = docs.value.filter((x) => x.id !== doc.id);
        msg.success('已删除');
      } catch (e) { msg.error((e as Error).message); }
    }
  });
}

function onBulkDelete() {
  if (!checkedRowKeys.value.length) return;
  dialog.warning({
    title: '批量删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 篇文档吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      const ids = new Set(checkedRowKeys.value.map(String));
      try {
        await Promise.all([...ids].map((id) => documentApi.remove(id)));
        docs.value = docs.value.filter((d) => !ids.has(d.id));
        checkedRowKeys.value = [];
        msg.success('已批量删除');
      } catch (e) { msg.error((e as Error).message); }
    }
  });
}

function openAssign(doc: Document) {
  assignDoc.value = doc;
  assignOpen.value = true;
}
function onAssignSaved() {
  // 归属变了，重刷
  loadDocs();
}

const columns = computed<DataTableColumns<Document>>(() => [
  { type: 'selection', fixed: 'left' },
  {
    title: '文件名',
    key: 'fileName',
    ellipsis: { tooltip: true },
    minWidth: 260,
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
  {
    title: '切片',
    key: 'chunkCount',
    width: 70,
    align: 'right',
    render: (row) => h('span', { class: 'num' }, row.chunkCount ?? 0)
  },
  {
    title: '大小',
    key: 'fileSize',
    width: 90,
    render: (row) => h(FileSize, { bytes: row.fileSize })
  },
  {
    title: '归属知识库',
    key: 'knowledgeBases',
    minWidth: 200,
    render: (row) => {
      const list = row.knowledgeBases || [];
      if (!list.length) {
        return h('span', { class: 'muted' }, '未归属');
      }
      return h('div', { class: 'kb-cell' },
        list.slice(0, 3).map((kb) =>
          h(KbTag, {
            kb, size: 'small', clickable: true,
            onClick: () => router.push(`/knowledge-bases/${kb.id}`)
          })
        ).concat(
          list.length > 3
            ? [h(NTag, { size: 'small', round: true, bordered: false }, { default: () => `+${list.length - 3}` })]
            : []
        )
      );
    }
  },
  {
    title: '上传时间',
    key: 'createdAt',
    width: 160,
    render: (row) =>
      h('span', { class: 'time' },
        row.createdAt
          ? new Date(row.createdAt).toLocaleString('zh-CN', { hour12: false })
          : '—')
  },
  {
    title: '操作',
    key: 'actions',
    width: 60,
    align: 'right',
    fixed: 'right',
    render: (row) =>
      h(NDropdown, {
        trigger: 'click',
        options: [
          { label: '查看详情', key: 'view', icon: () => h(NIcon, null, { default: () => h(EyeOutline) }) },
          { label: '设置归属', key: 'assign', icon: () => h(NIcon, null, { default: () => h(LibraryOutline) }) },
          { type: 'divider', key: 'd1' },
          { label: '删除', key: 'delete', icon: () => h(NIcon, null, { default: () => h(TrashOutline) }), props: { style: 'color:#ef4444' } }
        ],
        onSelect: (key: string) => {
          if (key === 'view') router.push(`/documents/${row.id}`);
          else if (key === 'assign') openAssign(row);
          else if (key === 'delete') onDelete(row);
        }
      }, {
        default: () => h(NButton, {
          quaternary: true, circle: true, size: 'small',
          onClick: (e: MouseEvent) => e.stopPropagation()
        }, { icon: () => h(NIcon, { component: EllipsisVerticalOutline }) })
      })
  }
]);
</script>

<template>
  <div class="docs-view">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-heading">文档管理</h2>
        <p class="page-desc">
          共 {{ docs.length }} 篇文档
          <template v-if="checkedRowKeys.length">
            · 已选 {{ checkedRowKeys.length }} 篇
          </template>
        </p>
      </div>
      <div class="header-right">
        <NInput
          v-model:value="search"
          placeholder="搜索文件名…"
          clearable
          class="search"
        >
          <template #prefix><NIcon :component="SearchOutline" /></template>
        </NInput>
        <NSelect
          v-model:value="statusFilter"
          :options="statusOptions"
          class="filter"
          placeholder="状态"
        />
        <NSelect
          v-model:value="kbFilter"
          :options="kbOptions"
          class="filter"
          placeholder="知识库"
        />
        <NButton quaternary circle size="small" @click="loadDocs">
          <template #icon><NIcon :component="RefreshOutline" /></template>
        </NButton>
        <NButton
          v-if="checkedRowKeys.length"
          type="error" secondary size="small"
          @click="onBulkDelete"
        >
          <template #icon><NIcon :component="TrashOutline" /></template>
          批量删除
        </NButton>
        <NButton type="primary" size="small" @click="uploadOpen = true">
          <template #icon><NIcon :component="CloudUploadOutline" /></template>
          上传文档
        </NButton>
      </div>
    </div>

    <div class="content">
      <NDataTable
        :columns="columns"
        :data="filtered"
        :loading="loading"
        :row-key="(r: Document) => r.id"
        v-model:checked-row-keys="checkedRowKeys"
        :bordered="false"
        :scroll-x="1200"
        size="small"
        striped
        class="doc-table"
        @row-click="(r: Document) => router.push(`/documents/${r.id}`)"
      />
    </div>

    <UploadDialog v-model:show="uploadOpen" @uploaded="loadDocs" />
    <AssignKbDrawer
      v-model:show="assignOpen"
      :document="assignDoc"
      @saved="onAssignSaved"
    />
  </div>
</template>

<style scoped>
.docs-view {
  display: flex; flex-direction: column;
  height: 100%; overflow: hidden;
}
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: 16px; padding: 20px 24px 12px;
  flex-shrink: 0; flex-wrap: wrap;
}
.page-heading {
  margin: 0; font-size: 20px; font-weight: 600;
  color: var(--text-primary); letter-spacing: -0.01em;
}
.page-desc {
  margin: 2px 0 0; font-size: 12px;
  color: var(--text-tertiary);
}
.header-right {
  display: flex; gap: 8px; align-items: center; flex-wrap: wrap;
}
.search { width: 200px; }
.filter { width: 130px; }

.content {
  flex: 1; min-height: 0;
  padding: 4px 24px 24px;
  overflow: auto;
}
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
:deep(.kb-cell) {
  display: flex; gap: 4px; align-items: center; flex-wrap: wrap;
}
:deep(.muted) { color: var(--text-tertiary); font-size: 12px; }
:deep(.num) {
  font-family: 'JetBrains Mono', monospace;
  font-variant-numeric: tabular-nums;
  font-size: 12px;
  color: var(--text-secondary);
}
:deep(.time) {
  font-size: 11px; color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}
:deep(.n-data-table-tr) { cursor: pointer; }
</style>
