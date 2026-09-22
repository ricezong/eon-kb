import { defineStore } from 'pinia';
import { ref } from 'vue';

/** UI 全局状态：侧栏折叠、抽屉/模态开关等 */
export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref<boolean>(
    localStorage.getItem('eon-kb:sidebar-collapsed') === '1'
  );

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
    localStorage.setItem('eon-kb:sidebar-collapsed', sidebarCollapsed.value ? '1' : '0');
  }

  // 切片预览抽屉：跨视图共享（Chat 引用点击 & 文档切片列表点击都能触发）
  const chunkPreviewId = ref<string | null>(null);
  const chunkPreviewOpen = ref(false);

  function openChunkPreview(id: string) {
    chunkPreviewId.value = id;
    chunkPreviewOpen.value = true;
  }
  function closeChunkPreview() {
    chunkPreviewOpen.value = false;
    // 延迟清 id，让抽屉关闭动画能看到内容
    setTimeout(() => { chunkPreviewId.value = null; }, 240);
  }

  return {
    sidebarCollapsed, toggleSidebar,
    chunkPreviewId, chunkPreviewOpen, openChunkPreview, closeChunkPreview
  };
});
