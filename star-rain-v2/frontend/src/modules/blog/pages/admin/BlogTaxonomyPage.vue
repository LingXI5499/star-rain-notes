<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  addTopicPost, createTag, createTopic, disableTag, disableTopic, enableTag, enableTopic,
  listAdminPosts, listAdminTags, listAdminTopics, listTopicMembers, removeTopicPost,
  reorderTopicPosts, updateTag, updateTopic,
} from '../../api/blogApi'
import { errorMessage } from '../../../../shared/http'
import { postStatusLabel, taxonomyStatusLabel } from '../../support/display'

/*
 * 分类与专题管理（BLOG-004 / BLOG-005 / BLOG-006 / BLOG-007）。
 *
 * 这个页面把 Tag 与 Topic 的语义差异摆在明面上：
 * - 左栏 Tag：只有名字与说明，没有任何顺序概念；停用只是不再接受新绑定；
 * - 右栏 Topic：除了名字与说明，还有成员列表与人工顺序，顺序用上下移动调整后整体提交。
 *
 * 两边都只有启用/停用，没有删除：已被文章使用的分类或专题一旦被物理删除，
 * 历史文章就会指向不存在的分类。
 */
const tags = ref([])
const topics = ref([])
const members = ref([])
const postOptions = ref([])
const selectedTopicId = ref(null)
const pendingPostId = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')

const tagForm = reactive({ id: null, slug: '', name: '', description: '' })
const topicForm = reactive({ id: null, slug: '', name: '', description: '' })

const selectedTopic = computed(() => topics.value.find((topic) => topic.id === selectedTopicId.value) || null)
const addablePosts = computed(() => {
  const taken = new Set(members.value.map((member) => member.postId))
  return postOptions.value.filter((post) => !taken.has(post.id))
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

function editTag(tag) {
  Object.assign(tagForm, {
    id: tag.id, slug: tag.slug, name: tag.name, description: tag.description || '',
  })
}

function resetTagForm() {
  Object.assign(tagForm, { id: null, slug: '', name: '', description: '' })
}

async function submitTag() {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const payload = { slug: tagForm.slug, name: tagForm.name, description: tagForm.description }
    if (tagForm.id) {
      await updateTag(tagForm.id, payload)
      notice.value = `标签「${tagForm.name}」已更新。`
    } else {
      await createTag(payload)
      notice.value = `标签「${tagForm.name}」已创建。`
    }
    resetTagForm()
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

function editTopic(topic) {
  Object.assign(topicForm, {
    id: topic.id, slug: topic.slug, name: topic.name, description: topic.description || '',
  })
}

function resetTopicForm() {
  Object.assign(topicForm, { id: null, slug: '', name: '', description: '' })
}

async function submitTopic() {
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const payload = { slug: topicForm.slug, name: topicForm.name, description: topicForm.description }
    if (topicForm.id) {
      await updateTopic(topicForm.id, payload)
      notice.value = `专题「${topicForm.name}」已更新。`
    } else {
      await createTopic(payload)
      notice.value = `专题「${topicForm.name}」已创建。`
    }
    resetTopicForm()
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
  selectedTopicId.value = topic.id
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
  <main class="page-container">
    <div class="page-heading">
      <p class="eyebrow">BLOG TAXONOMY</p>
      <h1>分类与专题</h1>
      <p>
        Tag 是<b>多维分类</b>：一篇文章可以有多个，彼此无序。Topic 是<b>人工策展的有序专题</b>：
        成员有明确顺序，顺序本身也是内容的一部分。两者不互相替代。
      </p>
    </div>

    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading" class="loading" role="status">正在加载分类数据…</p>

    <div class="blog-taxonomy-layout">
      <section class="surface-card">
        <h2 class="section-heading">标签（Tag）</h2>
        <form class="field-grid" @submit.prevent="submitTag">
          <label>slug<input v-model.trim="tagForm.slug" maxlength="100" placeholder="java" /></label>
          <label>名称<input v-model.trim="tagForm.name" maxlength="100" placeholder="Java" /></label>
          <label class="field-grid__wide">说明<input v-model.trim="tagForm.description" maxlength="500" placeholder="可选" /></label>
          <div class="field-grid__wide blog-editor__actions">
            <button class="primary-button" type="submit" :disabled="saving">{{ tagForm.id ? '保存标签' : '新增标签' }}</button>
            <button v-if="tagForm.id" type="button" @click="resetTagForm">取消编辑</button>
          </div>
        </form>

        <div class="table-scroll">
          <table>
            <thead><tr><th>名称</th><th>slug</th><th>状态</th><th>绑定文章</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="tag in tags" :key="tag.id">
                <td><strong>{{ tag.name }}</strong><small>{{ tag.description || '—' }}</small></td>
                <td>{{ tag.slug }}</td>
                <td><span :class="['status-chip', tag.status === 'DISABLED' && 'status-chip--danger']">{{ taxonomyStatusLabel(tag.status) }}</span></td>
                <td>{{ tag.postCount || 0 }}</td>
                <td class="table-actions">
                  <button class="link-button" type="button" @click="editTag(tag)">编辑</button>
                  <button class="link-button" type="button" :disabled="saving" @click="toggleTagStatus(tag)">
                    {{ tag.status === 'DISABLED' ? '启用' : '停用' }}
                  </button>
                </td>
              </tr>
              <tr v-if="!tags.length"><td colspan="5" class="empty-state">还没有标签。</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="surface-card">
        <h2 class="section-heading">专题（Topic）</h2>
        <form class="field-grid" @submit.prevent="submitTopic">
          <label>slug<input v-model.trim="topicForm.slug" maxlength="120" placeholder="java-roadmap" /></label>
          <label>名称<input v-model.trim="topicForm.name" maxlength="160" placeholder="Java 学习路线" /></label>
          <label class="field-grid__wide">说明<input v-model.trim="topicForm.description" maxlength="1000" placeholder="可选（专题名允许重复，slug 唯一）" /></label>
          <div class="field-grid__wide blog-editor__actions">
            <button class="primary-button" type="submit" :disabled="saving">{{ topicForm.id ? '保存专题' : '新增专题' }}</button>
            <button v-if="topicForm.id" type="button" @click="resetTopicForm">取消编辑</button>
          </div>
        </form>

        <div class="table-scroll">
          <table>
            <thead><tr><th>名称</th><th>slug</th><th>状态</th><th>成员</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="topic in topics" :key="topic.id" :class="selectedTopicId === topic.id && 'is-selected'">
                <td><strong>{{ topic.name }}</strong><small>{{ topic.description || '—' }}</small></td>
                <td>{{ topic.slug }}</td>
                <td><span :class="['status-chip', topic.status === 'DISABLED' && 'status-chip--danger']">{{ taxonomyStatusLabel(topic.status) }}</span></td>
                <td>{{ topic.memberCount || 0 }}</td>
                <td class="table-actions">
                  <button class="link-button" type="button" @click="selectTopic(topic)">管理文章</button>
                  <button class="link-button" type="button" @click="editTopic(topic)">编辑</button>
                  <button class="link-button" type="button" :disabled="saving" @click="toggleTopicStatus(topic)">
                    {{ topic.status === 'DISABLED' ? '启用' : '停用' }}
                  </button>
                </td>
              </tr>
              <tr v-if="!topics.length"><td colspan="5" class="empty-state">还没有专题。</td></tr>
            </tbody>
          </table>
        </div>
      </section>

      <aside class="surface-card blog-taxonomy-aside">
        <h2 class="blog-aside__title">专题内文章顺序</h2>
        <p v-if="!selectedTopic" class="muted">先在左侧专题表里点「管理文章」。</p>
        <template v-else>
          <p class="muted">当前专题：<strong>{{ selectedTopic.name }}</strong>（{{ taxonomyStatusLabel(selectedTopic.status) }}）</p>

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

          <div class="blog-editor__actions">
            <button class="primary-button" type="button" :disabled="saving || !members.length" @click="saveOrder">保存顺序</button>
          </div>

          <h3 class="blog-editor__subheading">加入文章</h3>
          <div class="blog-editor__actions">
            <select v-model="pendingPostId">
              <option value="">选择一篇文章</option>
              <option v-for="post in addablePosts" :key="post.id" :value="post.id">
                {{ post.title }}（{{ postStatusLabel(post.status) }}）
              </option>
            </select>
            <button type="button" :disabled="saving || !pendingPostId" @click="addMember">追加到末尾</button>
          </div>
          <p class="muted">停用的专题不能加入新文章；已停用专题的成员与顺序仍然保留。</p>
        </template>
      </aside>
    </div>
  </main>
</template>
