<script setup lang="ts">
/** 简单的空状态占位 */
defineProps<{
  title?: string;
  description?: string;
  icon?: any;
}>();
</script>

<template>
  <div class="empty-state">
    <div v-if="icon" class="empty-icon">
      <component :is="icon" />
    </div>
    <div v-if="title" class="empty-title">{{ title }}</div>
    <div v-if="description" class="empty-desc">{{ description }}</div>
    <div class="empty-slot"><slot /></div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
  gap: 8px;
}
.empty-icon {
  width: 56px; height: 56px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-lg);
  background: var(--brand-gradient-soft);
  color: var(--brand-primary);
  font-size: 24px;
  margin-bottom: 4px;
}
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
.empty-desc {
  font-size: 13px;
  color: var(--text-secondary);
  max-width: 380px;
  line-height: 1.6;
}
.empty-slot { margin-top: 8px; }
</style>
