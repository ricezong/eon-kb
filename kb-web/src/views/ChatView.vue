<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { NIcon, NButton, NScrollbar, NTooltip } from 'naive-ui';
import {
  SparklesOutline, TrashBinOutline, BulbOutline,
  BookOutline, ChatbubbleEllipsesOutline
} from '@vicons/ionicons5';
import { useChatStore } from '@/stores/chat';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';
import MessageBubble from '@/components/chat/MessageBubble.vue';
import ChatInput from '@/components/chat/ChatInput.vue';
import KbSelector from '@/components/chat/KbSelector.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const route = useRoute();
const chat = useChatStore();
const kbStore = useKnowledgeBaseStore();
const scrollRef = ref<InstanceType<typeof NScrollbar> | null>(null);

onMounted(async () => {
  await kbStore.load();
  // 从 URL /chat/:sessionId 恢复会话
  const sid = route.params.sessionId as string | undefined;
  if (sid && chat.sessions.some((s) => s.id === sid)) {
    chat.selectSession(sid);
  } else if (!chat.getActive()) {
    chat.createSession();
  }
});

const session = computed(() => chat.getActive());
const messages = computed(() => session.value?.messages ?? []);
const isStreaming = computed(() =>
  messages.value.length > 0 &&
  messages.value[messages.value.length - 1].status === 'streaming'
);

/** 消息更新时自动滚到底部 */
watch(
  () => messages.value.length + (messages.value.at(-1)?.content.length ?? 0),
  () => nextTick(() => scrollRef.value?.scrollTo({ top: 999999, behavior: 'smooth' }))
);

/** 推荐问题（首屏空态引导） */
const suggestions = [
  { icon: BulbOutline, text: '帮我总结一下这份文档的核心要点' },
  { icon: BookOutline, text: '知识库里关于 X 的内容有哪些？' },
  { icon: ChatbubbleEllipsesOutline, text: '对比 A 与 B 的差异，并给出建议' }
];

function onSubmit(q: string) {
  chat.send(q);
}
function onStop() {
  chat.stop();
}
function clearMessages() {
  chat.clearActiveMessages();
}
function useSuggestion(text: string) {
  chat.send(text);
}
</script>

<template>
  <div class="chat-view">
    <!-- 顶部工具条 -->
    <div class="toolbar">
      <KbSelector />
      <div class="toolbar-right">
        <NTooltip v-if="messages.length" trigger="hover">
          <template #trigger>
            <NButton
              quaternary circle size="small"
              @click="clearMessages"
              :disabled="isStreaming"
            >
              <template #icon><NIcon :component="TrashBinOutline" /></template>
            </NButton>
          </template>
          清空当前对话
        </NTooltip>
      </div>
    </div>

    <!-- 消息流 -->
    <NScrollbar ref="scrollRef" class="messages-scroll">
      <div class="messages-inner">
        <EmptyState
          v-if="!messages.length"
          :icon="SparklesOutline"
          title="开始你的第一次提问"
          description="Eon 会基于你的知识库进行混合检索（向量 + 全文 + 模糊），再由 qwen3-rerank 精排后交给 DeepSeek 生成带引用的答案。"
        >
          <div class="suggestions">
            <button
              v-for="(s, i) in suggestions"
              :key="i"
              class="suggestion"
              @click="useSuggestion(s.text)"
            >
              <NIcon :size="16" :component="s.icon" />
              <span>{{ s.text }}</span>
            </button>
          </div>
        </EmptyState>

        <MessageBubble
          v-for="m in messages"
          :key="m.id"
          :message="m"
          :streaming="m.status === 'streaming'"
          @stop="onStop"
        />
      </div>
    </NScrollbar>

    <!-- 输入区 -->
    <ChatInput
      :loading="chat.sending"
      :disabled="false"
      @submit="onSubmit"
      @stop="onStop"
    />
  </div>
</template>

<style scoped>
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--bg-app);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-bottom: 1px solid var(--border-default);
  background: var(--bg-elevated);
  flex-shrink: 0;
  gap: 12px;
}
.toolbar-right { display: flex; gap: 4px; }

.messages-scroll {
  flex: 1;
  min-height: 0;
}
.messages-inner {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 20px 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 10px;
  margin-top: 12px;
  width: 100%;
  max-width: 720px;
}
.suggestion {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition: transform var(--transition-fast), border-color var(--transition-fast),
    color var(--transition-fast), box-shadow var(--transition-fast);
}
.suggestion:hover {
  transform: translateY(-2px);
  border-color: var(--brand-primary);
  color: var(--brand-primary);
  box-shadow: var(--shadow-md);
}
</style>
