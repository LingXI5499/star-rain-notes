<script setup lang="ts">
import { RouterView } from 'vue-router'
import BaseLayout from './BaseLayout.vue'
</script>

<template>
  <!-- Documentation shell: pages inside compose their own
       Sidebar | Content | TOC columns with layout tokens.
       Key by route name so chapter/lesson param changes keep the reader mounted. -->
  <BaseLayout>
    <RouterView v-slot="{ Component, route }">
      <Transition name="page-view" mode="out-in">
        <component :is="Component" :key="String(route.name ?? route.path)" />
      </Transition>
    </RouterView>
  </BaseLayout>
</template>
