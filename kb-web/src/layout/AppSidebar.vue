<script setup lang="ts">
import { h, computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { NMenu, NIcon, NTooltip, NButton, NScrollbar } from 'naive-ui';
import type { MenuOption } from 'naive-ui';
import {
  ChatbubbleEllipsesOutline, LibraryOutline, DocumentsOutline,
  ChevronBackOutline, ChevronForwardOutline, SparklesOutline
} from '@vicons/ionicons5';
import { useUiStore } from '@/stores/ui';
import { useChatStore } from '@/stores/chat';

const route = useRoute();
const ui = useUiStore();
const chat = useChatStore();

const icon = (comp: any) => h(NIcon, null, { default: () => h(comp) });

const menuOptions = computed<MenuOption[]>(() => [
  {
    label: () => h(RouterLink, { to: '/chat' }, { default: () => '智能问答' }),
    key: 'chat',
    icon: () => icon(ChatbubbleEllipsesOutline)
  },
  {
    label: () => h(RouterLink, { to: '/knowledge-bases' }, { default: () => '知识库' }),
    key: 'knowledge-bases',
    icon: () => icon(LibraryOutline)
  },
  {
    label: () => h(RouterLink, { to: '/documents' }, { default: () => '文档管理' }),
    key: 'documents',
    icon: () => icon(DocumentsOutline)
  }
]);

const activeKey = computed(() => {
  const p = route.path;
  if (p.startsWith('/chat')) return 'chat';
  if (p.startsWith('/knowledge-bases')) return 'knowledge-bases';
  if (p.startsWith('/documents')) return 'documents';
  return '';
});

/** 历史会话（Chat 页展开显示） */
const historySessions = computed(() => chat.sessions.slice(0, 12));
</script>

<template>
  <aside
    class="sidebar"
    :class="{ collapsed: ui.sidebarCollapsed }"
  >
    <!-- 品牌区 -->
    <div class="brand">
      <div class="brand-logo">
        <NIcon :size="20" color="#fff"><SparklesOutline /></NIcon>
      </div>
      <Transition name="fade-slide">
        <div v-if="!ui.sidebarCollapsed" class="brand-text">
          <div class="brand-name">Eon 知识库</div>
          <div class="brand-sub">Knowledge Base</div>
        </div>
      </Transition>
    </div>

    <!-- 主导航 -->
    <NMenu
      :collapsed="ui.sidebarCollapsed"
      :collapsed-width="68"
      :collapsed-icon-size="20"
      :indent="18"
      :options="menuOptions"
      :value="activeKey"
      class="main-menu"
    />

    <!-- 会话历史（仅在未折叠 + Chat 页显示） -->
    <Transition name="fade-slide">
      <div
        v-if="!ui.sidebarCollapsed && route.path.startsWith('/chat')"
        class="history-section"
      >
        <div class="section-header">
          <span class="section-title">最近对话</span>
          <NButton
            text
            size="tiny"
            class="new-chat-btn"
            @click="chat.createSession(); $router.push('/chat')"
          >
            + 新建
          </NButton>
        </div>
        <NScrollbar style="max-height: 260px">
          <ul v-if="historySessions.length" class="history-list">
            <li
              v-for="s in historySessions"
              :key="s.id"
              class="history-item"
              :class="{ active: chat.activeSessionId === s.id }"
              @click="chat.selectSession(s.id); $router.push(`/chat/${s.id}`)"
            >
              <NIcon :size="14" class="history-icon"><ChatbubbleEllipsesOutline /></NIcon>
              <span class="history-title">{{ s.title }}</span>
            </li>
          </ul>
          <div v-else class="history-empty">暂无对话</div>
        </NScrollbar>
      </div>
    </Transition>

    <!-- 底部：折叠按钮 -->
    <div class="sidebar-footer">
      <NTooltip :disabled="!ui.sidebarCollapsed" placement="right">
        <template #trigger>
          <NButton
            quaternary circle size="small"
            class="collapse-btn"
            @click="ui.toggleSidebar()"
          >
            <template #icon>
              <NIcon :size="16">
                <ChevronForwardOutline v-if="ui.sidebarCollapsed" />
                <ChevronBackOutline v-else />
              </NIcon>
            </template>
          </NButton>
        </template>
        展开侧栏
      </NTooltip>
      <Transition name="fade-slide">
        <span v-if="!ui.sidebarCollapsed" class="footer-hint">
          收起侧栏
        </span>
      </Transition>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  height: 100%;
  background: var(--bg-elevated);
  border-right: 1px solid var(--border-default);
  transition: width var(--transition-base);
  overflow: hidden;
  flex-shrink: 0;
}
.sidebar.collapsed { width: var(--sidebar-collapsed-width); }

/* 品牌区 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 14px;
  height: var(--header-height);
  flex-shrink: 0;
  border-bottom: 1px solid var(--border-default);
}
.brand-logo {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  background: var(--brand-gradient);
  border-radius: 9px;
  box-shadow: var(--shadow-brand);
  flex-shrink: 0;
}
.brand-text { overflow: hidden; white-space: nowrap; }
.brand-name {
  font-size: 14px; font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}
.brand-sub {
  font-size: 10px;
  color: var(--text-tertiary);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

/* 主菜单 */
.main-menu { padding: 8px 6px; flex-shrink: 0; }

/* 历史会话 */
.history-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 8px 12px 12px;
  border-top: 1px solid var(--border-default);
  margin-top: 4px;
}
.section-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 6px 8px;
}
.section-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  letter-spacing: 0.6px;
  text-transform: uppercase;
}
.new-chat-btn {
  font-size: 11px;
  color: var(--brand-primary) !important;
}
.history-list {
  list-style: none;
  margin: 0; padding: 0;
}
.history-item {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 10px;
  margin-bottom: 2px;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: background var(--transition-fast), color var(--transition-fast);
  overflow: hidden;
}
.history-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}
.history-item.active {
  background: var(--brand-gradient-soft);
  color: var(--brand-primary);
  font-weight: 500;
}
.history-icon { flex-shrink: 0; opacity: 0.7; }
.history-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-empty {
  padding: 16px 10px;
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 底部折叠按钮 */
.sidebar-footer {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid var(--border-default);
  flex-shrink: 0;
}
.sidebar.collapsed .sidebar-footer { justify-content: center; padding: 10px 0; }
.collapse-btn { color: var(--text-tertiary) !important; }
.collapse-btn:hover { color: var(--brand-primary) !important; }
.footer-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
}
</style>
