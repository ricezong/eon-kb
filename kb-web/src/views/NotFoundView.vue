<script setup lang="ts">
import { NButton, NResult } from 'naive-ui';
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<template>
  <div class="notfound">
    <NResult
      status="404"
      title="页面不存在"
      description="请检查 URL 是否正确，或返回首页继续使用"
    >
      <template #footer>
        <div class="actions">
          <NButton @click="router.back()">返回上一页</NButton>
          <NButton type="primary" @click="router.push('/chat')">
            回到智能问答
          </NButton>
        </div>
      </template>
    </NResult>
  </div>
</template>

<style scoped>
.notfound {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-app);
}
.actions { display: flex; gap: 10px; justify-content: center; }
</style>
