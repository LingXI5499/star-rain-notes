<script setup>
import { ref } from 'vue'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'

const dialog = ref(null)
defineExpose({ ask: (...args) => dialog.value.ask(...args) })
</script>

<template>
  <AppConfirmDialog ref="dialog" />
</template>
