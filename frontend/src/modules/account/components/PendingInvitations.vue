<script setup>
/**
 * PendingInvitations.vue：账号模块的视图组件。
 * 职责范围：身份、会话、邮箱验证、角色权限和邀请策略。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、vue-router、../stores/authStore、../api/accountApi、../api/http、../support/display。
 * 主要响应式状态：auth、router、items、loading、busy、error、confirm。
 * 阅读入口：load（加载当前操作）；accept（接受当前操作）。
 */


import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { myInvitations, acceptMyInvitation } from '../api/accountApi'
import { clearCsrf, errorMessage } from '../api/http'
import { dateLabel } from '../support/display'
import ConfirmDialog from './ConfirmDialog.vue'

/**
 * 认证；通过 useAuthStore 建立。
 */
const auth = useAuthStore()
/**
 * router；通过 useRouter 建立。
 */
const router = useRouter()
/**
 * 条目列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const items = ref([])
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * busy；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const busy = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * 确认；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirm = ref(null)
/**
 * 加载当前操作。
 * 主要协作：myInvitations、errorMessage。
 * 更新的响应式状态包括 loading、error、items；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function load() {
  loading.value = true; error.value = ''
  try { items.value = await myInvitations() }
  catch (cause) { error.value = errorMessage(cause) }
  finally { loading.value = false }
}
/**
 * 接受当前操作。
 * 主要协作：confirm.value.ask、acceptMyInvitation、clearCsrf、router.replace、errorMessage。
 * 更新的响应式状态包括 busy、error；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @param item item；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function accept(item) {
  if (busy.value || !await confirm.value.ask('接受这条管理员邀请？接受后将获得管理员身份，并需要重新登录。')) return
  busy.value = true; error.value = ''
  try {
    await acceptMyInvitation(item.id)
    auth.currentUser = null
    auth.initialized = true
    clearCsrf()
    await router.replace({ path: '/useradmin/login', query: { invited: '1' } })
  } catch (cause) { error.value = errorMessage(cause) }
  finally { busy.value = false }
}
/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(load)
</script>

<template>
  <section class="surface-card detail-panel">
    <div class="section-heading"><div><p class="eyebrow">PENDING INVITATIONS</p><h2>待处理邀请</h2></div><button class="secondary-button" type="button" :disabled="loading || busy" @click="load">刷新邀请</button></div>
    <p v-if="loading" class="loading" role="status">正在查询邀请…</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="!loading && !error && !items.length" class="muted">当前账户没有待处理的管理员邀请。</p>
    <article v-for="item in items" :key="item.id" class="invitation-card">
      <h3>管理员邀请 #{{ item.id }}</h3><p>超级管理员邀请当前账户成为管理员。你可以在这里接受，无需等待邮件。</p><p class="muted">受邀邮箱：{{ item.targetEmail }} · 有效期至 {{ dateLabel(item.expiresAt) }}</p>
      <button class="primary-button" type="button" :disabled="busy" @click="accept(item)">{{ busy ? '处理中…' : '接受管理员邀请' }}</button>
    </article>
  </section>
  <ConfirmDialog ref="confirm" />
</template>
