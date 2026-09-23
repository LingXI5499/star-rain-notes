<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/http'
import MediaPicker from '@/components/MediaPicker.vue'
import type { MediaAsset } from '@/api/media'
import {
  createAdminTheme,
  createAdminWord,
  addAdminExample,
  addAdminWordAudio,
  deleteAdminWordAudio,
  fetchAdminWords,
  fetchVocabularyLayers,
  deleteAdminTheme,
  deleteAdminWord,
  removeAdminExample,
  setAdminMemory,
  setAdminWordPrimaryAudio,
  updateAdminWord,
  updateAdminTheme,
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
const activeLayerOrder = ref(0)
const activeThemeId = ref<number | undefined>()
const visibleLayers = computed(() => activeLayerOrder.value
  ? layers.value.filter((layer) => layer.layerOrder === activeLayerOrder.value)
  : layers.value)
const visibleThemes = computed(() => visibleLayers.value.flatMap((layer) =>
  layer.themes.map((theme) => ({ ...theme, layer: layer.layer, layerOrder: layer.layerOrder })),
))
const currentLayerName = computed(() => layers.value.find((layer) => layer.layerOrder === activeLayerOrder.value)?.layer ?? '')

const editVisible = ref(false)
const isNewWord = ref(false)
const editForm = reactive({
  id: 0,
  themeId: 0,
  word: '',
  partOfSpeech: '',
  translation: '',
  sceneMeaning: '',
  phoneticUs: '',
  phoneticUk: '',
  inflections: '',
  examples: [] as VocabularyExample[],
  memoryCount: 0,
  audios: [] as VocabularyWord['audios'],
})
const themeVisible = ref(false)
const themeSaving = ref(false)
const themeForm = reactive({ id: 0, layerOrder: 1, name: '' })
const exampleInput = reactive({ sentence: '', translation: '' })
const mediaVisible = ref(false)
const audioAccent = ref<'UK' | 'US'>('US')

async function load() {
  loading.value = true
  try {
    const result = await fetchAdminWords({
      themeId: filters.themeId,
      layerOrder: activeLayerOrder.value || undefined,
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
    if (activeThemeId.value && !themeOptions.value.some((theme) => theme.id === activeThemeId.value)) {
      activeThemeId.value = undefined
    }
    filters.themeId = activeThemeId.value
  } catch {
    ElMessage.error('加载主题失败。')
  }
}

function search() {
  filters.page = 1
  void load()
}

function selectLayer(layerOrder: number) {
  activeLayerOrder.value = layerOrder
  activeThemeId.value = undefined
  filters.themeId = undefined
  filters.page = 1
  void load()
}

function selectTheme(themeId?: number) {
  activeThemeId.value = themeId
  filters.themeId = themeId
  filters.page = 1
  void load()
}

function openEdit(word: VocabularyWord) {
  isNewWord.value = false
  editForm.id = word.id
  editForm.themeId = word.themeId
  editForm.word = word.word
  editForm.partOfSpeech = word.partOfSpeech
  editForm.translation = word.translation
  editForm.sceneMeaning = word.sceneMeaning ?? ''
  editForm.phoneticUs = word.phoneticUs ?? ''
  editForm.phoneticUk = word.phoneticUk ?? ''
  editForm.inflections = word.inflections ?? ''
  editForm.examples = word.examples.map((e) => ({ ...e }))
  editForm.memoryCount = word.memoryCount
  editForm.audios = word.audios ?? []
  exampleInput.sentence = ''
  exampleInput.translation = ''
  editVisible.value = true
}

function openCreateWord() {
  if (!activeThemeId.value) {
    ElMessage.warning('请先选择一个主题，再添加词条。')
    return
  }
  isNewWord.value = true
  editForm.id = 0
  editForm.themeId = activeThemeId.value
  editForm.word = ''
  editForm.partOfSpeech = ''
  editForm.translation = ''
  editForm.sceneMeaning = ''
  editForm.phoneticUs = ''
  editForm.phoneticUk = ''
  editForm.inflections = ''
  editForm.examples = []
  editForm.memoryCount = 0
  editForm.audios = []
  exampleInput.sentence = ''
  exampleInput.translation = ''
  editVisible.value = true
}

function openThemeEditor(theme?: { id: number; name: string; layerOrder: number }) {
  themeForm.id = theme?.id ?? 0
  themeForm.layerOrder = theme?.layerOrder ?? (activeLayerOrder.value || layers.value[0]?.layerOrder || 1)
  themeForm.name = theme?.name ?? ''
  themeVisible.value = true
}

async function saveTheme() {
  if (!themeForm.name.trim()) {
    ElMessage.warning('请填写主题名称。')
    return
  }
  themeSaving.value = true
  try {
    const payload = { layerOrder: themeForm.layerOrder, name: themeForm.name.trim() }
    const theme = themeForm.id
      ? await updateAdminTheme(themeForm.id, payload)
      : await createAdminTheme(payload)
    activeLayerOrder.value = themeForm.layerOrder
    if (!themeForm.id) activeThemeId.value = theme.id
    await initThemes()
    themeVisible.value = false
    await load()
    ElMessage.success(themeForm.id ? '分类已更新。' : '分类已创建。')
  } catch (error) {
    showError(error)
  } finally {
    themeSaving.value = false
  }
}

async function removeTheme(theme: { id: number; name: string }) {
  if (!window.confirm(`确认删除分类“${theme.name}”？分类下必须没有词条。`)) return
  try {
    await deleteAdminTheme(theme.id)
    if (activeThemeId.value === theme.id) activeThemeId.value = undefined
    await initThemes()
    await load()
    ElMessage.success('分类已删除。')
  } catch (error) {
    showError(error)
  }
}

async function removeWord(word: VocabularyWord) {
  if (!window.confirm(`确认删除单词“${word.word}”？该单词的记忆进度和复习记录也会一并删除。`)) return
  try {
    await deleteAdminWord(word.id)
    ElMessage.success('词条已删除。')
    filters.page = 1
    await Promise.all([load(), initThemes()])
  } catch (error) {
    showError(error)
  }
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
    const wasNew = isNewWord.value
    const previousThemeId = activeThemeId.value
    const payload = {
      themeId: editForm.themeId,
      word: editForm.word.trim(),
      partOfSpeech: editForm.partOfSpeech.trim(),
      translation: editForm.translation,
      sceneMeaning: editForm.sceneMeaning || null,
      phoneticUs: editForm.phoneticUs || null,
      phoneticUk: editForm.phoneticUk || null,
      inflections: editForm.inflections || null,
    }
    if (isNewWord.value) await createAdminWord({ ...payload, word: payload.word, themeId: payload.themeId })
    else await updateAdminWord(editForm.id, payload)
    ElMessage.success('已保存。')
    editVisible.value = false
    if (wasNew) filters.q = payload.word
    else if (previousThemeId && previousThemeId !== payload.themeId) {
      activeThemeId.value = payload.themeId
      filters.themeId = payload.themeId
    }
    filters.page = 1
    await Promise.all([load(), initThemes()])
  } catch (error) {
    showError(error)
  } finally {
    saving.value = false
  }
}

async function selectAudio(asset: MediaAsset) {
  if (asset.assetType !== 'AUDIO') return
  try {
    await addAdminWordAudio(editForm.id, {
      accent: audioAccent.value, mediaAssetId: asset.id, provider: 'UPLOADED',
      licenseNote: '本站上传且确认具有使用权的真人发音。', primary: !editForm.audios.some((item) => item.accent === audioAccent.value),
    })
    const refreshed = await fetchAdminWords({ q: editForm.word, page: 1, pageSize: 20 })
    const word = refreshed.items.find((item) => item.id === editForm.id)
    if (word) editForm.audios = word.audios
    ElMessage.success('发音已关联。')
  } catch (error) { showError(error) }
}

async function removeAudio(audioId: number) {
  try { await deleteAdminWordAudio(editForm.id, audioId); editForm.audios = editForm.audios.filter((item) => item.id !== audioId) }
  catch (error) { showError(error) }
}

async function makePrimary(audioId: number) {
  try {
    const selected = await setAdminWordPrimaryAudio(editForm.id, audioId)
    editForm.audios = editForm.audios.map((item) => item.accent === selected.accent ? { ...item, primary: item.id === audioId } : item)
  } catch (error) { showError(error) }
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
    <header class="vocab-admin__header">
      <div>
        <p class="vocab-admin__eyebrow">CET-4 · VOCABULARY TAXONOMY</p>
        <h1 class="vocab-admin__title">词汇管理</h1>
        <p class="vocab-admin__subtitle">先按六大分类定位主题，再在列表中维护主题词条。</p>
      </div>
      <div class="vocab-admin__header-actions">
        <el-button @click="openThemeEditor()">新建主题</el-button>
        <el-button type="primary" :disabled="!activeThemeId" @click="openCreateWord">添加词条</el-button>
      </div>
    </header>

    <nav class="vocab-admin__layers" aria-label="按一级分类筛选主题">
      <button :class="{ active: activeLayerOrder === 0 }" @click="selectLayer(0)">全部分类</button>
      <button v-for="layer in layers" :key="layer.layerOrder" :class="{ active: activeLayerOrder === layer.layerOrder }" @click="selectLayer(layer.layerOrder)">
        {{ layer.layer }}
      </button>
    </nav>

    <section v-if="visibleThemes.length" class="vocab-admin__taxonomy" aria-label="主题分类">
      <div class="vocab-admin__section-heading">
        <div>
          <h2>{{ activeLayerOrder ? currentLayerName : '全部主题' }}</h2>
          <p>选择主题查看词条；空主题可以删除，含词条主题需要先清空内容。</p>
        </div>
        <el-button text type="primary" @click="selectTheme(undefined)">显示全部词条</el-button>
      </div>
      <div class="vocab-admin__theme-grid">
        <article v-for="theme in visibleThemes" :key="theme.id" class="vocab-admin__theme-card" :class="{ selected: activeThemeId === theme.id }" @click="selectTheme(theme.id)">
          <div class="vocab-admin__theme-actions">
            <button type="button" aria-label="编辑分类" @click.stop="openThemeEditor(theme)">编辑</button>
            <button type="button" class="danger" aria-label="删除分类" @click.stop="removeTheme(theme)">删除</button>
          </div>
          <h3>{{ theme.name }}</h3>
          <p>{{ theme.wordCount.toLocaleString() }} 词</p>
        </article>
      </div>
    </section>

    <section class="vocab-admin__words">
      <div class="vocab-admin__words-heading">
        <div>
          <h2>{{ activeThemeId ? themeOptions.find((item) => item.id === activeThemeId)?.label.split(' › ').pop() : '词条列表' }}</h2>
          <p>共 {{ total.toLocaleString() }} 条分类词条</p>
        </div>
        <div class="vocab-admin__filters">
          <el-input v-model="filters.q" placeholder="搜索英文、中文释义或主题用法" clearable @keyup.enter="search" @clear="search" />
          <el-button @click="search">搜索</el-button>
        </div>
      </div>

      <div class="vocab-admin__table-wrap">
        <el-table v-loading="loading" :data="items" style="width: 100%">
          <el-table-column prop="partOfSpeech" label="词性" width="100" />
          <el-table-column prop="word" label="英文原词" min-width="150" />
          <el-table-column prop="phoneticUs" label="美式音标" min-width="130" />
          <el-table-column prop="phoneticUk" label="英式音标" min-width="130" />
          <el-table-column prop="translation" label="中文释义" min-width="190" show-overflow-tooltip />
          <el-table-column prop="sceneMeaning" label="主题用法" min-width="180" show-overflow-tooltip />
          <el-table-column v-if="!activeThemeId" label="所属主题" min-width="180">
            <template #default="{ row }">{{ themeOptions.find((theme) => theme.id === row.themeId)?.label ?? '—' }}</template>
          </el-table-column>
          <el-table-column prop="memoryCount" label="记忆次数" width="90" />
          <el-table-column label="最近记忆" width="115"><template #default="{ row }">{{ formatMemoryTime(row.lastMemoryAt) || '—' }}</template></el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="removeWord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-pagination v-if="totalPages > 1" v-model:current-page="filters.page" :page-size="filters.pageSize" :total="total" layout="prev, pager, next" @current-change="load" />
    </section>

    <el-dialog v-model="themeVisible" :title="themeForm.id ? '编辑分类' : '新建分类'" width="520px">
      <el-form label-position="top">
        <el-form-item label="所属一级分类">
          <el-select v-model="themeForm.layerOrder" style="width:100%">
            <el-option v-for="layer in layers" :key="layer.layerOrder" :label="layer.layer" :value="layer.layerOrder" />
          </el-select>
        </el-form-item>
        <el-form-item label="主题名称"><el-input v-model="themeForm.name" maxlength="200" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="themeVisible = false">取消</el-button><el-button type="primary" :loading="themeSaving" @click="saveTheme">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="isNewWord ? '添加词条' : '编辑词条'" width="760px" top="5vh">
      <el-form v-loading="saving" label-position="top">
        <div class="vocab-admin__edit-grid">
          <el-form-item label="所属主题">
            <el-select v-model="editForm.themeId" filterable style="width:100%"><el-option v-for="option in themeOptions" :key="option.id" :label="option.label" :value="option.id" /></el-select>
          </el-form-item>
          <el-form-item label="英文原词"><el-input v-model="editForm.word" maxlength="200" /></el-form-item>
          <el-form-item label="词性"><el-input v-model="editForm.partOfSpeech" maxlength="50" /></el-form-item>
          <el-form-item label="中文释义"><el-input v-model="editForm.translation" maxlength="1000" /></el-form-item>
          <el-form-item label="主题用法"><el-input v-model="editForm.sceneMeaning" maxlength="1000" /></el-form-item>
          <el-form-item label="美式音标"><el-input v-model="editForm.phoneticUs" maxlength="100" /></el-form-item>
          <el-form-item label="英式音标"><el-input v-model="editForm.phoneticUk" maxlength="100" /></el-form-item>
          <el-form-item label="词形变化 / 派生词"><el-input v-model="editForm.inflections" maxlength="1000" /></el-form-item>
        </div>

        <template v-if="!isNewWord">
          <el-form-item label="真人发音（仅关联具有使用权的音频）">
            <div class="vocab-admin__audio-list">
              <article v-for="audio in editForm.audios" :key="audio.id">
                <b>{{ audio.accent === 'UK' ? '英音' : '美音' }}</b><audio :src="audio.publicUrl" controls preload="none" />
                <el-tag v-if="audio.primary" type="success">主音频</el-tag><el-button v-else link @click="makePrimary(audio.id)">设为主音频</el-button>
                <el-button link type="danger" @click="removeAudio(audio.id)">移除关联</el-button>
              </article>
              <div class="vocab-admin__audio-add"><el-select v-model="audioAccent" style="width:110px"><el-option label="美音" value="US"/><el-option label="英音" value="UK"/></el-select><el-button @click="mediaVisible = true">上传或选择音频</el-button></div>
            </div>
          </el-form-item>
          <el-form-item label="例句（用户自加）">
            <div class="vocab-admin__examples">
              <div v-for="(example, i) in editForm.examples" :key="i" class="vocab-admin__example"><span>{{ example.sentence }} <small>{{ example.translation }}</small></span><el-button link type="danger" @click="removeExample(i)">删除</el-button></div>
              <div v-if="!editForm.examples.length" class="vocab-admin__example-empty">暂无例句，请在下方添加。</div>
            </div>
            <div class="vocab-admin__example-add"><el-input v-model="exampleInput.sentence" placeholder="英文例句" maxlength="1000" /><el-input v-model="exampleInput.translation" placeholder="例句翻译（可选）" maxlength="1000" /><el-button @click="addExample">添加例句</el-button></div>
          </el-form-item>
          <el-form-item label="记忆次数（手动修正）"><el-input-number v-model="editForm.memoryCount" :min="0" :controls="false" /><el-button style="margin-left:12px" @click="saveMemory">更新记忆次数</el-button></el-form-item>
        </template>
      </el-form>
      <template #footer><el-button @click="editVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveWord">保存</el-button></template>
    </el-dialog>
    <MediaPicker v-model="mediaVisible" asset-type="AUDIO" allow-upload title="选择单词真人发音" @select="selectAudio" />
  </section>
</template>

<style scoped>
.vocab-admin{padding-bottom:48px}.vocab-admin__header,.vocab-admin__section-heading,.vocab-admin__words-heading{display:flex;justify-content:space-between;align-items:flex-end;gap:20px}.vocab-admin__header{margin-bottom:24px}.vocab-admin__eyebrow{margin:0 0 8px;color:var(--accent);font-size:12px;letter-spacing:.15em}.vocab-admin__title{margin:0;font-size:clamp(28px,4vw,38px);line-height:1.15}.vocab-admin__subtitle,.vocab-admin__section-heading p,.vocab-admin__words-heading p{margin:8px 0 0;color:var(--text-muted);font-size:13px}.vocab-admin__header-actions{display:flex;gap:10px;flex-shrink:0}.vocab-admin__layers{display:flex;gap:8px;overflow-x:auto;padding:2px 2px 12px;margin-bottom:20px}.vocab-admin__layers button{flex-shrink:0;min-height:38px;padding:8px 16px;border:1px solid var(--border);border-radius:999px;background:var(--bg-surface);color:var(--text-secondary);cursor:pointer}.vocab-admin__layers button.active{border-color:var(--primary);background:var(--primary);color:var(--on-primary)}.vocab-admin__taxonomy{margin-bottom:34px}.vocab-admin__section-heading{align-items:center;margin-bottom:14px}.vocab-admin__section-heading h2,.vocab-admin__words-heading h2{margin:0;font-size:19px}.vocab-admin__theme-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:12px}.vocab-admin__theme-card{position:relative;display:flex;min-height:132px;flex-direction:column;justify-content:space-between;padding:18px;border:1px solid var(--border);border-radius:15px;background:var(--bg-surface);box-shadow:0 2px 6px rgb(0 0 0/.025);cursor:pointer;transition:border-color .16s,transform .16s,box-shadow .16s}.vocab-admin__theme-card:hover,.vocab-admin__theme-card.selected{border-color:var(--primary);box-shadow:0 7px 22px rgb(0 0 0/.06);transform:translateY(-1px)}.vocab-admin__theme-card.selected{outline:1px solid color-mix(in srgb,var(--primary) 34%,transparent)}.vocab-admin__theme-card h3{margin:18px 0 10px;font-size:16px;line-height:1.55;color:var(--text-primary)}.vocab-admin__theme-card p{margin:0;color:var(--text-muted);font-size:13px}.vocab-admin__theme-actions{position:absolute;top:10px;right:10px;display:flex;gap:2px;opacity:.8}.vocab-admin__theme-actions button{padding:4px 6px;border:0;background:transparent;color:var(--primary);font-size:12px;cursor:pointer}.vocab-admin__theme-actions .danger{color:var(--accent)}.vocab-admin__words{padding:20px;border:1px solid var(--border);border-radius:16px;background:var(--bg-surface)}.vocab-admin__words-heading{align-items:center;margin-bottom:16px}.vocab-admin__filters{display:flex;width:min(100%,520px);gap:8px}.vocab-admin__filters .el-input{min-width:180px}.vocab-admin__table-wrap{overflow:hidden;border:1px solid var(--border);border-radius:10px}.vocab-admin :deep(.el-table){--el-table-header-bg-color:var(--bg-subtle);--el-table-border-color:var(--border);--el-table-row-hover-bg-color:color-mix(in srgb,var(--primary) 5%,var(--bg-surface));color:var(--text-secondary)}.vocab-admin :deep(.el-pagination){justify-content:center;margin-top:18px}

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
  grid-template-columns: repeat(2,minmax(0,1fr));
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
.vocab-admin__audio-list{display:grid;width:100%;gap:10px}.vocab-admin__audio-list article{display:flex;align-items:center;gap:10px;padding:10px;border:1px solid var(--border);border-radius:10px}.vocab-admin__audio-list audio{width:250px;max-width:45%}.vocab-admin__audio-add{display:flex;gap:10px}

@media(max-width:900px){.vocab-admin__theme-grid{grid-template-columns:repeat(auto-fill,minmax(190px,1fr))}.vocab-admin__edit-grid{grid-template-columns:1fr}.vocab-admin__example-add{flex-direction:column}}
@media(max-width:640px){.vocab-admin__header,.vocab-admin__words-heading{align-items:stretch;flex-direction:column}.vocab-admin__header-actions{width:100%}.vocab-admin__header-actions>*{flex:1}.vocab-admin__theme-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.vocab-admin__theme-card{min-height:126px;padding:14px}.vocab-admin__theme-card h3{font-size:14px}.vocab-admin__words{padding:12px}.vocab-admin__filters{width:100%}.vocab-admin__section-heading{align-items:flex-start}}
</style>
