<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import { fetchAdminEnglish, updateEnglish } from '@/api/english'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

const loading = ref(true)
const saving = ref(false)
const currentStage = ref('')

const form = reactive({
  title: '',
  subtitle: '',
  introduction: '',
  roadmapMarkdown: '',
})

// Unsaved-changes guard + Ctrl/Cmd+S (TASK-011).
const { capture } = useUnsavedGuard(() => form, save)

onMounted(async () => {
  try {
    const data = await fetchAdminEnglish()
    currentStage.value = data.currentStage
    form.title = data.title
    form.subtitle = data.subtitle ?? ''
    form.introduction = data.introduction ?? ''
    form.roadmapMarkdown = data.roadmapMarkdown ?? ''
  } catch {
    ElMessage.error('加载失败。')
  } finally {
    loading.value = false
    capture()
  }
})

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写标题。')
    return
  }
  saving.value = true
  try {
    await updateEnglish({
      title: form.title,
      subtitle: form.subtitle || null,
      introduction: form.introduction || null,
      roadmapMarkdown: form.roadmapMarkdown || null,
    })
    capture()
    ElMessage.success('已保存。')
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="english-admin">
    <h1 class="english-admin__title">英语管理</h1>

    <el-form v-loading="loading" label-position="top" class="english-admin__form" @submit.prevent="save">
      <el-form-item label="当前阶段（V1 固定为 FOUNDATION）">
        <el-input :model-value="currentStage" disabled />
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="form.title" maxlength="200" />
      </el-form-item>
      <el-form-item label="副标题">
        <el-input v-model="form.subtitle" maxlength="500" />
      </el-form-item>
      <el-form-item label="介绍（Introduction）">
        <el-input v-model="form.introduction" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item label="路线图（Markdown）">
        <el-input v-model="form.roadmapMarkdown" type="textarea" :rows="10" />
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </el-form>
  </section>
</template>

<style scoped>
.english-admin__title {
  font-size: 28px;
  line-height: 36px;
  margin-bottom: var(--space-8);
}

.english-admin__form {
  max-width: 640px;
}
</style>
