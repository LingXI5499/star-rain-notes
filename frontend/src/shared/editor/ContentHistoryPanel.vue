<script setup>
import { ref } from 'vue'
import { get, errorMessage } from '../http'
const props = defineProps({ basePath: { type: String, required: true } })
const emit = defineEmits(['restore'])
const entries = ref([]), error = ref(''), opened = ref(false), busy = ref(false)
async function show() {
  opened.value = !opened.value
  if (!opened.value) return
  busy.value = true; error.value = ''
  try { entries.value = await get(`${props.basePath}/revisions`) } catch (e) { error.value = errorMessage(e) }
  finally { busy.value = false }
}
async function restore(entry) {
  busy.value = true; error.value = ''
  try { const revision = await get(`${props.basePath}/revisions/${entry.rowVersion}`); emit('restore', revision.snapshot) }
  catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
</script>
<template><section><button type="button" :disabled="busy" @click="show">{{ opened ? '收起历史版本' : '查看正文与基本信息历史' }}</button>
  <div v-if="opened"><p>载入历史内容后仍需保存；标签、区块及媒体关联沿用当前记录。最多保留最近 100 个编辑前版本。</p>
    <p v-if="error" role="alert">{{ error }}</p><p v-if="!busy && !entries.length">还没有历史版本。</p>
    <p v-for="entry in entries" :key="entry.rowVersion">版本 {{ entry.rowVersion }} · {{ entry.snapshot.title }} · {{ entry.createdAt }} <button type="button" :disabled="busy" @click="restore(entry)">载入此版本</button></p>
  </div></section></template>
