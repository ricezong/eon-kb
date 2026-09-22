<script setup lang="ts">
import MarkdownIt from 'markdown-it';
import { computed } from 'vue';

const md = new MarkdownIt({
  html: false,          // 不允许原始 HTML，防 XSS
  linkify: true,
  breaks: true,
  typographer: false
});

/**
 * 把答案里的 [1] [2] 这类引用编号转成可点击徽章。
 * 只匹配方括号里纯数字的形式，避免误伤代码/普通括号。
 */
function decorateCitations(html: string): string {
  return html.replace(
    /\[(\d{1,2})\]/g,
    (_, n) =>
      `<span class="citation-ref" data-citation-index="${n}" title="查看引用 [${n}]">${n}</span>`
  );
}

const props = defineProps<{
  content: string;
  /** 是否流式中：给末尾加打字光标 */
  streaming?: boolean;
}>();

const emit = defineEmits<{
  (e: 'citation-click', index: number): void;
}>();

const rendered = computed(() => {
  if (!props.content) return '';
  const html = md.render(props.content);
  return decorateCitations(html);
});

function onClick(ev: MouseEvent) {
  const t = ev.target as HTMLElement;
  if (t.classList.contains('citation-ref')) {
    const idx = parseInt(t.dataset.citationIndex || '0', 10);
    if (idx > 0) emit('citation-click', idx);
  }
}
</script>

<template>
  <div
    class="markdown-body"
    :class="{ 'typing-caret': streaming }"
    @click="onClick"
    v-html="rendered"
  />
</template>
