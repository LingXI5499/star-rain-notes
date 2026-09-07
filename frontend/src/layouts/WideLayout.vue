<script setup lang="ts">
import { RouterView } from 'vue-router'
import BaseLayout from './BaseLayout.vue'
</script>

<template>
  <BaseLayout>
    <!-- Key by route name so sibling chapter/lesson params keep the view mounted. -->
    <RouterView v-slot="{ Component, route }">
      <Transition name="page-view" mode="out-in">
        <component :is="Component" :key="String(route.name ?? route.path)" />
      </Transition>
    </RouterView>
  </BaseLayout>
</template>
