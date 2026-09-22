<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ fileType?: string | null; fileName?: string | null }>();

const ext = computed(() => {
  if (props.fileType) return props.fileType.toUpperCase();
  if (props.fileName) {
    const m = props.fileName.match(/\.([a-z0-9]+)$/i);
    if (m) return m[1].toUpperCase();
  }
  return 'FILE';
});

const style = computed(() => {
  const map: Record<string, { label: string; bg: string; fg: string }> = {
    PDF:  { label: 'PDF',  bg: '#fee2e2', fg: '#dc2626' },
    DOC:  { label: 'DOC',  bg: '#dbeafe', fg: '#2563eb' },
    DOCX: { label: 'DOC',  bg: '#dbeafe', fg: '#2563eb' },
    PPT:  { label: 'PPT',  bg: '#ffedd5', fg: '#ea580c' },
    PPTX: { label: 'PPT',  bg: '#ffedd5', fg: '#ea580c' },
    XLS:  { label: 'XLS',  bg: '#d1fae5', fg: '#059669' },
    XLSX: { label: 'XLS',  bg: '#d1fae5', fg: '#059669' },
    TXT:  { label: 'TXT',  bg: '#e2e8f0', fg: '#475569' },
    MD:   { label: 'MD',   bg: '#e0e7ff', fg: '#4f46e5' },
    HTML: { label: 'HTML', bg: '#fef3c7', fg: '#b45309' },
    HTM:  { label: 'HTML', bg: '#fef3c7', fg: '#b45309' }
  };
  const s = map[ext.value] || { label: ext.value.slice(0, 4), bg: '#e2e8f0', fg: '#475569' };
  return { background: s.bg, color: s.fg };
});

const label = computed(() => {
  const map: Record<string, string> = {
    PDF: 'PDF', DOC: 'DOC', DOCX: 'DOC', PPT: 'PPT', PPTX: 'PPT',
    XLS: 'XLS', XLSX: 'XLS', TXT: 'TXT', MD: 'MD', HTML: 'HTML', HTM: 'HTML'
  };
  return map[ext.value] || ext.value.slice(0, 4);
});
</script>

<template>
  <span class="file-icon" :style="style">
    {{ label }}
  </span>
</template>

<style scoped>
.file-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 22px;
  padding: 0 6px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.4px;
  font-family: 'Inter', system-ui, sans-serif;
  flex-shrink: 0;
}
:root[data-theme='dark'] .file-icon {
  filter: brightness(0.75) saturate(1.3);
}
</style>
