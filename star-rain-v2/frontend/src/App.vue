<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import PublicShell from './shared/shells/PublicShell.vue'
import AdminShell from './shared/shells/AdminShell.vue'

/*
 * 外壳按**路由用途**切换，不再按域名。
 *
 *   meta.authPage  —— 整屏认证页，不套外壳（登录 / 注册 / 找回 / 重置 / 接受邀请）
 *   meta.console   —— 控制台页面，套 AdminShell（有侧栏，控制台本来就该有侧栏）
 *   其它           —— PublicShell：公开树是匿名只读前台，账号树是同一套内容 + 右上角账号区
 *
 * 前台**永远**没有侧栏：唯一带侧栏的 AdminShell 只由 meta.console 的页面进入。
 * 账号模式由 PublicShell 自己从 URL 前缀解析（见 shared/viewMode.js），
 * 这里不再把模式当参数往下传：多一层转发只会多一个可能与 URL 不一致的真源。
 */
const route = useRoute()

const bare = computed(() => Boolean(route.meta.authPage))
const shell = computed(() => {
  if (bare.value) return null
  return route.meta.console ? AdminShell : PublicShell
})
</script>

<template>
  <component :is="shell" v-if="shell"><RouterView /></component>
  <RouterView v-else />
</template>
