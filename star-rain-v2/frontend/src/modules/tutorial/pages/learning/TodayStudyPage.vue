<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { accountPath } from '../../../../shared/viewMode'
const router = useRouter()
onMounted(() => router.replace(accountPath('/learning')))
</script>
<template><p>正在前往学习中心…</p></template>
