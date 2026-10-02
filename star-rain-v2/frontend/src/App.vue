<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { currentEntry } from './shared/entry'
import PublicShell from './shared/shells/PublicShell.vue'
import UserShell from './shared/shells/UserShell.vue'
import AdminShell from './shared/shells/AdminShell.vue'

/*
 * 外壳按入口切换，而不是按「是不是后台页」切换。
 *
 * 三个入口各有一套外壳：公开站匿名只读、用户站给 USER/ADMIN、管理站给 SUPER_ADMIN。
 * 当前入口在模块加载时由 shared/entry.js 从 hostname 解析出来，页面生命周期内不变。
 *
 * 例外是认证页：它们自带整屏布局，任何入口都不套外壳。
 * 入口不匹配的路由不会渲染到这里——导航守卫已经把它们重定向回本入口首页了。
 */
const route = useRoute()
const SHELLS = { public: PublicShell, user: UserShell, admin: AdminShell }
const bare = computed(() => Boolean(route.meta.authPage))
const shell = computed(() => (bare.value ? null : SHELLS[currentEntry] || PublicShell))
</script>

<template>
  <component :is="shell" v-if="shell"><RouterView /></component>
  <RouterView v-else />
</template>
