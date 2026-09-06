<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PublicHeader from '@/components/PublicHeader.vue'
import PublicFooter from '@/components/PublicFooter.vue'
import ReadingProgress from '@/components/ui/ReadingProgress.vue'
import MobilePublicNav from '@/components/ui/MobilePublicNav.vue'

const route = useRoute()
const showReadingProgress = computed(() => route.meta.readingProgress === true)
</script>

<template>
  <div class="base-layout">
    <ReadingProgress v-if="showReadingProgress" />
    <PublicHeader />
    <main class="layout-shell base-layout__main">
      <slot />
    </main>
    <PublicFooter />
    <MobilePublicNav />
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

@media (max-width: 768px) {
  .base-layout { padding-bottom: calc(78px + env(safe-area-inset-bottom)); }
}
</style>
