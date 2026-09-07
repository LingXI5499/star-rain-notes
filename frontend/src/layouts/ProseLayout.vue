<script setup lang="ts">
import { RouterView } from 'vue-router'
import BaseLayout from './BaseLayout.vue'
</script>

<template>
  <BaseLayout>
    <div class="layout-prose">
      <RouterView v-slot="{ Component, route }">
        <Transition name="page-view" mode="out-in">
          <component :is="Component" :key="String(route.name ?? route.path)" />
        </Transition>
      </RouterView>
    </div>
  </BaseLayout>
</template>
