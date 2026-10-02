<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import {
  addTopicPost, createTag, createTopic, disableTag, disableTopic, enableTag, enableTopic,
  listAdminPosts, listAdminTags, listAdminTopics, listTopicMembers, removeTopicPost,
  reorderTopicPosts, updateTag, updateTopic,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import { postStatusLabel, taxonomyStatusLabel } from '../../support/display'

/*
 * 分类与专题管理（BLOG-004 ~ BLOG-007），组织方式对齐 V1 views/admin/BlogTagView.vue：
 * 标题区 + 计数 + 卡片网格。
 *
 * 这里仍然把 Tag 与 Topic 的语义差异摆在明面上：
 * - Tag：只有名称，没有任何顺序概念；停用只是不再接受新绑定；
 * - Topic：除了名称，还有成员列表与人工顺序，顺序用上下移动调整后整体提交。
 *
 * 两边都只有启用 / 停用，没有物理删除：已被文章使用的分类或专题一旦被删除，
 * 历史文章就会指向不存在的分类（后端也不提供删除接口）。
 */
const tags = ref([])
const topics = ref([])
const members = ref([])
const postOptions = ref([])
const selectedTopicId = ref(null)
const pendingPostId = ref('')
const tagKeyword = ref('')
const topicKeyword = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const tagDialog = ref(null)
const topicDialog = ref(null)

const tagForm = reactive({ id: null, name: '' })
const topicForm = reactive({ id: null, name: '' })

const selectedTopic = computed(() => topics.value.find((topic) => topic.id === selectedTopicId.value) || null)
const addablePosts = computed(() => {
  const taken = new Set(members.value.map((member) => member.postId))
  return postOptions.value.filter((post) => !taken.has(post.id))
})
const totalRelations = computed(() => tags.value.reduce((sum, tag) => sum + (tag.postCount || 0), 0))
const filteredTags = computed(() => {
  const keyword = tagKeyword.value.trim().toLowerCase()
  if (!keyword) return tags.value
  return tags.value.filter((tag) => tag.name.toLowerCase().includes(keyword))
})
const filteredTopics = computed(() => {
  const keyword = topicKeyword.value.trim().toLowerCase()
  if (!keyword) return topics.value
  return topics.value.filter((topic) => topic.name.toLowerCase().includes(keyword))
})

async function loadTags() {
  const page = await listAdminTags({ page: 1, pageSize: 100 })
  tags.value = page.items
}

async function loadTopics() {
  const page = await listAdminTopics({ page: 1, pageSize: 100 })
  topics.value = page.items
}

async function loadPosts() {
  // 专题成员只从已有的文章里挑，草稿也能加入（发布时顺序不变）
  const page = await listAdminPosts({ page: 1, pageSize: 100 })
  postOptions.value = page.items
}

async function loadMembers() {
  if (!selectedTopicId.value) {
    members.value = []
    return
  }
  members.value = await listTopicMembers(selectedTopicId.value)
}

async function refresh() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([loadTags(), loadTopics(), loadPosts()])
    // 选中的专题可能已被别处改动，重新拉一次成员保持一致
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

// ---------------------------------------------------------------------
// Tag
// ---------------------------------------------------------------------

async function openTagDialog(tag = null) {
  Object.assign(tagForm, tag
    ? { id: tag.id, name: tag.name }
    : { id: null, name: '' })
  await nextTick()
  tagDialog.value?.showModal()
}

async function submitTag() {
  if (!tagForm.name.trim()) {
    error.value = '请填写标签名称。'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    // 创建时由服务端派生唯一地址；改名时不传 slug，已有公开链接保持不变。
    const payload = { name: tagForm.name.trim() }
    if (tagForm.id) {
      await updateTag(tagForm.id, payload)
      notice.value = `标签「${tagForm.name}」已更新。`
    } else {
      await createTag(payload)
      notice.value = `标签「${tagForm.name}」已创建。`
    }
    tagDialog.value?.close()
    await loadTags()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

async function toggleTagStatus(tag) {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    if (tag.status === 'DISABLED') {
      await enableTag(tag.id)
      notice.value = `标签「${tag.name}」已启用。`
    } else {
      await disableTag(tag.id)
      notice.value = `标签「${tag.name}」已停用：历史绑定保留，但不能绑定到新文章。`
    }
    await loadTags()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------------
// Topic
// ---------------------------------------------------------------------

async function openTopicDialog(topic = null) {
  Object.assign(topicForm, topic
    ? { id: topic.id, name: topic.name }
    : { id: null, name: '' })
  await nextTick()
  topicDialog.value?.showModal()
}

async function submitTopic() {
  if (!topicForm.name.trim()) {
    error.value = '请填写专题名称。'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const payload = { name: topicForm.name.trim() }
    if (topicForm.id) {
      await updateTopic(topicForm.id, payload)
      notice.value = `专题「${topicForm.name}」已更新。`
    } else {
      await createTopic(payload)
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
      notice.value = `专题「${topic.name}」已停用：不在前台展示，也不能加入新文章。`
    }
    await loadTopics()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------------
// 专题成员与顺序
// ---------------------------------------------------------------------

async function selectTopic(topic) {
  selectedTopicId.value = selectedTopicId.value === topic.id ? null : topic.id
  error.value = ''
  try {
    await loadMembers()
  } catch (cause) {
    error.value = errorMessage(cause)
  }
}

// 上下移动只改本地顺序，点「保存顺序」才整体提交，避免每按一次就写一次库
function move(index, delta) {
  const target = index + delta
  if (target < 0 || target >= members.value.length) return
  const items = [...members.value]
  const [moved] = items.splice(index, 1)
  items.splice(target, 0, moved)
  members.value = items.map((item, position) => ({ ...item, sortOrder: position + 1 }))
}

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

async function addMember() {
  if (!pendingPostId.value) return
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

onMounted(refresh)
</script>

<template>
  <section class="tag-admin">
    <header class="tag-admin__hero">
      <div>
        <p>TAG LIBRARY · 内容索引</p>
        <h1>分类与专题</h1>
      </div>
      <div class="content-admin__hero-actions">
        <button type="button" @click="openTopicDialog()">＋ 新建专题</button>
        <button class="primary-button" type="button" @click="openTagDialog()">＋ 新建标签</button>
      </div>
    </header>

    <div class="tag-admin__stats">
      <div><strong>{{ tags.length }}</strong><span>标签总数</span></div>
      <div><strong>{{ totalRelations }}</strong><span>文章关联</span></div>
      <label>搜索标签<input v-model="tagKeyword" placeholder="输入标签名称" /></label>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载分类数据…</p>

    <div class="tag-grid">
      <article v-for="tag in filteredTags" :key="tag.id" class="tag-card">
        <div class="tag-card__top">
          <span>#</span>
          <strong>{{ tag.name }}</strong>
          <em>{{ tag.postCount || 0 }}</em>
        </div>
        <footer>
          <span :class="['status-chip', tag.status === 'DISABLED' && 'status-chip--danger']">{{ taxonomyStatusLabel(tag.status) }}</span>
          <div>
            <button type="button" @click="openTagDialog(tag)">编辑</button>
            <button type="button" :disabled="saving" @click="toggleTagStatus(tag)">
              {{ tag.status === 'DISABLED' ? '启用' : '停用' }}
            </button>
          </div>
        </footer>
      </article>
      <div v-if="!loading && !filteredTags.length" class="tag-admin__empty">
        {{ tagKeyword ? '没有匹配的标签' : '暂无标签' }}
      </div>
    </div>

    <div class="tag-admin__section-head">
      <h2>专题（有序策展）</h2>
      <label class="tag-admin__search">搜索专题<input v-model="topicKeyword" placeholder="输入专题名称" /></label>
    </div>

    <div class="tag-grid">
      <article
        v-for="topic in filteredTopics"
        :key="topic.id"
        :class="['tag-card', selectedTopicId === topic.id && 'is-selected']"
      >
        <div class="tag-card__top">
          <span>专</span>
          <strong>{{ topic.name }}</strong>
          <em>{{ topic.memberCount || 0 }}</em>
        </div>
        <footer>
          <span :class="['status-chip', topic.status === 'DISABLED' && 'status-chip--danger']">{{ taxonomyStatusLabel(topic.status) }}</span>
          <div>
            <button type="button" :disabled="saving" @click="selectTopic(topic)">
              {{ selectedTopicId === topic.id ? '收起顺序' : '管理顺序' }}
            </button>
            <button type="button" @click="openTopicDialog(topic)">编辑</button>
            <button type="button" :disabled="saving" @click="toggleTopicStatus(topic)">
              {{ topic.status === 'DISABLED' ? '启用' : '停用' }}
            </button>
          </div>
        </footer>
      </article>
      <div v-if="!loading && !filteredTopics.length" class="tag-admin__empty">
        {{ topicKeyword ? '没有匹配的专题' : '暂无专题' }}
      </div>
    </div>

    <section v-if="selectedTopic" class="surface-card tag-admin__members">
      <div class="section-heading">
        <div>
          <h2>专题内文章顺序 · {{ selectedTopic.name }}</h2>
          <p class="muted">上下移动只改本地顺序，点「保存顺序」才整体提交。</p>
        </div>
        <button class="text-button" type="button" @click="selectedTopicId = null">关闭</button>
      </div>

      <ol class="blog-member-list">
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
        <li v-if="!members.length" class="muted">该专题还没有文章。</li>
      </ol>

      <div class="blog-edit__actions blog-edit__actions--split">
        <div class="blog-edit__actions-group">
          <select v-model="pendingPostId" class="tag-admin__post-select">
            <option value="">选择一篇文章</option>
            <option v-for="post in addablePosts" :key="post.id" :value="post.id">
              {{ post.title }}（{{ postStatusLabel(post.status) }}）
            </option>
          </select>
          <button type="button" :disabled="saving || !pendingPostId" @click="addMember">追加到末尾</button>
        </div>
        <div class="blog-edit__actions-group">
          <button class="primary-button" type="button" :disabled="saving || !members.length" @click="saveOrder">保存顺序</button>
        </div>
      </div>
      <p class="tag-admin__note">停用的专题不能加入新文章；已停用专题的成员与顺序仍然保留。</p>
    </section>

    <p class="tag-admin__note">
      标签与专题都不提供物理删除：已被文章使用的分类一旦删除，历史文章就会指向不存在的分类。
      需要下架时用「停用」，历史绑定保留。
    </p>

    <dialog ref="tagDialog" aria-labelledby="tag-dialog-title" @cancel.prevent="tagDialog?.close()">
      <h2 id="tag-dialog-title">{{ tagForm.id ? '编辑标签' : '新建标签' }}</h2>
      <form class="form-stack" @submit.prevent="submitTag">
        <label>名称<input v-model="tagForm.name" maxlength="100" placeholder="例如：Spring Boot" /></label>
        <div class="dialog-actions">
          <button type="button" @click="tagDialog?.close()">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">{{ tagForm.id ? '保存' : '创建' }}</button>
        </div>
      </form>
    </dialog>

    <dialog ref="topicDialog" aria-labelledby="topic-dialog-title" @cancel.prevent="topicDialog?.close()">
      <h2 id="topic-dialog-title">{{ topicForm.id ? '编辑专题' : '新建专题' }}</h2>
      <form class="form-stack" @submit.prevent="submitTopic">
        <label>名称<input v-model="topicForm.name" maxlength="160" placeholder="例如：Java 学习路线" /></label>
        <div class="dialog-actions">
          <button type="button" @click="topicDialog?.close()">取消</button>
          <button class="primary-button" type="submit" :disabled="saving">{{ topicForm.id ? '保存' : '创建' }}</button>
        </div>
      </form>
    </dialog>
  </section>
</template>
