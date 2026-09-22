<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue';
import { useRouter } from 'vue-router';
import {
  NButton, NIcon, NSpin, NDropdown, useDialog, useMessage, NInput
} from 'naive-ui';
import {
  AddOutline, EllipsisVerticalOutline, CreateOutline, TrashOutline,
  LibraryOutline, SearchOutline, DocumentsOutline
} from '@vicons/ionicons5';
import { knowledgeBaseApi } from '@/api';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';
import type { KnowledgeBase } from '@/api/types';
import KbFormModal from '@/components/knowledge/KbFormModal.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const router = useRouter();
const kbStore = useKnowledgeBaseStore();
const dialog = useDialog();
const msg = useMessage();

const loading = ref(false);
const search = ref('');
const formOpen = ref(false);
const editing = ref<KnowledgeBase | null>(null);

onMounted(async () => {
  loading.value = true;
  try { await kbStore.refresh(); } catch (e) {
    msg.error((e as Error).message);
  } finally { loading.value = false; }
});

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase();
  if (!q) return kbStore.items;
  return kbStore.items.filter(
    (kb) =>
      kb.name.toLowerCase().includes(q) ||
      (kb.description || '').toLowerCase().includes(q)
  );
});

function openCreate() { editing.value = null; formOpen.value = true; }
function openEdit(kb: KnowledgeBase) { editing.value = kb; formOpen.value = true; }

function onDelete(kb: KnowledgeBase) {
  dialog.warning({
    title: '删除知识库',
    content: `确定要删除「${kb.name}」吗？该操作会解除 ${kb.documentCount} 篇文档的归属关系，但不会删除文档本身。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await knowledgeBaseApi.remove(kb.id!);
        kbStore.remove(kb.id!);
        msg.success('已删除');
      } catch (e) { msg.error((e as Error).message); }
    }
  });
}

function openDetail(kb: KnowledgeBase) {
  router.push(`/knowledge-bases/${kb.id}`);
}

function menuOptions(_kb: KnowledgeBase) {
  return [
    { label: '查看详情', key: 'view', icon: () => h(NIcon, null, { default: () => h(DocumentsOutline) }) },
    { label: '编辑', key: 'edit', icon: () => h(NIcon, null, { default: () => h(CreateOutline) }) },
    { type: 'divider', key: 'd1' },
    { label: '删除', key: 'delete', icon: () => h(NIcon, null, { default: () => h(TrashOutline) }), props: { style: 'color: #ef4444' } }
  ];
}
function onMenuSelect(key: string, kb: KnowledgeBase) {
  if (key === 'view') openDetail(kb);
  else if (key === 'edit') openEdit(kb);
  else if (key === 'delete') onDelete(kb);
}
</script>

<template>
  <div class="kb-list-view">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-heading">知识库</h2>
        <p class="page-desc">
          共 {{ kbStore.items.length }} 个知识库，累计 {{ kbStore.totalDocuments }} 篇文档
        </p>
      </div>
      <div class="header-right">
        <NInput
          v-model:value="search"
          placeholder="搜索知识库…"
          clearable
          class="search-input"
        >
          <template #prefix>
            <NIcon :component="SearchOutline" />
          </template>
        </NInput>
        <NButton type="primary" @click="openCreate">
          <template #icon><NIcon :component="AddOutline" /></template>
          新建
        </NButton>
      </div>
    </div>

    <NSpin :show="loading">
      <div class="scroll-area app-scroll">
        <EmptyState
          v-if="!loading && !filtered.length"
          :icon="LibraryOutline"
          :title="search ? '没有匹配的知识库' : '还没有知识库'"
          :description="search ? '试试其他关键词' : '创建第一个知识库来组织你的文档，让检索更聚焦'"
        >
          <NButton v-if="!search" type="primary" @click="openCreate">
            <template #icon><NIcon :component="AddOutline" /></template>
            新建知识库
          </NButton>
        </EmptyState>

        <div v-else class="kb-grid">
          <div
            v-for="kb in filtered"
            :key="kb.id!"
            class="kb-card card-elevated"
            @click="openDetail(kb)"
          >
            <div class="kb-color-bar" :style="{ background: kb.color }" />
            <div class="kb-body">
              <div class="kb-top">
                <div class="kb-avatar" :style="{ background: kb.color }">
                  {{ kb.name.slice(0, 1).toUpperCase() }}
                </div>
                <div class="kb-meta">
                  <div class="kb-name">{{ kb.name }}</div>
                  <div class="kb-desc">
                    {{ kb.description || '暂无描述' }}
                  </div>
                </div>
                <NDropdown
                  trigger="click"
                  :options="menuOptions(kb)"
                  @select="(k: string) => onMenuSelect(k, kb)"
                >
                  <NButton
                    quaternary circle size="small"
                    class="kb-more"
                    @click.stop
                  >
                    <template #icon><NIcon :component="EllipsisVerticalOutline" /></template>
                  </NButton>
                </NDropdown>
              </div>

              <div class="kb-footer">
                <span class="stat">
                  <NIcon :size="14" :component="DocumentsOutline" />
                  {{ kb.documentCount }} 篇文档
                </span>
                <span class="stat-id">{{ kb.id?.slice(0, 8) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </NSpin>

    <KbFormModal v-model:show="formOpen" :editing="editing" />
  </div>
</template>

<style scoped>
.kb-list-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
.page-header {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: 16px;
  padding: 20px 24px 12px;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.page-heading {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}
.page-desc {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
}
.header-right { display: flex; gap: 8px; align-items: center; }
.search-input { width: 220px; }

.scroll-area {
  flex: 1;
  min-height: 0;
  padding: 8px 24px 24px;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

.kb-card {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}
.kb-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
.kb-color-bar {
  height: 3px;
  width: 100%;
  flex-shrink: 0;
}
.kb-body {
  padding: 14px 16px 12px;
  display: flex; flex-direction: column; gap: 12px;
  flex: 1;
}
.kb-top {
  display: flex; align-items: flex-start; gap: 12px;
}
.kb-avatar {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 10px -2px currentColor;
}
.kb-meta {
  flex: 1;
  min-width: 0;
}
.kb-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.kb-desc {
  margin-top: 3px;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}
.kb-more {
  flex-shrink: 0;
  color: var(--text-tertiary) !important;
}
.kb-more:hover { color: var(--brand-primary) !important; }

.kb-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid var(--border-default);
  font-size: 12px;
  color: var(--text-tertiary);
}
.stat { display: inline-flex; align-items: center; gap: 4px; }
.stat-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  opacity: 0.6;
}
</style>
