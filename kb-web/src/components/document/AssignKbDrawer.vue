<script setup lang="ts">
import { ref, watch } from 'vue';
import {
  NDrawer, NDrawerContent, NButton, NIcon, useMessage, NEmpty, NSpin
} from 'naive-ui';
import { CheckmarkOutline } from '@vicons/ionicons5';
import { documentApi } from '@/api';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';
import type { Document } from '@/api/types';

const props = defineProps<{
  show: boolean;
  document: Document | null;
}>();
const emit = defineEmits<{
  (e: 'update:show', v: boolean): void;
  (e: 'saved', kbIds: string[]): void;
}>();

const msg = useMessage();
const kbStore = useKnowledgeBaseStore();

const selected = ref<string[]>([]);
const loading = ref(false);
const saving = ref(false);

watch(() => [props.show, props.document] as const, async ([show, doc]) => {
  if (!show || !doc) return;
  loading.value = true;
  try {
    const current = await documentApi.knowledgeBases(doc.id);
    selected.value = current.map((k) => k.id!).filter(Boolean);
  } catch (e) {
    msg.error((e as Error).message);
  } finally {
    loading.value = false;
  }
}, { immediate: true });

async function save() {
  if (!props.document) return;
  saving.value = true;
  try {
    await documentApi.assignKnowledgeBases(props.document.id, selected.value);
    msg.success('归属已更新');
    emit('saved', [...selected.value]);
    emit('update:show', false);
  } catch (e) {
    msg.error((e as Error).message);
  } finally {
    saving.value = false;
  }
}

function toggle(id: string) {
  if (selected.value.includes(id)) {
    selected.value = selected.value.filter((x) => x !== id);
  } else {
    selected.value.push(id);
  }
}
</script>

<template>
  <NDrawer
    :show="show"
    :width="400"
    placement="right"
    @update:show="(v: boolean) => emit('update:show', v)"
  >
    <NDrawerContent
      closable
      title="设置知识库归属"
      :native-scrollbar="false"
    >
      <template v-if="document">
        <div class="doc-line">
          文档：<strong>{{ document.fileName }}</strong>
        </div>

        <NSpin :show="loading">
          <NEmpty
            v-if="!loading && !kbStore.items.length"
            description="还没有知识库"
            size="small"
            style="padding: 32px 0"
          />
          <ul v-else class="kb-list">
            <li
              v-for="kb in kbStore.items"
              :key="kb.id!"
              class="kb-row"
              :class="{ active: selected.includes(kb.id!) }"
              @click="toggle(kb.id!)"
            >
              <span class="kb-check">
                <NIcon v-if="selected.includes(kb.id!)" :component="CheckmarkOutline" />
              </span>
              <span class="kb-dot" :style="{ background: kb.color }" />
              <span class="kb-name">{{ kb.name }}</span>
              <span class="kb-count">{{ kb.documentCount }}</span>
            </li>
          </ul>
        </NSpin>

        <div class="summary">
          已选 {{ selected.length }} / {{ kbStore.items.length }} 个知识库
        </div>
      </template>

      <template #footer>
        <div class="footer">
          <NButton @click="emit('update:show', false)" :disabled="saving">取消</NButton>
          <NButton type="primary" :loading="saving" @click="save">
            保存
          </NButton>
        </div>
      </template>
    </NDrawerContent>
  </NDrawer>
</template>

<style scoped>
.doc-line {
  padding: 8px 12px;
  background: var(--bg-subtle);
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  border-left: 3px solid var(--brand-primary);
}
.kb-list {
  list-style: none;
  padding: 0; margin: 0;
  display: flex; flex-direction: column;
  gap: 4px;
}
.kb-row {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-default);
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
  font-size: 13px;
}
.kb-row:hover { background: var(--bg-hover); border-color: var(--border-strong); }
.kb-row.active {
  background: var(--brand-gradient-soft);
  border-color: var(--brand-primary);
}
.kb-check {
  width: 18px; height: 18px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 4px;
  border: 1.5px solid var(--border-strong);
  color: transparent;
  font-size: 12px;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}
.kb-row.active .kb-check {
  background: var(--brand-primary);
  border-color: var(--brand-primary);
  color: #fff;
}
.kb-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.kb-name { flex: 1; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; }
.kb-count {
  font-size: 11px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}
.summary {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-default);
  font-size: 12px;
  color: var(--text-tertiary);
  text-align: right;
}
.footer { display: flex; justify-content: flex-end; gap: 8px; }
</style>
