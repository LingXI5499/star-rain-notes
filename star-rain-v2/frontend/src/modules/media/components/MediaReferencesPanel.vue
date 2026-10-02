<script setup>
import { onMounted, ref, watch } from 'vue'
import { listMediaReferences } from '../api/mediaApi'
import { errorMessage } from '../../../shared/http'
import { dateLabel } from '../support/display'

/*
 * MED-007 媒体引用情况面板。
 *
 * 回答「这个媒体正在被谁引用」：存在引用时后端会拒绝归档，
 * 所以这里既是信息展示，也是归档前必须看的依据。
 */
const props = defineProps({
  mediaAssetId: { type: [String, Number], required: true },
})

const references = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    references.value = await listMediaReferences(props.mediaAssetId)
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.mediaAssetId, load)
</script>

<template>
  <div>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="loading" class="loading" role="status">正在加载引用…</p>
    <div v-else-if="references.length" class="table-scroll">
      <table>
        <thead>
          <tr><th>来源模块</th><th>对象类型</th><th>对象 ID</th><th>用途</th><th>建立时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in references" :key="`${item.sourceModule}-${item.sourceType}-${item.sourceId}-${item.usageCode}`">
            <td>{{ item.sourceModule }}</td>
            <td>{{ item.sourceType }}</td>
            <td>{{ item.sourceId }}</td>
            <td><small>{{ item.usageCode }}</small></td>
            <td>{{ dateLabel(item.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="muted">尚无业务引用。只有没有任何引用时才允许归档。</p>
  </div>
</template>
