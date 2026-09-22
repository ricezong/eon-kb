import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { knowledgeBaseApi } from '@/api';
import type { KnowledgeBase } from '@/api/types';

/**
 * 知识库缓存：多个视图（Chat 侧栏、KB 页、文档归属编辑）共用同一份列表。
 * 提供 refresh() 强制拉取，load() 首次/缓存失效时拉取。
 */
export const useKnowledgeBaseStore = defineStore('knowledgeBase', () => {
  const items = ref<KnowledgeBase[]>([]);
  const loading = ref(false);
  const lastFetchedAt = ref<number | null>(null);
  const error = ref<string | null>(null);

  const byId = computed(() => {
    const map = new Map<string, KnowledgeBase>();
    items.value.forEach((kb) => { if (kb.id) map.set(kb.id, kb); });
    return map;
  });

  const totalDocuments = computed(() =>
    items.value.reduce((s, kb) => s + (kb.documentCount || 0), 0)
  );

  async function refresh(): Promise<KnowledgeBase[]> {
    loading.value = true;
    error.value = null;
    try {
      const data = await knowledgeBaseApi.list();
      items.value = data;
      lastFetchedAt.value = Date.now();
      return data;
    } catch (e) {
      error.value = (e as Error).message;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function load(force = false): Promise<KnowledgeBase[]> {
    // 5 分钟内命中缓存
    if (!force && lastFetchedAt.value && Date.now() - lastFetchedAt.value < 5 * 60_000) {
      return items.value;
    }
    return refresh();
  }

  /** 局部更新（避免创建/编辑后全量刷新） */
  function upsert(kb: KnowledgeBase) {
    if (!kb.id) return;
    const idx = items.value.findIndex((x) => x.id === kb.id);
    if (idx >= 0) items.value[idx] = kb;
    else items.value.push(kb);
  }

  function remove(id: string) {
    items.value = items.value.filter((x) => x.id !== id);
  }

  return {
    items, loading, error, byId, totalDocuments,
    load, refresh, upsert, remove
  };
});
