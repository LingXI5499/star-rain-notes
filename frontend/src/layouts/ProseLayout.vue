<script setup lang="ts">
import { RouterView } from 'vue-router'
import BaseLayout from './BaseLayout.vue'
</script>

<template>
  <BaseLayout>
    <div class="layout-prose">
      <RouterView />
    </div>
  </BaseLayout>
</template>
