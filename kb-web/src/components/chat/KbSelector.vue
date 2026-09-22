<script setup lang="ts">
import { computed } from 'vue';
import { NButton, NIcon, NPopover, NEmpty } from 'naive-ui';
import { LibraryOutline } from '@vicons/ionicons5';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';
import { useChatStore } from '@/stores/chat';
import KbTag from '@/components/common/KbTag.vue';

const kbStore = useKnowledgeBaseStore();
const chat = useChatStore();

const isGlobal = computed(() => chat.activeKbIds.length === 0);
</script>

<template>
  <div class="kb-selector">
    <NPopover trigger="click" placement="bottom-start" :show-arrow="false" raw>
      <template #trigger>
        <NButton
          secondary size="small"
          class="trigger-btn"
          :type="isGlobal ? 'default' : 'primary'"
        >
          <template #icon>
            <NIcon :component="LibraryOutline" />
          </template>
          <span class="trigger-label">
            {{ isGlobal ? '全部知识库' : `已选 ${chat.activeKbIds.length} 个` }}
          </span>
        </NButton>
      </template>
      <div class="kb-panel card-elevated">
        <div class="panel-header">
          <span class="panel-title">选择检索范围</span>
          <NButton text size="tiny" @click="chat.activeKbIds = []">
            清空（全局）
          </NButton>
        </div>
        <NEmpty
          v-if="!kbStore.items.length"
          description="还没有知识库，去创建一个吧"
          size="small"
          style="padding: 20px"
        />
        <div v-else class="kb-list">
          <label
            v-for="kb in kbStore.items"
            :key="kb.id!"
            class="kb-row"
            :class="{ active: chat.activeKbIds.includes(kb.id!) }"
          >
            <input
              type="checkbox"
              :value="kb.id!"
              v-model="chat.activeKbIds"
              class="kb-check"
            />
            <span class="kb-dot" :style="{ background: kb.color }" />
            <span class="kb-name">{{ kb.name }}</span>
            <span class="kb-count">{{ kb.documentCount }}</span>
          </label>
        </div>
        <div v-if="chat.activeKbIds.length" class="panel-footer">
          <div class="chips">
            <KbTag
              v-for="id in chat.activeKbIds"
              :key="id"
              :kb="kbStore.byId.get(id) || { name: '?', color: '#94a3b8' }"
              size="small"
            />
          </div>
        </div>
      </div>
    </NPopover>

    <div v-if="!isGlobal" class="active-chips">
      <KbTag
        v-for="id in chat.activeKbIds"
        :key="id"
        :kb="kbStore.byId.get(id) || { name: '?', color: '#94a3b8' }"
        size="small"
        clickable
        @click="chat.activeKbIds = chat.activeKbIds.filter(x => x !== id)"
      />
    </div>
  </div>
</template>

<style scoped>
.kb-selector { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.trigger-btn { border-radius: var(--radius-full); }
.trigger-label { font-size: 12px; }

.kb-panel {
  width: 280px;
  padding: 10px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 6px 8px;
  border-bottom: 1px solid var(--border-default);
  margin-bottom: 6px;
}
.panel-title {
  font-size: 12px; font-weight: 600;
  color: var(--text-primary);
}
.kb-list {
  max-height: 260px;
  overflow-y: auto;
  display: flex; flex-direction: column;
  gap: 2px;
}
.kb-row {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  color: var(--text-primary);
  transition: background var(--transition-fast);
}
.kb-row:hover { background: var(--bg-hover); }
.kb-row.active { background: var(--brand-gradient-soft); }
.kb-check {
  accent-color: var(--brand-primary);
  width: 14px; height: 14px;
  cursor: pointer;
  margin: 0;
}
.kb-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.kb-name {
  flex: 1;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.kb-count {
  font-size: 11px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}
.panel-footer {
  border-top: 1px solid var(--border-default);
  margin-top: 8px;
  padding-top: 8px;
}
.chips { display: flex; flex-wrap: wrap; gap: 4px; }

.active-chips {
  display: flex; flex-wrap: wrap; gap: 4px;
}
</style>
