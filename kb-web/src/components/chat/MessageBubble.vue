<script setup lang="ts">
import { computed } from 'vue';
import { NAvatar, NIcon, NButton } from 'naive-ui';
import { PersonOutline, SparklesOutline, StopCircleOutline } from '@vicons/ionicons5';
import type { ChatMessage } from '@/stores/chat';
import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue';
import CitationCard from './CitationCard.vue';

const props = defineProps<{
  message: ChatMessage;
  streaming?: boolean;
}>();

const emit = defineEmits<{ (e: 'stop'): void }>();

const isUser = computed(() => props.message.role === 'user');

function onCitationClick(idx: number) {
  const c = props.message.citations?.find((x) => x.index === idx);
  if (c) {
    // 由 ChunkPreviewDrawer 全局挂载，直接通过 store 触发
    import('@/stores/ui').then(({ useUiStore }) => {
      useUiStore().openChunkPreview(c.id);
    });
  }
}
</script>

<template>
  <div class="msg-row" :class="{ 'is-user': isUser, 'is-assistant': !isUser }">
    <div class="avatar">
      <NAvatar
        :size="30" round
        :style="isUser
          ? { background: 'var(--bg-subtle)', color: 'var(--text-secondary)' }
          : { background: 'var(--brand-gradient)', color: '#fff' }"
      >
        <NIcon :component="isUser ? PersonOutline : SparklesOutline" />
      </NAvatar>
    </div>

    <div class="bubble-wrap">
      <div class="bubble" :class="{ 'is-error': message.status === 'error' }">
        <template v-if="isUser">
          <div class="user-text">{{ message.content }}</div>
        </template>
        <template v-else>
          <div v-if="message.status === 'error' && !message.content" class="err-msg">
            {{ message.errorMessage || '出错了' }}
          </div>
          <MarkdownRenderer
            v-else
            :content="message.content || (streaming ? '' : '_无内容_')"
            :streaming="streaming"
            @citation-click="onCitationClick"
          />
          <div v-if="message.status === 'error' && message.content" class="err-inline">
            ⚠ {{ message.errorMessage }}
          </div>
        </template>
      </div>

      <!-- 引用列表 -->
      <div
        v-if="!isUser && message.citations && message.citations.length"
        class="citations"
      >
        <div class="cite-header">
          <span class="cite-count">引用 {{ message.citations.length }} 条</span>
          <span class="cite-hint">点击查看原文切片</span>
        </div>
        <div class="cite-grid">
          <CitationCard
            v-for="c in message.citations"
            :key="c.id"
            :citation="c"
          />
        </div>
      </div>

      <!-- 流式中显示停止按钮 -->
      <div v-if="streaming && !isUser" class="stream-actions">
        <NButton size="tiny" quaternary @click="emit('stop')">
          <template #icon><NIcon :component="StopCircleOutline" /></template>
          停止生成
        </NButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.msg-row {
  display: flex;
  gap: 12px;
  padding: 4px 0;
}
.msg-row.is-user { flex-direction: row-reverse; }

.avatar { flex-shrink: 0; padding-top: 2px; }

.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: min(760px, 88%);
  min-width: 0;
}
.is-user .bubble-wrap { align-items: flex-end; }

.bubble {
  padding: 10px 14px;
  border-radius: var(--radius-lg);
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  color: var(--text-primary);
  line-height: 1.7;
  word-wrap: break-word;
  overflow-wrap: anywhere;
  box-shadow: var(--shadow-sm);
}
.is-user .bubble {
  background: var(--brand-gradient);
  color: #fff;
  border-color: transparent;
  box-shadow: var(--shadow-brand);
  border-bottom-right-radius: 4px;
}
.is-assistant .bubble {
  border-bottom-left-radius: 4px;
}
.bubble.is-error {
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.04);
}
.user-text {
  white-space: pre-wrap;
  font-size: 14px;
}
.err-msg {
  color: #ef4444;
  font-size: 13px;
}
.err-inline {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-default);
  color: #ef4444;
  font-size: 11px;
}

/* 引用区 */
.citations {
  display: flex; flex-direction: column; gap: 8px;
  padding: 10px 12px;
  background: var(--bg-subtle);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-default);
}
.cite-header {
  display: flex; align-items: center; justify-content: space-between;
}
.cite-count {
  font-size: 11px; font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}
.cite-hint { font-size: 11px; color: var(--text-tertiary); }
.cite-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 8px;
}

.stream-actions { display: flex; justify-content: flex-start; }
</style>
