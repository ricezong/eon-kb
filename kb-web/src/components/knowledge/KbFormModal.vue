<script setup lang="ts">
import { ref, computed, watchEffect } from 'vue';
import {
  NModal, NForm, NFormItem, NInput, NButton, NIcon, useMessage, NTooltip
} from 'naive-ui';
import { ColorPaletteOutline } from '@vicons/ionicons5';
import { knowledgeBaseApi } from '@/api';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';
import { KB_COLOR_PRESETS } from '@/theme';
import type { KnowledgeBase } from '@/api/types';

const props = defineProps<{
  show: boolean;
  /** 传入即编辑模式，否则创建 */
  editing?: KnowledgeBase | null;
}>();
const emit = defineEmits<{
  (e: 'update:show', v: boolean): void;
  (e: 'saved', kb: KnowledgeBase): void;
}>();

const msg = useMessage();
const kbStore = useKnowledgeBaseStore();
const submitting = ref(false);

const form = ref({
  name: '',
  description: '',
  color: KB_COLOR_PRESETS[0] as string
});

const isEdit = computed(() => !!props.editing?.id);
const title = computed(() => (isEdit.value ? '编辑知识库' : '新建知识库'));

/** 打开时重置表单 */
watchEffect(() => {
  if (props.show) {
    if (props.editing) {
      form.value = {
        name: props.editing.name || '',
        description: props.editing.description || '',
        color: props.editing.color || KB_COLOR_PRESETS[0]
      };
    } else {
      form.value = {
        name: '',
        description: '',
        // 从预设里挑一个未被大量使用的颜色
        color: KB_COLOR_PRESETS[kbStore.items.length % KB_COLOR_PRESETS.length]
      };
    }
  }
});

async function submit() {
  const name = form.value.name.trim();
  if (!name) {
    msg.warning('请输入知识库名称');
    return;
  }
  submitting.value = true;
  try {
    let saved: KnowledgeBase;
    if (isEdit.value && props.editing?.id) {
      saved = await knowledgeBaseApi.update(props.editing.id, {
        name,
        description: form.value.description,
        color: form.value.color
      });
      msg.success('已保存');
    } else {
      saved = await knowledgeBaseApi.create({
        name,
        description: form.value.description,
        color: form.value.color
      });
      msg.success('已创建');
    }
    kbStore.upsert(saved);
    emit('saved', saved);
    emit('update:show', false);
  } catch (e) {
    msg.error((e as Error).message);
  } finally {
    submitting.value = false;
  }
}

function close() {
  emit('update:show', false);
}
</script>

<template>
  <NModal
    :show="show"
    preset="card"
    :title="title"
    :style="{ width: '480px' }"
    :mask-closable="!submitting"
    :closable="!submitting"
    @update:show="(v: boolean) => emit('update:show', v)"
  >
    <NForm label-placement="top" :disabled="submitting">
      <NFormItem label="名称" required>
        <NInput
          v-model:value="form.name"
          placeholder="例如：产品手册、法务合同、内部规范"
          maxlength="200"
          show-count
          clearable
        />
      </NFormItem>

      <NFormItem label="描述">
        <NInput
          v-model:value="form.description"
          type="textarea"
          placeholder="简单说明这个知识库收录的内容"
          maxlength="500"
          show-count
          :autosize="{ minRows: 2, maxRows: 5 }"
        />
      </NFormItem>

      <NFormItem>
        <template #label>
          <span class="color-label">
            <NIcon :component="ColorPaletteOutline" :size="14" />
            标识色
          </span>
        </template>
        <div class="color-picker">
          <NTooltip v-for="c in KB_COLOR_PRESETS" :key="c" trigger="hover">
            <template #trigger>
              <button
                type="button"
                class="color-dot"
                :class="{ active: form.color === c }"
                :style="{ background: c }"
                @click="form.color = c"
              />
            </template>
            {{ c }}
          </NTooltip>

          <!-- 自定义颜色 -->
          <label class="custom-color" title="自定义颜色">
            <input
              type="color"
              :value="form.color"
              @input="(e) => form.color = (e.target as HTMLInputElement).value"
            />
            <span class="custom-icon">+</span>
          </label>

          <span class="color-hex">{{ form.color }}</span>
        </div>
      </NFormItem>
    </NForm>

    <template #footer>
      <div class="footer">
        <NButton @click="close" :disabled="submitting">取消</NButton>
        <NButton
          type="primary"
          :loading="submitting"
          @click="submit"
        >
          {{ isEdit ? '保存' : '创建' }}
        </NButton>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
.color-label {
  display: inline-flex; align-items: center; gap: 6px;
}
.color-picker {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
}
.color-dot {
  width: 26px; height: 26px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: transform var(--transition-fast), border-color var(--transition-fast),
    box-shadow var(--transition-fast);
  padding: 0;
  outline: none;
}
.color-dot:hover { transform: scale(1.12); }
.color-dot.active {
  border-color: var(--text-primary);
  box-shadow: 0 0 0 2px var(--bg-elevated), 0 0 0 4px currentColor;
  transform: scale(1.08);
}
.custom-color {
  position: relative;
  width: 26px; height: 26px;
  border-radius: 50%;
  border: 2px dashed var(--border-strong);
  cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  overflow: hidden;
  transition: border-color var(--transition-fast);
}
.custom-color:hover { border-color: var(--brand-primary); }
.custom-color input[type="color"] {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  padding: 0; border: none;
  cursor: pointer;
  opacity: 0;
}
.custom-icon {
  font-size: 14px;
  color: var(--text-tertiary);
  pointer-events: none;
  line-height: 1;
}
.color-hex {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: var(--text-tertiary);
  margin-left: 4px;
}
.footer { display: flex; justify-content: flex-end; gap: 8px; }
</style>
