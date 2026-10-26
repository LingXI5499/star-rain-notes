<script setup>
/**
 * BlogTopicManagePage.vue：博客模块的页面组件。
 * 职责范围：文章、标签、专题、归档、发布状态和公开阅读。
 * 负责路由对应页面的数据加载、交互状态和内容展示，业务写入通过导入的接口执行。
 * 主要依赖：vue、vue-router、../../api/blogApi、../../../../shared/http、../../../../shared/ui/AppConfirmDialog.vue、../../components/admin/tagSlug。
 * 主要响应式状态：topics、members、postOptions、selectedTopicId、pendingPostId、topicKeyword、loading、saving。
 * 阅读入口：loadTopics（加载专题列表）；loadPosts（加载文章列表）；loadMembers（加载Members）；refresh（刷新当前操作）；openTopicDialog（打开专题弹窗）。
 */


import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  addTopicPost, createTopic, deleteTopic, disableTopic, enableTopic, listAdminPosts,
  listAdminTopics, listTopicMembers, removeTopicPost, reorderTopicPosts, reorderTopics, updateTopic,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import AppConfirmDialog from '../../../../shared/ui/AppConfirmDialog.vue'
import { createTaxonomy } from '../../components/admin/tagSlug'
import { postStatusLabel, taxonomyStatusLabel } from '../../support/display'
import { reorderTopicIds } from '../../support/topicOrder'

/*
 * 专题管理（BLOG-005 ~ BLOG-007）。
 *
 * 用户要求与标签管理拆成两页，并且「专题应该是左右两栏布局：左侧为该专题，
 * 右侧为该专题下的文章，和教程那个感觉一样」。因此这里照教程工作台的组织方式：
 *   左栏 = 专题本身（展示顺序、状态、成员数、点一下选中）；
 *   右栏 = 选中专题的文章（人工顺序、上下移动、移出、追加）。
 *
 * 删除：后端 `DELETE /admin/blog/topics/{id}` 只对**空专题**开放（有成员返回 409
 * BLOG_TOPIC_NOT_EMPTY）。前端按同一条规则先把按钮禁掉，用户看到的会是
 * 「先移出文章」的提示，而不是一次注定失败的请求。只想下架又不想丢成员与顺序时用「停用」。
 *
 * 顺序不变量：成员序号恒为 1..n。上下移动只改本地数组，点「保存顺序」才整体提交，
 * 避免每按一次就写一次库；专题之间的顺序同理，拖动或上下移后立即提交（专题数量少）。
 */
/**
 * 专题列表；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const topics = ref([])
/**
 * members；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const members = ref([])
/**
 * 文章选项；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const postOptions = ref([])
/**
 * 选中专题标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const selectedTopicId = ref(null)
/**
 * 待处理文章标识；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const pendingPostId = ref('')
/**
 * 专题关键词；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const topicKeyword = ref('')
/**
 * loading；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const loading = ref(false)
/**
 * saving；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const saving = ref(false)
/**
 * 错误；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const error = ref('')
/**
 * notice；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const notice = ref('')
/**
 * 专题弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const topicDialog = ref(null)
/**
 * 确认弹窗；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const confirmDialog = ref(null)

/**
 * 专题Form；通过 reactive 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const topicForm = reactive({ id: null, name: '', featured: true })
let draggingTopicId = null

/**
 * 选中专题；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const selectedTopic = computed(() => topics.value.find((topic) => topic.id === selectedTopicId.value) || null)
/**
 * addable文章列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const addablePosts = computed(() => {
  const taken = new Set(members.value.map((member) => member.postId))
  return postOptions.value.filter((post) => !taken.has(post.id))
})
/**
 * filtered专题列表；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const filteredTopics = computed(() => {
  const keyword = topicKeyword.value.trim().toLowerCase()
  if (!keyword) return topics.value
  return topics.value.filter((topic) => topic.name.toLowerCase().includes(keyword))
})

/**
 * 加载专题列表。
 * 主要协作：listAdminTopics、rows.push。
 * 更新的响应式状态包括 topics；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadTopics() {
  const first = await listAdminTopics({ page: 1, pageSize: 100 })
  const rows = [...(first.items || [])]
  for (let page = 2; page <= Math.ceil((first.total || 0) / 100); page += 1) {
    const next = await listAdminTopics({ page, pageSize: 100 })
    rows.push(...(next.items || []))
  }
  topics.value = rows
}

/**
 * 加载文章列表。
 * 主要协作：listAdminPosts。
 * 更新的响应式状态包括 postOptions；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadPosts() {
  // 专题成员只从已有的文章里挑，草稿也能加入（发布时顺序不变）
  const page = await listAdminPosts({ page: 1, pageSize: 100 })
  postOptions.value = page.items
}

/**
 * 加载Members。
 * 主要协作：listTopicMembers。
 * 更新的响应式状态包括 members；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function loadMembers() {
  if (!selectedTopicId.value) {
    members.value = []
    return
  }
  members.value = await listTopicMembers(selectedTopicId.value)
}

/**
 * 刷新当前操作。
 * 主要协作：Promise.all、loadTopics、loadPosts、topics.value.some、loadMembers。
 * 更新的响应式状态包括 loading、error、selectedTopicId；渲染由 Vue 的依赖跟踪触发。
 * 结束路径在 finally 中恢复忙碌或加载状态，成功与失败都会释放界面等待状态。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function refresh() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([loadTopics(), loadPosts()])
    // 选中的专题可能已被别处删掉，重新拉成员前先确认它还在
    if (selectedTopicId.value && !topics.value.some((topic) => topic.id === selectedTopicId.value)) {
      selectedTopicId.value = null
    }
    await loadMembers()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

/**
 * 打开专题弹窗。
 * 主要协作：Object.assign、nextTick、topicDialog.value.showModal。
 * 更新的响应式状态包括 error；渲染由 Vue 的依赖跟踪触发。
 * @param topic = null 专题空值；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function openTopicDialog(topic = null) {
  error.value = ''
  Object.assign(topicForm, topic
    ? { id: topic.id, name: topic.name, featured: topic.featured }
    : { id: null, name: '', featured: true })
  await nextTick()
  topicDialog.value?.showModal()
}

/**
 * 提交专题。
 * 主要协作：topicForm.name.trim、updateTopic、createTaxonomy、topicDialog.value.close、loadTopics。
 * 更新的响应式状态包括 error、saving、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function submitTopic() {
  if (!topicForm.name.trim()) {
    error.value = '请填写专题名称。'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const payload = { name: topicForm.name.trim(), featured: topicForm.featured }
    if (topicForm.id) {
      await updateTopic(topicForm.id, payload)
      notice.value = `专题「${topicForm.name}」已更新。`
    } else {
      await createTaxonomy(createTopic, payload.name, 'topic', 120, { featured: payload.featured })
      notice.value = `专题「${topicForm.name}」已创建。`
    }
    topicDialog.value?.close()
    await loadTopics()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/**
 * 切换专题状态。
 * 主要协作：enableTopic、disableTopic、loadTopics、errorMessage。
 * 更新的响应式状态包括 saving、error、notice；渲染由 Vue 的依赖跟踪触发。
 * @param topic 专题；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function toggleTopicStatus(topic) {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    if (topic.status === 'DISABLED') {
      await enableTopic(topic.id)
      notice.value = `专题「${topic.name}」已启用，成员与顺序保持不变。`
    } else {
      await disableTopic(topic.id)
      notice.value = `专题「${topic.name}」已停用：不在前台展示，也不能加入新文章；成员与顺序保留。`
    }
    await loadTopics()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/*
 * 删除专题。
 * 有成员时直接给提示而不是发请求：后端会用 409 BLOG_TOPIC_NOT_EMPTY 拒绝，
 * 让用户看到「为什么不能删」比看到一次失败请求更有用。
 */
async function removeTopic(topic) {
  error.value = ''
  notice.value = ''
  if ((topic.memberCount || 0) > 0) {
    error.value = `「${topic.name}」下还有 ${topic.memberCount} 篇文章：请先在右侧把它们全部移出，再删除专题。`
    return
  }
  const accepted = await confirmDialog.value.ask({
    title: '删除专题',
    message: `确定删除专题「${topic.name}」？删除后不可恢复，文章本身不受影响。`,
    confirmText: '删除',
    danger: true,
  })
  if (!accepted) return
  saving.value = true
  try {
    await deleteTopic(topic.id)
    if (selectedTopicId.value === topic.id) selectedTopicId.value = null
    notice.value = `专题「${topic.name}」已删除。`
    await loadTopics()
    await loadMembers()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/**
 * move专题的处理函数。
 * 主要协作：topics.value.map、ids.indexOf、ids.splice、reorderTopics、loadTopics。
 * 更新的响应式状态包括 saving、notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param topic 专题；默认值、解构字段与可选参数见函数签名。
 * @param delta delta；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function moveTopic(topic, delta) {
  const ids = topics.value.map((item) => item.id)
  const from = ids.indexOf(topic.id)
  const to = from + delta
  if (to < 0 || to >= ids.length) return
  ids.splice(from, 1)
  ids.splice(to, 0, topic.id)
  saving.value = true
  try {
    await reorderTopics(ids)
    await loadTopics()
    notice.value = '专题展示顺序已更新。'
  } catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

/**
 * drop专题的处理函数。
 * 主要协作：reorderTopicIds、topics.value.map、reorderTopics、loadTopics、errorMessage。
 * 更新的响应式状态包括 saving、notice、error；渲染由 Vue 的依赖跟踪触发。
 * @param target 目标；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function dropTopic(target) {
  if (!draggingTopicId || draggingTopicId === target.id) return
  const ids = reorderTopicIds(topics.value.map((item) => item.id), draggingTopicId, target.id)
  draggingTopicId = null
  saving.value = true
  try { await reorderTopics(ids); await loadTopics(); notice.value = '专题展示顺序已更新。' }
  catch (cause) { error.value = errorMessage(cause) }
  finally { saving.value = false }
}

// 再点一次同一个专题 = 收起右栏，等价于原来那个「关闭」按钮
/**
 * 查询专题的处理函数。
 * 主要协作：loadMembers、errorMessage。
 * 更新的响应式状态包括 selectedTopicId、pendingPostId、error；渲染由 Vue 的依赖跟踪触发。
 * @param topic 专题；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function selectTopic(topic) {
  selectedTopicId.value = selectedTopicId.value === topic.id ? null : topic.id
  pendingPostId.value = ''
  error.value = ''
  try {
    await loadMembers()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

// 上下移动只改本地顺序，点「保存顺序」才整体提交，避免每按一次就写一次库
/**
 * move的处理函数。
 * 主要协作：items.splice、items.map。
 * 更新的响应式状态包括 members；渲染由 Vue 的依赖跟踪触发。
 * @param index 索引；默认值、解构字段与可选参数见函数签名。
 * @param delta delta；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function move(index, delta) {
  const target = index + delta
  if (target < 0 || target >= members.value.length) return
  const items = [...members.value]
  const [moved] = items.splice(index, 1)
  items.splice(target, 0, moved)
  members.value = items.map((item, position) => ({ ...item, sortOrder: position + 1 }))
}

/**
 * 保存顺序。
 * 主要协作：reorderTopicPosts、members.value.map、loadMembers、errorMessage。
 * 更新的响应式状态包括 saving、error、notice；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function saveOrder() {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    await reorderTopicPosts(selectedTopicId.value, members.value.map((item) => item.postId))
    notice.value = '专题顺序已保存。'
    await loadMembers()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/**
 * 添加Member。
 * 主要协作：addTopicPost、loadMembers、loadTopics、errorMessage。
 * 更新的响应式状态包括 saving、error、notice、pendingPostId；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function addMember() {
  if (!pendingPostId.value || !selectedTopicId.value) return
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    // 新成员一律追加到末尾：加入位置由「保存顺序」显式决定，而不是靠插入算法猜
    await addTopicPost(selectedTopicId.value, pendingPostId.value)
    pendingPostId.value = ''
    notice.value = '已加入专题末尾。'
    await loadMembers()
    await loadTopics()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/**
 * 移除Member。
 * 主要协作：removeTopicPost、loadMembers、loadTopics、errorMessage。
 * 更新的响应式状态包括 saving、error、notice；渲染由 Vue 的依赖跟踪触发。
 * @param member member；默认值、解构字段与可选参数见函数签名。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function removeMember(member) {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    await removeTopicPost(selectedTopicId.value, member.postId)
    notice.value = `已移出《${member.title}》，后面的文章序号自动前移。`
    await loadMembers()
    await loadTopics()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

/**
 * 组件挂载后执行初始化工作，依赖此时已存在的 DOM 或初始状态。
 */
onMounted(refresh)
</script>

<template>
  <section class="topic-manage">
    <header class="tag-admin__hero">
      <div>
        <p>TOPIC WORKSPACE · 专题策展</p>
        <h1>专题管理</h1>
        <span>左侧是专题与展示顺序，右侧是选中专题的文章与顺序；空专题可以删除。</span>
      </div>
      <div class="content-admin__hero-actions">
        <RouterLink to="/blog/manage">← 返回博客管理</RouterLink>
        <RouterLink to="/blog/taxonomy">标签管理 →</RouterLink>
        <button class="primary-button" type="button" @click="openTopicDialog()">＋ 新建专题</button>
      </div>
    </header>

    <p v-if="error && !topicDialog?.open" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载专题…</p>

    <div class="topic-manage__layout">
      <aside class="topic-manage__panel">
        <header class="topic-manage__panel-head">
          <div><span>01 · TOPICS</span><h2>专题</h2></div>
          <input v-model="topicKeyword" type="search" class="topic-manage__filter" placeholder="搜索专题" aria-label="搜索专题" />
        </header>
        <p class="topic-manage__hint">拖动左侧手柄调整展示顺序；点一下专题，在右侧管理它的文章。</p>
        <div class="topic-manage__body">
          <p v-if="!loading && !filteredTopics.length" class="topic-manage__empty-message">
            {{ topicKeyword ? '没有匹配的专题' : '暂无专题，先新建一个。' }}
          </p>
          <article
            v-for="(topic, index) in filteredTopics"
            :key="topic.id"
            class="topic-manage__item"
            :class="{ 'is-active': selectedTopicId === topic.id }"
            :draggable="!saving && !topicKeyword"
            @click="selectTopic(topic)"
            @dragstart="draggingTopicId = topic.id"
            @dragover.prevent
            @drop.prevent="dropTopic(topic)"
          >
            <span class="topic-manage__grip" aria-hidden="true">⠿</span>
            <div class="topic-manage__item-copy">
              <h3>{{ topic.name }}</h3>
              <p>
                序号 {{ String(index + 1).padStart(2, '0') }} · {{ topic.memberCount || 0 }} 篇文章<template v-if="topic.featured"> · 精选</template>
              </p>
            </div>
            <span class="topic-manage__count">{{ topic.memberCount || 0 }}</span>
            <span :class="['topic-manage__state', topic.status === 'DISABLED' && 'is-off']">
              {{ taxonomyStatusLabel(topic.status) }}
            </span>
            <div class="topic-manage__item-actions">
              <button type="button" :disabled="saving || !!topicKeyword || index === 0" @click.stop="moveTopic(topic, -1)">上移</button>
              <button type="button" :disabled="saving || !!topicKeyword || index === filteredTopics.length - 1" @click.stop="moveTopic(topic, 1)">下移</button>
              <button type="button" @click.stop="openTopicDialog(topic)">编辑</button>
              <button type="button" :disabled="saving" @click.stop="toggleTopicStatus(topic)">
                {{ topic.status === 'DISABLED' ? '启用' : '停用' }}
              </button>
              <button
                class="danger"
                type="button"
                :disabled="saving || (topic.memberCount || 0) > 0"
                :title="(topic.memberCount || 0) > 0 ? `专题下还有 ${topic.memberCount} 篇文章，先全部移出才能删除` : '删除这个空专题'"
                @click.stop="removeTopic(topic)"
              >删除</button>
            </div>
          </article>
        </div>
      </aside>

      <section class="topic-manage__panel topic-manage__panel--posts">
        <header class="topic-manage__panel-head topic-manage__panel-head--stack">
          <div>
            <span>02 · POSTS</span>
            <h2>{{ selectedTopic?.name || '请选择专题' }}</h2>
            <small v-if="selectedTopic">{{ selectedTopic.memberCount || 0 }} 篇文章 · {{ taxonomyStatusLabel(selectedTopic.status) }}</small>
          </div>
          <div class="topic-manage__tools">
            <select v-model="pendingPostId" :disabled="!selectedTopic" aria-label="选择要加入专题的文章">
              <option value="">选择一篇文章</option>
              <option v-for="post in addablePosts" :key="post.id" :value="post.id">
                {{ post.title }}（{{ postStatusLabel(post.status) }}）
              </option>
            </select>
            <button class="primary-button" type="button" :disabled="saving || !pendingPostId || !selectedTopic" @click="addMember">追加到末尾</button>
          </div>
        </header>
        <p class="topic-manage__hint">上下移动只改本地顺序，点「保存顺序」才整体提交；停用的专题不能加入新文章。</p>
        <div class="topic-manage__body">
          <div v-if="!selectedTopic" class="topic-manage__empty">
            <span aria-hidden="true">←</span>
            <h3>先选择一个专题</h3>
            <p>右侧只会显示该专题的文章与顺序。</p>
          </div>
          <template v-else>
            <div v-if="!members.length" class="topic-manage__empty">
              <span aria-hidden="true">⌁</span>
              <h3>该专题还没有文章</h3>
              <p>用右上角的「选择一篇文章」追加到末尾。</p>
            </div>
            <ol v-else class="blog-member-list">
              <li v-for="(member, index) in members" :key="member.postId">
                <span class="blog-member-list__order">{{ index + 1 }}</span>
                <span class="blog-member-list__title">
                  {{ member.title }}
                  <small>{{ postStatusLabel(member.status) }}</small>
                </span>
                <span class="blog-member-list__actions">
                  <button class="link-button" type="button" :disabled="index === 0" @click="move(index, -1)">上移</button>
                  <button class="link-button" type="button" :disabled="index === members.length - 1" @click="move(index, 1)">下移</button>
                  <button class="link-button blog-danger" type="button" :disabled="saving" @click="removeMember(member)">移出</button>
                </span>
              </li>
            </ol>
            <div class="blog-edit__actions blog-edit__actions--split">
              <div class="blog-edit__actions-group">
                <span class="muted">共 {{ members.length }} 篇，按上面的顺序在前台展示。</span>
              </div>
              <div class="blog-edit__actions-group">
                <button class="primary-button" type="button" :disabled="saving || !members.length" @click="saveOrder">保存顺序</button>
              </div>
            </div>
          </template>
        </div>
      </section>
    </div>

    <p class="tag-admin__note">
      删除只对空专题开放：专题的价值在成员与顺序上，物理删除会连带毁掉策展结果，
      所以有文章时请先在右侧移出。只想下架、还要留住成员与顺序时用「停用」。
      标签在「标签管理」页，未被文章引用的标签也可删除。
    </p>

    <dialog ref="topicDialog" aria-labelledby="topic-dialog-title" @cancel.prevent="topicDialog?.close()">
      <h2 id="topic-dialog-title">{{ topicForm.id ? '编辑专题' : '新建专题' }}</h2>
      <form class="form-stack" @submit.prevent="submitTopic">
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <label>名称<input v-model="topicForm.name" maxlength="160" placeholder="例如：Java 学习路线" /></label>
        <label class="topic-manage__check"><input v-model="topicForm.featured" type="checkbox" /> 精选专题（数量过多时优先显示在博客导航）</label>
        <div class="dialog-actions">
          <button type="button" @click="topicDialog?.close()">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">{{ topicForm.id ? '保存' : '创建' }}</button>
        </div>
      </form>
    </dialog>

    <AppConfirmDialog ref="confirmDialog" />
  </section>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.topic-manage。
 */
.topic-manage { min-width: 0; }

/**
 * 样式范围：.topic-manage__layout。
 * 使用网格布局组织内容；列定义为 minmax(300px, 380px) minmax(0, 1fr)，间距由 20px 控制。
 */
.topic-manage__layout {
  display: grid;
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

/**
 * 样式范围：.topic-manage__panel。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 hidden。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--bg-surface);
  box-shadow: 0 12px 36px rgb(17 35 29 / 0.055);
  overflow: hidden;
}

/**
 * 样式范围：.topic-manage__panel-head。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__panel-head {
  display: flex;
  min-height: 84px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-subtle) 52%, transparent);
}

/**
 * 样式范围：.topic-manage__panel-head--stack。
 */
.topic-manage__panel-head--stack { align-items: flex-end; }

/**
 * 样式范围：.topic-manage__panel-head > div > span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__panel-head > div > span {
  color: var(--accent);
  font: 700 9px/1.3 var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace);
  letter-spacing: 0.13em;
}

/**
 * 样式范围：.topic-manage__panel-head h2。
 */
.topic-manage__panel-head h2 { margin-top: 5px; font-size: 18px; line-height: 1.3; }

/**
 * 样式范围：.topic-manage__panel-head small。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__panel-head small { color: var(--text-muted); font-size: 11px; }

/**
 * 样式范围：.topic-manage__filter。
 */
.topic-manage__filter {
  width: min(190px, 45%);
  min-height: 38px;
  padding: 7px 10px;
}

/**
 * 样式范围：.topic-manage__hint。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__hint {
  margin: 0;
  padding: 9px 20px;
  border-bottom: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 11px;
}

/**
 * 样式范围：.topic-manage__body。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 溢出处理：整体 auto。
 */
.topic-manage__body {
  display: flex;
  min-height: 300px;
  flex: 1;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
  overflow: auto;
}

/**
 * 样式范围：.topic-manage__item。
 * 使用网格布局组织内容；列定义为 16px minmax(0, 1fr) auto auto，间距由 9px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__item {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 9px;
  padding: 11px;
  border: 1px solid transparent;
  border-radius: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: transform 170ms ease, background-color 170ms ease, border-color 170ms ease, box-shadow 170ms ease;
}

/**
 * 样式范围：.topic-manage__item:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__item:hover {
  border-color: var(--border);
  background: var(--bg-subtle);
  transform: translateX(3px);
}

/**
 * 样式范围：.topic-manage__item.is-active。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__item.is-active {
  border-color: color-mix(in srgb, var(--primary) 28%, var(--border));
  background: color-mix(in srgb, var(--primary) 10%, var(--bg-surface));
  box-shadow: 0 7px 20px color-mix(in srgb, var(--primary) 12%, transparent);
}

/**
 * 样式范围：.topic-manage__grip。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__grip { color: var(--text-muted); letter-spacing: -4px; cursor: grab; }

/**
 * 样式范围：.topic-manage__item-copy。
 */
.topic-manage__item-copy { min-width: 0; }

/**
 * 样式范围：.topic-manage__item-copy h3。
 * 溢出处理：整体 hidden。
 * 长文本使用省略号收束，配合 overflow 与 white-space 避免挤压邻列。
 */
.topic-manage__item-copy h3 {
  overflow: hidden;
  font-size: 13px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 样式范围：.topic-manage__item-copy p。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__item-copy p { margin-top: 3px; color: var(--text-muted); font-size: 10px; }

/**
 * 样式范围：.topic-manage__count。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__count {
  min-width: 27px;
  padding: 3px 7px;
  border-radius: 999px;
  color: var(--text-muted);
  background: var(--bg-page);
  font-size: 10px;
  text-align: center;
}

/**
 * 样式范围：.topic-manage__state。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__state {
  padding: 3px 8px;
  border-radius: 999px;
  color: var(--primary);
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  font-size: 10px;
  white-space: nowrap;
}

/**
 * 样式范围：.topic-manage__state.is-off。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__state.is-off { color: var(--text-muted); background: var(--bg-page); }

/**
 * 样式范围：.topic-manage__item-actions。
 */
.topic-manage__item-actions {
  display: none;
  grid-column: 2 / 5;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 5px;
}

/**
 * 样式范围：.topic-manage__item:hover .topic-manage__item-actions, .topic-manage__item:focus-within .topic-manage__item-actions。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.topic-manage__item:hover .topic-manage__item-actions,
.topic-manage__item:focus-within .topic-manage__item-actions { display: flex; }

/**
 * 样式范围：.topic-manage__item-actions button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__item-actions button {
  padding: 0;
  border: 0;
  color: var(--primary);
  background: none;
  font-size: 11px;
  cursor: pointer;
}

/**
 * 样式范围：.topic-manage__item-actions button:hover:not(:disabled)。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 */
.topic-manage__item-actions button:hover:not(:disabled) { background: none; text-decoration: underline; }

/**
 * 样式范围：.topic-manage__item-actions button:disabled。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__item-actions button:disabled { color: var(--text-muted); cursor: not-allowed; opacity: 0.6; }

/**
 * 样式范围：.topic-manage__item-actions .danger。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__item-actions .danger { color: var(--danger); }

/**
 * 样式范围：.topic-manage__tools。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.topic-manage__tools {
  display: flex;
  align-items: center;
  gap: 10px;
  width: min(430px, 55%);
}

/**
 * 样式范围：.topic-manage__tools select。
 */
.topic-manage__tools select { min-width: 0; }

/**
 * 样式范围：.topic-manage__tools button。
 */
.topic-manage__tools button { white-space: nowrap; }

/**
 * 样式范围：.topic-manage__empty, .topic-manage__empty-message。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__empty,
.topic-manage__empty-message { color: var(--text-muted); text-align: center; }

/**
 * 样式范围：.topic-manage__empty。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 7px 控制。
 */
.topic-manage__empty {
  display: grid;
  min-height: 260px;
  place-content: center;
  gap: 7px;
}

/**
 * 样式范围：.topic-manage__empty span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__empty span { color: var(--primary); font-size: 30px; }

/**
 * 样式范围：.topic-manage__empty h3。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.topic-manage__empty h3 { color: var(--text-secondary); font-size: 16px; }

/**
 * 样式范围：.topic-manage__empty p, .topic-manage__empty-message。
 */
.topic-manage__empty p,
.topic-manage__empty-message { font-size: 12px; }

/**
 * 样式范围：.topic-manage__empty-message。
 */
.topic-manage__empty-message { padding: 38px 12px; }

/**
 * 样式范围：.topic-manage__check。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.topic-manage__check { display: flex; align-items: center; gap: 8px; }

/**
 * 样式范围：.topic-manage__check input。
 */
.topic-manage__check input { width: auto; min-height: auto; }

/**
 * 响应式或设备条件：(prefers-reduced-motion: reduce)；仅在该条件成立时覆盖基础样式。
 */
@media (prefers-reduced-motion: reduce) {
  /**
   * 样式范围：.topic-manage__item。
   */
  .topic-manage__item { transition: none; }
}

/**
 * 响应式或设备条件：(max-width: 1100px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 1100px) {
  /**
   * 样式范围：.topic-manage__layout。
   */
  .topic-manage__layout { grid-template-columns: minmax(260px, 320px) minmax(0, 1fr); }

  /**
   * 样式范围：.topic-manage__panel-head--stack。
   */
  .topic-manage__panel-head--stack { align-items: flex-start; flex-direction: column; }

  /**
   * 样式范围：.topic-manage__tools。
   */
  .topic-manage__tools { width: 100%; }
}

/**
 * 响应式或设备条件：(max-width: 820px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width: 820px) {
  /**
   * 样式范围：.topic-manage__layout。
   */
  .topic-manage__layout { grid-template-columns: 1fr; }

  /**
   * 样式范围：.topic-manage__tools。
   */
  .topic-manage__tools { align-items: stretch; flex-direction: column; }

  /**
   * 样式范围：.topic-manage__filter。
   */
  .topic-manage__filter { width: 100%; }
}
</style>
