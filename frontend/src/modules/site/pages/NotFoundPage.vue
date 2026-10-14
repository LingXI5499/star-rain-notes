<script setup>
import { RouterLink } from 'vue-router'
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
<template><section class="container"><h1>页面不存在</h1><p>这个链接可能已经变更，或内容已撤回。</p><RouterLink :to="route.path.startsWith('/useradmin') ? '/useradmin' : '/'">返回首页</RouterLink></section></template>
