<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NIcon, NButton, NTooltip, NBreadcrumb, NBreadcrumbItem } from 'naive-ui';
import {
  SunnyOutline, MoonOutline, ReloadOutline
} from '@vicons/ionicons5';
import { useThemeStore } from '@/stores/theme';
import { useKnowledgeBaseStore } from '@/stores/knowledgeBase';

const route = useRoute();
const router = useRouter();
const themeStore = useThemeStore();
const kbStore = useKnowledgeBaseStore();

const pageTitle = computed(() => (route.meta?.title as string) || '');

/** 面包屑：根据路由派生 */
const crumbs = computed(() => {
  const list: Array<{ label: string; to?: string }> = [];
  const p = route.path;
  if (p.startsWith('/knowledge-bases')) {
    list.push({ label: '知识库', to: '/knowledge-bases' });
    if (route.params.id) {
      const kb = kbStore.byId.get(String(route.params.id));
      list.push({ label: kb?.name || '详情' });
    }
  } else if (p.startsWith('/documents')) {
    list.push({ label: '文档管理', to: '/documents' });
    if (route.params.id) list.push({ label: '详情' });
  } else if (p.startsWith('/chat')) {
    list.push({ label: '智能问答' });
  }
  return list;
});

function onRefresh() {
  // 简单粗暴：重新触发当前视图的数据加载
  router.replace({ path: route.fullPath, force: true } as any);
  window.location.reload();
}
</script>

<template>
  <header class="header">
    <div class="header-left">
      <h1 class="page-title">{{ pageTitle }}</h1>
      <NBreadcrumb v-if="crumbs.length > 1" class="crumbs">
        <NBreadcrumbItem v-for="(c, i) in crumbs" :key="i">
          <RouterLink v-if="c.to" :to="c.to">{{ c.label }}</RouterLink>
          <span v-else>{{ c.label }}</span>
        </NBreadcrumbItem>
      </NBreadcrumb>
    </div>

    <div class="header-right">
      <NTooltip trigger="hover">
        <template #trigger>
          <NButton quaternary circle size="small" @click="onRefresh">
            <template #icon><NIcon :size="17"><ReloadOutline /></NIcon></template>
          </NButton>
        </template>
        刷新页面
      </NTooltip>

      <NTooltip trigger="hover">
        <template #trigger>
          <NButton
            quaternary circle size="small"
            class="theme-toggle"
            @click="themeStore.toggle()"
          >
            <template #icon>
              <span class="toggle-icon-wrap">
                <NIcon :size="17" class="icon-sun"><SunnyOutline /></NIcon>
                <NIcon :size="17" class="icon-moon"><MoonOutline /></NIcon>
              </span>
            </template>
          </NButton>
        </template>
        切换到{{ themeStore.isDark ? '亮色' : '暗色' }}主题
      </NTooltip>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 24px 0 24px;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-default);
  flex-shrink: 0;
  gap: 16px;
}
.header-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  gap: 2px;
}
.page-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
  letter-spacing: -0.01em;
}
.crumbs {
  font-size: 11px;
  line-height: 1.4;
}
.crumbs :deep(.n-breadcrumb-item__link) { color: var(--text-tertiary); }
.crumbs :deep(.n-breadcrumb-item:last-child .n-breadcrumb-item__link) {
  color: var(--text-secondary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

/* 主题切换图标叠加 */
.toggle-icon-wrap {
  position: relative;
  display: inline-flex;
  width: 17px; height: 17px;
}
.toggle-icon-wrap .icon-sun,
.toggle-icon-wrap .icon-moon {
  position: absolute;
  inset: 0;
}
</style>
