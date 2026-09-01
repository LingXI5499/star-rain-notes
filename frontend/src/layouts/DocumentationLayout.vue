<script setup lang="ts">
import { RouterView } from 'vue-router'
import BaseLayout from './BaseLayout.vue'
</script>

<template>
  <!-- Documentation shell: pages inside compose their own
       Sidebar | Content | TOC columns with layout tokens. -->
  <BaseLayout>
    <RouterView />
  </BaseLayout>
</template>
