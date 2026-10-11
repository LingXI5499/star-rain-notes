<script setup>
/**
 * MediaReferencesPanel.vue：媒体模块的视图组件。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../api/mediaApi、../../../shared/http、../support/display。
 * 组件输入：mediaAssetId；默认值与类型见 defineProps。
 * 主要响应式状态：props、references、loading、error。
 * 阅读入口：load（加载当前操作）。
 */


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
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  mediaAssetId: { type: [String, Number], required: true },
})

/**
 * references；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const references = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')

/**
 * 加载当前操作。
 * 主要协作：listMediaReferences、errorMessage。
 * 更新的响应式状态包括 loading、error、references；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
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

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
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
