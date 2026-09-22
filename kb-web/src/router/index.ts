import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

/**
 * 路由约定：
 *  - 所有页面都在 AppLayout 内，共享侧栏 + 顶栏
 *  - 使用 lazy import 拆分 chunk，首屏只加载当前视图
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    children: [
      {
        path: 'chat',
        name: 'chat',
        component: () => import('@/views/ChatView.vue'),
        meta: { title: '智能问答', icon: 'chat' }
      },
      {
        path: 'chat/:sessionId?',
        name: 'chat-session',
        component: () => import('@/views/ChatView.vue'),
        meta: { title: '智能问答', hidden: true }
      },
      {
        path: 'knowledge-bases',
        name: 'knowledge-bases',
        component: () => import('@/views/KnowledgeBaseListView.vue'),
        meta: { title: '知识库', icon: 'library' }
      },
      {
        path: 'knowledge-bases/:id',
        name: 'knowledge-base-detail',
        component: () => import('@/views/KnowledgeBaseDetailView.vue'),
        meta: { title: '知识库详情', hidden: true }
      },
      {
        path: 'documents',
        name: 'documents',
        component: () => import('@/views/DocumentsView.vue'),
        meta: { title: '文档管理', icon: 'documents' }
      },
      {
        path: 'documents/:id',
        name: 'document-detail',
        component: () => import('@/views/DocumentDetailView.vue'),
        meta: { title: '文档详情', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue')
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
});

router.afterEach((to) => {
  const title = (to.meta?.title as string | undefined) || 'Eon 知识库';
  document.title = `${title} · Eon 知识库`;
});

export default router;
