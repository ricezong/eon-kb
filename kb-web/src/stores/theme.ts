import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import { THEME_STORAGE_KEY, type ThemeMode } from '@/theme';

/**
 * 主题状态：持久化到 localStorage，同时同步 <html data-theme="..."> 供全局 CSS 变量使用。
 */
export const useThemeStore = defineStore('theme', () => {
  const readInitial = (): ThemeMode => {
    try {
      const saved = localStorage.getItem(THEME_STORAGE_KEY);
      if (saved === 'light' || saved === 'dark') return saved;
    } catch { /* ignore */ }
    return window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  };

  const mode = ref<ThemeMode>(readInitial());

  const isDark = computed(() => mode.value === 'dark');

  const apply = (m: ThemeMode) => {
    document.documentElement.setAttribute('data-theme', m);
    document.documentElement.classList.toggle('dark', m === 'dark');
    // 同步浏览器 UI 颜色（地址栏等）
    const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (meta) meta.content = m === 'dark' ? '#0b0d12' : '#f7f8fb';
  };

  watch(
    mode,
    (m) => {
      apply(m);
      try { localStorage.setItem(THEME_STORAGE_KEY, m); } catch { /* ignore */ }
    },
    { immediate: true }
  );

  // 跟随系统主题（仅当用户没有显式选择时）
  const media = window.matchMedia('(prefers-color-scheme: dark)');
  media.addEventListener('change', (e) => {
    if (!localStorage.getItem(THEME_STORAGE_KEY)) {
      mode.value = e.matches ? 'dark' : 'light';
    }
  });

  const toggle = () => {
    mode.value = mode.value === 'dark' ? 'light' : 'dark';
  };
  const set = (m: ThemeMode) => { mode.value = m; };

  return { mode, isDark, toggle, set };
});
