<script setup lang="ts">
import { computed } from 'vue';
import {
  NConfigProvider, NMessageProvider, NDialogProvider,
  NNotificationProvider, NLoadingBarProvider,
  zhCN, dateZhCN, darkTheme
} from 'naive-ui';
import type { GlobalThemeOverrides } from 'naive-ui';
import { useThemeStore } from '@/stores/theme';
import {
  lightThemeOverrides, darkThemeOverrides
} from '@/theme';
import ChunkPreviewDrawer from '@/components/chunk/ChunkPreviewDrawer.vue';

/**
 * 根组件：Naive UI 全局 Provider 链
 *   ConfigProvider (主题) → LoadingBar → Message → Dialog → Notification → RouterView
 * ChunkPreviewDrawer 挂在根级，任何视图都能通过 useUiStore 触发。
 */
const themeStore = useThemeStore();
const naiveTheme = computed(() => (themeStore.isDark ? darkTheme : null));
const themeOverrides = computed<GlobalThemeOverrides>(() =>
  themeStore.isDark ? darkThemeOverrides : lightThemeOverrides
);
</script>

<template>
  <NConfigProvider
    :theme="naiveTheme"
    :theme-overrides="themeOverrides"
    :locale="zhCN"
    :date-locale="dateZhCN"
    inline-theme-disabled
  >
    <NLoadingBarProvider>
      <NDialogProvider>
        <NNotificationProvider>
          <NMessageProvider>
            <RouterView v-slot="{ Component, route }">
              <Transition name="fade-slide" mode="out-in">
                <component :is="Component" :key="route.fullPath" />
              </Transition>
            </RouterView>
            <ChunkPreviewDrawer />
          </NMessageProvider>
        </NNotificationProvider>
      </NDialogProvider>
    </NLoadingBarProvider>
  </NConfigProvider>
</template>

<style>
/* 让 Transition 在根组件生效 */
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.fade-slide-enter-from { opacity: 0; transform: translateY(6px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
