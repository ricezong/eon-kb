import { darkTheme, type GlobalThemeOverrides } from 'naive-ui';

/**
 * 主题系统：亮暗双主题 + 品牌渐变主色。
 *
 * 设计原则：
 *  - Naive UI 通过 theme + themeOverrides 控制组件颜色；
 *  - 全局背景/边框/文本走 CSS 变量（:root[data-theme='dark']），
 *    方便自定义组件（不依赖 Naive）也能吃到主题；
 *  - 主色使用品牌渐变（indigo → cyan），关键交互点用 --brand-primary 纯色。
 */

export type ThemeMode = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'eon-kb:theme';

export const naiveDarkTheme = darkTheme;

/** 品牌色板（亮暗共用） */
export const BRAND = {
  primary: '#6366f1',        // indigo-500
  primaryHover: '#818cf8',   // indigo-400
  primaryPressed: '#4f46e5', // indigo-600
  accent: '#06b6d4',         // cyan-500
  success: '#10b981',
  warning: '#f59e0b',
  error: '#ef4444',
  info: '#3b82f6'
} as const;

/** Naive UI 主题覆盖（亮色） */
export const lightThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: BRAND.primary,
    primaryColorHover: BRAND.primaryHover,
    primaryColorPressed: BRAND.primaryPressed,
    primaryColorSuppl: BRAND.primary,
    infoColor: BRAND.info,
    successColor: BRAND.success,
    warningColor: BRAND.warning,
    errorColor: BRAND.error,
    borderRadius: '10px',
    borderRadiusSmall: '6px',
    fontFamily:
      '"Inter", "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
    fontFamilyMono:
      '"JetBrains Mono", "Fira Code", "SF Mono", Menlo, Consolas, monospace',
    fontSize: '14px'
  },
  Card: {
    borderRadius: '14px'
  },
  Button: {
    borderRadiusMedium: '10px',
    fontWeightStrong: '500'
  },
  DataTable: {
    thColor: 'rgba(255,255,255,0)',
    thFontWeight: '600',
    borderColor: 'rgba(15, 23, 42, 0.06)'
  },
  Menu: {
    itemBorderRadius: '10px',
    itemIconSize: '18px'
  },
  Tag: {
    borderRadius: '999px'
  }
};

/** Naive UI 主题覆盖（暗色） */
export const darkThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: BRAND.primaryHover,
    primaryColorHover: '#a5b4fc',
    primaryColorPressed: BRAND.primary,
    primaryColorSuppl: BRAND.primaryHover,
    infoColor: '#60a5fa',
    successColor: '#34d399',
    warningColor: '#fbbf24',
    errorColor: '#f87171',
    borderRadius: '10px',
    borderRadiusSmall: '6px',
    bodyColor: '#0b0d12',
    cardColor: '#12151c',
    modalColor: '#141821',
    popoverColor: '#141821',
    tableColor: '#12151c',
    inputColor: '#1a1f2b',
    actionColor: '#161a23',
    tabColor: '#12151c',
    hoverColor: 'rgba(99, 102, 241, 0.12)',
    fontFamily:
      '"Inter", "PingFang SC", "Microsoft YaHei", "Hiragino Sans GB", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
    fontFamilyMono:
      '"JetBrains Mono", "Fira Code", "SF Mono", Menlo, Consolas, monospace'
  },
  Card: {
    color: '#12151c',
    borderColor: 'rgba(255,255,255,0.06)',
    borderRadius: '14px'
  },
  DataTable: {
    thColor: 'rgba(255,255,255,0)',
    tdColor: '#12151c',
    tdColorHover: 'rgba(99, 102, 241, 0.06)',
    borderColor: 'rgba(255,255,255,0.06)',
    thFontWeight: '600'
  },
  Menu: {
    itemBorderRadius: '10px',
    itemColorActive: 'rgba(99, 102, 241, 0.16)',
    itemColorActiveHover: 'rgba(99, 102, 241, 0.22)',
    itemTextColorActive: '#c7d2fe',
    itemIconColorActive: '#c7d2fe'
  },
  Tag: {
    borderRadius: '999px'
  }
};

/** 知识库色板预设（用于创建/编辑时的颜色选择器） */
export const KB_COLOR_PRESETS = [
  '#6366f1', // indigo
  '#06b6d4', // cyan
  '#10b981', // emerald
  '#f59e0b', // amber
  '#ef4444', // red
  '#ec4899', // pink
  '#8b5cf6', // violet
  '#3b82f6', // blue
  '#14b8a6', // teal
  '#f97316', // orange
  '#84cc16', // lime
  '#64748b'  // slate
] as const;
