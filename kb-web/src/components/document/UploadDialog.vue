<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import {
  NModal, NUpload, NButton, NIcon, useMessage, NSelect, NAlert,
  NProgress, NRadioGroup, NRadioButton
} from 'naive-ui';
import type { UploadCustomRequestOptions } from 'naive-ui';
import { CloudUploadOutline } from '@vicons/ionicons5';
import { documentApi } from '@/api';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';
import type { UploadResponse, ParseMode } from '@/api/types';

const props = defineProps<{
  show: boolean;
  /** 打开时预选的知识库（从知识库详情页进入时用） */
  presetKbIds?: string[];
}>();
const emit = defineEmits<{
  (e: 'update:show', v: boolean): void;
  (e: 'uploaded', resp: UploadResponse): void;
}>();

const msg = useMessage();
const kbStore = useKnowledgeBaseStore();

const selectedKbs = ref<string[]>([]);
const uploading = ref(false);
const progress = ref(0);
const currentFile = ref<string>('');

/** 解析方式：默认 AUTO，云端能力从后端 parse-options 拉取 */
const parseMode = ref<ParseMode>('AUTO');
const cloudEnabled = ref(false);

watch(() => props.show, async (v) => {
  if (!v) return;
  selectedKbs.value = [...(props.presetKbIds || [])];
  progress.value = 0;
  currentFile.value = '';
  parseMode.value = 'AUTO';
  try {
    const opts = await documentApi.parseOptions();
    cloudEnabled.value = opts.cloudEnabled;
    parseMode.value = opts.defaultMode;
  } catch {
    // 拉取失败不阻断上传，退回最保守的本地默认
    cloudEnabled.value = false;
    parseMode.value = 'AUTO';
  }
});

const kbOptions = computed(() =>
  kbStore.items.map((kb) => ({
    label: kb.name,
    value: kb.id!
  }))
);

/** 三种解析方式的说明，随选中项动态提示 */
const parseModeHint = computed(() => {
  if (parseMode.value === 'LOCAL') {
    return '强制本地解析（Tika / POI / Jsoup），不调用云端，适合敏感或离线场景。';
  }
  if (parseMode.value === 'CLOUD') {
    return cloudEnabled.value
      ? '强制 LlamaParse 云端解析，对复杂版式还原更好；云端异常时自动回退本地。'
      : '未配置 LlamaParse 密钥，选择此项将自动回退本地解析。';
  }
  return cloudEnabled.value
    ? '按文档类型自动选择：PDF / Word / PPT 走云端增强，其余本地解析。'
    : '云端未启用，当前等同本地解析。';
});

const ACCEPT = '.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.md,.markdown,.html,.htm';

async function customUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const raw = file.file as File | null;
  if (!raw) { onError(); return; }

  uploading.value = true;
  currentFile.value = raw.name;
  progress.value = 0;

  try {
    const resp = await documentApi.upload(raw, selectedKbs.value, parseMode.value, (p) => {
      progress.value = p;
    });
    msg.success(`「${resp.fileName}」已上传，正在处理`);
    emit('uploaded', resp);
    onFinish();
    // 单文件模式：成功后关闭
    setTimeout(() => emit('update:show', false), 400);
  } catch (e) {
    msg.error((e as Error).message);
    onError();
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <NModal
    :show="show"
    preset="card"
    title="上传文档"
    :style="{ width: '520px' }"
    :mask-closable="!uploading"
    :closable="!uploading"
    @update:show="(v: boolean) => emit('update:show', v)"
  >
    <div class="upload-body">
      <div class="form-row">
        <label class="label">解析方式</label>
        <NRadioGroup v-model:value="parseMode" :disabled="uploading" size="small">
          <NRadioButton value="AUTO">自动</NRadioButton>
          <NRadioButton value="LOCAL">本地 (Tika)</NRadioButton>
          <NRadioButton value="CLOUD" :disabled="!cloudEnabled">云端 (LlamaParse)</NRadioButton>
        </NRadioGroup>
        <span class="hint">{{ parseModeHint }}</span>
      </div>

      <div class="form-row">
        <label class="label">归属知识库（可选，可多选）</label>
        <NSelect
          v-model:value="selectedKbs"
          :options="kbOptions"
          multiple
          clearable
          placeholder="留空表示不归属任何知识库"
          :max-tag-count="3"
        />
      </div>

      <NUpload
        :custom-request="customUpload"
        :accept="ACCEPT"
        :max="1"
        :show-file-list="false"
        :disabled="uploading"
        directory-dnd
      >
        <div class="dropzone" :class="{ uploading }">
          <div class="dz-icon">
            <NIcon :size="28" :component="CloudUploadOutline" />
          </div>
          <div class="dz-title">
            {{ uploading ? `正在上传 ${currentFile}` : '拖拽文件到此处，或点击选择' }}
          </div>
          <div class="dz-hint">
            支持 PDF / Word / PPT / Excel / TXT / Markdown / HTML，最大 100MB
          </div>
          <NProgress
            v-if="uploading"
            type="line"
            :percentage="progress"
            :show-indicator="false"
            :height="4"
            class="dz-progress"
            status="success"
          />
        </div>
      </NUpload>

      <NAlert type="info" :bordered="false" size="small" class="tip">
        上传后系统会自动解析、切片、向量化并写入索引，状态可在文档管理页查看。
        处理耗时取决于文档大小与解析器（PDF/DOCX/PPTX 走 LlamaParse 云端增强，其余本地解析）。
      </NAlert>
    </div>

    <template #footer>
      <div class="footer">
        <NButton @click="emit('update:show', false)" :disabled="uploading">关闭</NButton>
      </div>
    </template>
  </NModal>
</template>

<style scoped>
.upload-body { display: flex; flex-direction: column; gap: 14px; }
.form-row { display: flex; flex-direction: column; gap: 6px; }
.label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
}
.hint {
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.5;
}

.dropzone {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px;
  padding: 32px 20px;
  border: 2px dashed var(--border-strong);
  border-radius: var(--radius-lg);
  background: var(--bg-subtle);
  cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast),
    transform var(--transition-fast);
  text-align: center;
}
.dropzone:hover {
  border-color: var(--brand-primary);
  background: var(--brand-gradient-soft);
}
.dropzone.uploading {
  cursor: progress;
  border-color: var(--brand-primary);
}
.dz-icon {
  width: 52px; height: 52px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #fff;
  margin-bottom: 4px;
  box-shadow: var(--shadow-brand);
}
.dz-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}
.dz-hint {
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.5;
}
.dz-progress { width: 80%; margin-top: 10px; }
.tip { font-size: 12px; }
.footer { display: flex; justify-content: flex-end; gap: 8px; }
</style>
