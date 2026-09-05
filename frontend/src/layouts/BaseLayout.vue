<script setup lang="ts">
import PublicHeader from '@/components/PublicHeader.vue'
import PublicFooter from '@/components/PublicFooter.vue'
import ReadingProgress from '@/components/ui/ReadingProgress.vue'
</script>

<template>
  <div class="base-layout">
    <ReadingProgress />
    <PublicHeader />
    <main class="layout-shell base-layout__main">
      <slot />
    </main>
    <PublicFooter />
  </div>
</template>

<style scoped>
.base-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.base-layout__main {
  flex: 1;
  width: 100%;
  padding-block: var(--space-10);
}
</style>
