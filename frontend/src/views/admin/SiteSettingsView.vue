<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import { fetchSiteSettings, updateSiteSettings, type AdminSiteSettings } from '@/api/site'
import type { ProblemDetail } from '@/api/http'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'

const loading = ref(true)
const saving = ref(false)

const form = reactive({
  siteName: '',
  tagline: '',
  siteUrl: '',
  footerText: '',
  githubUrl: '',
  timezone: 'Asia/Shanghai',
  logoMediaId: null as number | null,
  faviconMediaId: null as number | null,
})

// Unsaved-changes guard + Ctrl/Cmd+S (TASK-011).
const { capture } = useUnsavedGuard(() => form, save)

onMounted(async () => {
  try {
    const settings = await fetchSiteSettings()
    Object.assign(form, {
      siteName: settings.siteName,
      tagline: settings.tagline ?? '',
      siteUrl: settings.siteUrl ?? '',
      footerText: settings.footerText ?? '',
      githubUrl: settings.githubUrl ?? '',
      timezone: settings.timezone,
      logoMediaId: settings.logoMediaId,
      faviconMediaId: settings.faviconMediaId,
    })
  } catch {
    ElMessage.error('加载站点设置失败。')
  } finally {
    loading.value = false
    capture()
  }
})

async function save() {
  if (!form.siteName.trim() || !form.timezone.trim()) {
    ElMessage.warning('站点名称与时区不能为空。')
    return
  }
  saving.value = true
  try {
    await updateSiteSettings({ ...form })
    capture()
    ElMessage.success('站点设置已保存。')
  } catch (error) {
    const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
    ElMessage.error(problem?.detail ?? '保存失败。')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="settings">
    <h1 class="settings__title">站点设置</h1>

    <el-form v-loading="loading" label-position="top" class="settings__form" @submit.prevent="save">
      <el-form-item label="站点名称">
        <el-input v-model="form.siteName" maxlength="100" />
      </el-form-item>
      <el-form-item label="标语（Tagline）">
        <el-input v-model="form.tagline" maxlength="255" />
      </el-form-item>
      <el-form-item label="站点 URL">
        <el-input v-model="form.siteUrl" maxlength="255" />
      </el-form-item>
      <el-form-item label="页脚文本">
        <el-input v-model="form.footerText" maxlength="500" />
      </el-form-item>
      <el-form-item label="GitHub URL">
        <el-input v-model="form.githubUrl" maxlength="500" />
      </el-form-item>
      <el-form-item label="时区（IANA）">
        <el-input v-model="form.timezone" maxlength="64" placeholder="Asia/Shanghai" />
      </el-form-item>
      <el-form-item label="Logo 媒体 ID">
        <el-input-number v-model="form.logoMediaId" :min="1" :controls="false" placeholder="媒体库将在后续任务提供" />
      </el-form-item>
      <el-form-item label="Favicon 媒体 ID">
        <el-input-number v-model="form.faviconMediaId" :min="1" :controls="false" placeholder="媒体库将在后续任务提供" />
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </el-form>
  </section>
</template>

<style scoped>
.settings__title {
  font-size: 28px;
  line-height: 36px;
  margin-bottom: var(--space-8);
}

.settings__form {
  max-width: 560px;
}
</style>
