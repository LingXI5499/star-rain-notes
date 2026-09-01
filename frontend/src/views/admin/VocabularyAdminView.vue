<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import {
  addAdminExample,
  fetchAdminWords,
  fetchVocabularyLayers,
  removeAdminExample,
  setAdminMemory,
  updateAdminWord,
  formatMemoryTime,
  type VocabularyExample,
  type VocabularyLayer,
  type VocabularyWord,
} from '@/api/vocabulary'

/**
 * 词汇管理：按主题浏览词条，编辑翻译/音标/词形变化，维护例句（用户自加），
 * 以及记忆次数修正。MVP 仅覆盖词汇模块本身。
 */
const loading = ref(true)
const saving = ref(false)

const filters = reactive({ themeId: undefined as number | undefined, q: '', page: 1, pageSize: 20 })
const layers = ref<VocabularyLayer[]>([])
const items = ref<VocabularyWord[]>([])
const total = ref(0)
const totalPages = ref(0)

const themeOptions = ref<{ id: number; label: string }[]>([])

const editVisible = ref(false)
const editForm = reactive({
  id: 0,
  word: '',
  partOfSpeech: '',
  translation: '',
  phoneticUs: '',
  inflections: '',
  examples: [] as VocabularyExample[],
  memoryCount: 0,
})
const exampleInput = reactive({ sentence: '', translation: '' })

async function load() {
  loading.value = true
  try {
    const result = await fetchAdminWords({
      themeId: filters.themeId,
      q: filters.q || undefined,
      page: filters.page,
      pageSize: filters.pageSize,
    })
    items.value = result.items
    total.value = result.total
    totalPages.value = result.totalPages
  } catch {
    ElMessage.error('加载词汇失败。')
  } finally {
    loading.value = false
  }
}

async function initThemes() {
  try {
    layers.value = await fetchVocabularyLayers()
    themeOptions.value = layers.value.flatMap((layer) =>
      layer.themes.map((theme) => ({ id: theme.id, label: `${layer.layer} › ${theme.name}` })),
    )
  } catch {
    ElMessage.error('加载主题失败。')
  }
}

function search() {
  filters.page = 1
  void load()
}

function openEdit(word: VocabularyWord) {
  editForm.id = word.id
  editForm.word = word.word
  editForm.partOfSpeech = word.partOfSpeech
  editForm.translation = word.translation
  editForm.phoneticUs = word.phoneticUs ?? ''
  editForm.inflections = word.inflections ?? ''
  editForm.examples = word.examples.map((e) => ({ ...e }))
  editForm.memoryCount = word.memoryCount
  exampleInput.sentence = ''
  exampleInput.translation = ''
  editVisible.value = true
}

function showError(error: unknown) {
  const problem = error instanceof AxiosError ? (error.response?.data as ProblemDetail | undefined) : undefined
  ElMessage.error(problem?.detail ?? '操作失败。')
}

async function saveWord() {
  if (!editForm.translation.trim()) {
    ElMessage.warning('请填写中文翻译。')
    return
  }
  saving.value = true
  try {
    await updateAdminWord(editForm.id, {
      translation: editForm.translation,
      phoneticUs: editForm.phoneticUs || null,
      inflections: editForm.inflections || null,
    })
    ElMessage.success('已保存。')
    editVisible.value = false
    await load()
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function addExample() {
  if (!exampleInput.sentence.trim()) {
    ElMessage.warning('请填写例句。')
    return
  }
  try {
    const updated = await addAdminExample(editForm.id, {
      sentence: exampleInput.sentence.trim(),
      translation: exampleInput.translation.trim() || null,
    })
    editForm.examples = updated.examples
    exampleInput.sentence = ''
    exampleInput.translation = ''
  } catch (error) {
    showError(error)
  }
}

async function removeExample(index: number) {
  try {
    const updated = await removeAdminExample(editForm.id, index)
    editForm.examples = updated.examples
  } catch (error) {
    showError(error)
  }
}

async function saveMemory() {
  try {
    const updated = await setAdminMemory(editForm.id, Math.max(0, editForm.memoryCount))
    editForm.memoryCount = updated.memoryCount
    ElMessage.success('记忆次数已更新。')
    await load()
  } catch (error) {
    showError(error)
  }
}

onMounted(async () => {
  await initThemes()
  await load()
})
</script>

<template>
  <section class="vocab-admin">
    <div class="vocab-admin__header">
      <h1 class="vocab-admin__title">词汇管理</h1>
    </div>

    <div class="vocab-admin__filters">
      <el-select
        v-model="filters.themeId"
        placeholder="全部主题"
        clearable
        filterable
        style="width: 300px"
        @change="search"
      >
        <el-option v-for="option in themeOptions" :key="option.id" :label="option.label" :value="option.id" />
      </el-select>
      <el-input
        v-model="filters.q"
        placeholder="搜索英文原词 / 中文翻译"
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button @click="search">搜索</el-button>
    </div>

    <el-table v-loading="loading" :data="items" style="width: 100%">
      <el-table-column prop="partOfSpeech" label="词性" width="120" />
      <el-table-column prop="word" label="英文原词" min-width="140" />
      <el-table-column prop="phoneticUs" label="美式音标" min-width="130" />
      <el-table-column prop="translation" label="中文翻译" min-width="200" show-overflow-tooltip />
      <el-table-column prop="memoryCount" label="记忆次数" width="100" />
      <el-table-column label="最近记忆" width="130">
        <template #default="{ row }">{{ formatMemoryTime(row.lastMemoryAt) || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="totalPages > 1"
      v-model:current-page="filters.page"
      :page-size="filters.pageSize"
      :total="total"
      layout="prev, pager, next"
      style="margin-top: var(--space-5)"
      @current-change="load"
    />

    <!-- edit dialog -->
    <el-dialog v-model="editVisible" title="编辑词条" width="720px" top="6vh">
      <el-form v-loading="saving" label-position="top">
        <div class="vocab-admin__edit-head">
          <span class="vocab-admin__edit-pos">{{ editForm.partOfSpeech }}</span>
          <span class="vocab-admin__edit-word">{{ editForm.word }}</span>
        </div>
        <div class="vocab-admin__edit-grid">
          <el-form-item label="中文翻译">
            <el-input v-model="editForm.translation" maxlength="1000" />
          </el-form-item>
          <el-form-item label="美式音标">
            <el-input v-model="editForm.phoneticUs" maxlength="100" />
          </el-form-item>
        </div>
        <el-form-item label="词形变化 / 派生词">
          <el-input v-model="editForm.inflections" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>

        <el-form-item label="例句（用户自加）">
          <div class="vocab-admin__examples">
            <div v-for="(example, i) in editForm.examples" :key="i" class="vocab-admin__example">
              <span class="vocab-admin__example-text">
                {{ example.sentence }}
                <span v-if="example.translation" class="vocab-admin__example-trans">{{ example.translation }}</span>
              </span>
              <el-button link type="danger" @click="removeExample(i)">删除</el-button>
            </div>
            <div v-if="!editForm.examples.length" class="vocab-admin__example-empty">暂无例句，请在下方添加。</div>
          </div>
          <div class="vocab-admin__example-add">
            <el-input v-model="exampleInput.sentence" placeholder="英文例句" maxlength="1000" style="flex: 2" />
            <el-input v-model="exampleInput.translation" placeholder="例句翻译（可选）" maxlength="1000" style="flex: 1" />
            <el-button @click="addExample">添加例句</el-button>
          </div>
        </el-form-item>

        <el-form-item label="记忆次数（手动修正）">
          <el-input-number v-model="editForm.memoryCount" :min="0" :controls="false" style="width: 160px" />
          <el-button style="margin-left: var(--space-3)" @click="saveMemory">更新记忆次数</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveWord">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.vocab-admin__title {
  font-size: 28px;
  line-height: 36px;
  margin-bottom: var(--space-6);
}

.vocab-admin__filters {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.vocab-admin__edit-head {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.vocab-admin__edit-pos {
  font-size: 13px;
  color: var(--accent);
}

.vocab-admin__edit-word {
  font-size: 26px;
  font-weight: 600;
}

.vocab-admin__edit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 var(--space-4);
}

.vocab-admin__examples {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.vocab-admin__example {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.vocab-admin__example-text {
  font-size: 14px;
  color: var(--text-primary);
}

.vocab-admin__example-trans {
  color: var(--text-muted);
  margin-left: var(--space-2);
}

.vocab-admin__example-empty {
  color: var(--text-muted);
  font-size: 13px;
  padding: var(--space-2) 0;
}

.vocab-admin__example-add {
  display: flex;
  gap: var(--space-3);
  width: 100%;
}

@media (max-width: 900px) {
  .vocab-admin__edit-grid,
  .vocab-admin__example-add {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
