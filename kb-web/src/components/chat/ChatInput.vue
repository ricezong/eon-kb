<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { NButton, NIcon, NInput } from 'naive-ui';
import { ArrowUpCircleOutline, StopCircleOutline } from '@vicons/ionicons5';

const props = defineProps<{
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
}>();
const emit = defineEmits<{
  (e: 'submit', text: string): void;
  (e: 'stop'): void;
}>();

const text = ref('');
const inputRef = ref<InstanceType<typeof NInput> | null>(null);

const canSend = computed(() =>
  text.value.trim().length > 0 && !props.loading && !props.disabled
);

function send() {
  if (!canSend.value) return;
  const t = text.value.trim();
  text.value = '';
  emit('submit', t);
  nextTick(() => inputRef.value?.focus());
}

function onKeydown(e: KeyboardEvent) {
  // Enter 发送；Shift+Enter 换行；输入法组合中不触发
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault();
    send();
  }
}

onMounted(() => inputRef.value?.focus());
</script>

<template>
  <div class="input-wrap">
    <div class="input-card">
      <NInput
        ref="inputRef"
        v-model:value="text"
        type="textarea"
        :placeholder="placeholder || '输入问题，Enter 发送，Shift+Enter 换行'"
        :autosize="{ minRows: 1, maxRows: 8 }"
        :disabled="disabled"
        class="input-el"
        @keydown="onKeydown"
      />
      <div class="input-actions">
        <span class="hint">
          基于知识库检索 + DeepSeek 生成，回答仅供参考
        </span>
        <NButton
          v-if="!loading"
          type="primary"
          circle
          :disabled="!canSend"
          class="send-btn"
          @click="send"
        >
          <template #icon><NIcon :size="18" :component="ArrowUpCircleOutline" /></template>
        </NButton>
        <NButton
          v-else
          type="error"
          circle
          class="send-btn"
          @click="emit('stop')"
        >
          <template #icon><NIcon :size="18" :component="StopCircleOutline" /></template>
        </NButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.input-wrap {
  padding: 12px 20px 16px;
  flex-shrink: 0;
  background: linear-gradient(to top,
    var(--bg-app) 60%,
    color-mix(in srgb, var(--bg-app) 0%, transparent));
}
.input-card {
  max-width: 820px;
  margin: 0 auto;
  padding: 8px 10px 8px 14px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}
.input-card:focus-within {
  border-color: var(--brand-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--brand-primary) 14%, transparent),
    var(--shadow-md);
}
.input-el :deep(.n-input__textarea-el) {
  font-size: 14px;
  line-height: 1.6;
  padding: 6px 0;
}
.input-el :deep(.n-input-wrapper) { padding: 0; }
.input-el :deep(.n-input__border),
.input-el :deep(.n-input__state-border) { display: none; }

.input-actions {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 6px;
  margin-top: 4px;
  border-top: 1px solid var(--border-default);
}
.hint {
  font-size: 11px;
  color: var(--text-tertiary);
  padding-left: 2px;
}
.send-btn {
  width: 34px; height: 34px;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}
.send-btn:not(:disabled):hover {
  transform: scale(1.06);
  box-shadow: var(--shadow-brand);
}
</style>
