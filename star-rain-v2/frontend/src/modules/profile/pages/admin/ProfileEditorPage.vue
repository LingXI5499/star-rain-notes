<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import MarkdownEditor from '../../../../shared/editor/MarkdownEditor.vue'
import MediaPicker from '../../../media/components/MediaPicker.vue'
import { useMediaPicker } from '../../../media/support/useMediaPicker'
import { listPublicTutorials } from '../../../tutorial/api/tutorialApi'
import { listPublicPosts } from '../../../blog/api/blogApi'
import { listPublicWorks } from '../../../portfolio/api/portfolioApi'
import { errorMessage } from '../../../../shared/http'
import { accountPath } from '../../../../shared/viewMode'
import {
  addFeatured, clearProfileMedia, getAdminProfile, orderExperiences, orderFeatured,
  orderSkills, orderSocial, removeExperience, removeFeatured, removeSkill, removeSocial,
  saveExperience, saveSkill, saveSocial, setProfileMedia, updateProfile,
} from '../../api/profileApi'

const profile = ref(null)
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const editorKey = ref(0)
const { pickerOpen, pickerType, pick, settle } = useMediaPicker()
const basic = reactive({ displayName: '', headline: '', bioMarkdown: '', locationText: '' })
const experience = reactive({ id: '', experienceType: 'EDUCATION', title: '', organization: '', startDate: '', endDate: '', isCurrent: false, descriptionMd: '' })
const skill = reactive({ id: '', category: 'SKILL', name: '', description: '', proficiency: '' })
const social = reactive({ id: '', platformCode: 'GITHUB', label: '', url: '' })
const feature = reactive({ contentType: 'TUTORIAL', contentId: '', titleOverride: '' })
const candidates = reactive({ TUTORIAL: [], BLOG: [], PORTFOLIO: [] })
const availableCandidates = computed(() => candidates[feature.contentType] || [])

async function fetchAll(api) {
  const first = await api({ page: 1, pageSize: 100 })
  const items = [...(first.items || [])]
  for (let page = 2; page <= Math.ceil((first.total || 0) / 100); page += 1) {
    const next = await api({ page, pageSize: 100 })
    items.push(...(next.items || []))
  }
  return items.map((item) => ({ id: String(item.id), title: item.title }))
}

async function load() {
  loading.value = true
  try {
    profile.value = await getAdminProfile()
    basic.displayName = profile.value.displayName || ''
    basic.headline = profile.value.headline || ''
    basic.bioMarkdown = profile.value.bioMarkdown || ''
    basic.locationText = profile.value.locationText || ''
    editorKey.value += 1
    const [tutorials, blogs, works] = await Promise.all([
      fetchAll(listPublicTutorials), fetchAll(listPublicPosts), fetchAll(listPublicWorks),
    ])
    candidates.TUTORIAL = tutorials
    candidates.BLOG = blogs
    candidates.PORTFOLIO = works
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function run(action, success = '已保存') {
  if (busy.value) return false
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    profile.value = await action()
    notice.value = success
    return true
  } catch (cause) {
    error.value = errorMessage(cause)
    return false
  } finally {
    busy.value = false
  }
}

function resetExperience() { Object.assign(experience, { id: '', experienceType: 'EDUCATION', title: '', organization: '', startDate: '', endDate: '', isCurrent: false, descriptionMd: '' }) }
function editExperience(item) { Object.assign(experience, { ...item, startDate: item.startDate || '', endDate: item.endDate || '', descriptionMd: item.descriptionMd || '' }); document.getElementById('profile-experience-form')?.scrollIntoView({ behavior: 'smooth' }) }
async function submitExperience() {
  const payload = { ...experience, startDate: experience.startDate || null, endDate: experience.isCurrent ? null : experience.endDate || null }
  delete payload.id
  if (await run(() => saveExperience(experience.id, payload))) resetExperience()
}
async function deleteExperience(id) { if (window.confirm('删除这条经历？')) await run(() => removeExperience(id), '经历已删除') }

function resetSkill() { Object.assign(skill, { id: '', category: 'SKILL', name: '', description: '', proficiency: '' }) }
function editSkill(item) { Object.assign(skill, item); document.getElementById('profile-skill-form')?.scrollIntoView({ behavior: 'smooth' }) }
async function submitSkill() { const payload = { ...skill }; delete payload.id; if (await run(() => saveSkill(skill.id, payload))) resetSkill() }
async function deleteSkill(id) { if (window.confirm('删除这个条目？')) await run(() => removeSkill(id), '条目已删除') }

function resetSocial() { Object.assign(social, { id: '', platformCode: 'GITHUB', label: '', url: '' }) }
function editSocial(item) { Object.assign(social, item); document.getElementById('profile-social-form')?.scrollIntoView({ behavior: 'smooth' }) }
async function submitSocial() { const payload = { ...social }; delete payload.id; if (await run(() => saveSocial(social.id, payload))) resetSocial() }
async function deleteSocial(id) { if (window.confirm('删除这个链接？')) await run(() => removeSocial(id), '链接已删除') }

async function chooseMedia(kind) {
  const asset = await pick(kind === 'avatar' ? 'IMAGE' : 'DOCUMENT')
  if (asset) await run(() => setProfileMedia(kind, asset.id), kind === 'avatar' ? '头像已更新' : '简历已更新')
}
async function clearMedia(kind) { await run(() => clearProfileMedia(kind), '媒体引用已移除') }

async function submitFeatured() {
  if (!feature.contentId) return
  if (await run(() => addFeatured({ ...feature }), '精选内容已添加')) {
    feature.contentId = ''
    feature.titleOverride = ''
  }
}
async function deleteFeatured(id) { if (window.confirm('移除这条精选内容？')) await run(() => removeFeatured(id), '精选已移除') }

async function move(section, index, delta) {
  const items = profile.value?.[section] || []
  const target = index + delta
  if (target < 0 || target >= items.length) return
  const ids = items.map((item) => item.id)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  const save = { experiences: orderExperiences, skills: orderSkills, socialLinks: orderSocial, featuredContents: orderFeatured }[section]
  await run(() => save(ids), '顺序已更新')
}

onMounted(load)
</script>

<template>
  <main class="profile-editor">
    <header class="profile-editor__header"><p>AUTHOR PROFILE · 内容编辑</p><h1>作者资料</h1><span>管理公开介绍、经历、技能、社交链接与精选内容。</span><RouterLink :to="accountPath('/about')">查看公开页 ↗</RouterLink></header>
    <p v-if="loading" class="profile-editor__state">正在加载作者资料…</p>
    <template v-else-if="profile">
      <p v-if="error" class="profile-editor__error" role="alert">{{ error }}</p>
      <p v-if="notice" class="profile-editor__notice" role="status">{{ notice }}</p>

      <section class="profile-editor__panel"><h2>基础介绍</h2>
        <form @submit.prevent="run(() => updateProfile({ ...basic }))">
          <div class="profile-editor__grid"><label>显示名称<input v-model.trim="basic.displayName" required maxlength="100" /></label><label>一句话介绍<input v-model.trim="basic.headline" maxlength="255" /></label><label>所在地<input v-model.trim="basic.locationText" maxlength="120" /></label></div>
          <label>个人介绍</label><MarkdownEditor :key="editorKey" v-model="basic.bioMarkdown" /><button type="submit" :disabled="busy">保存基础介绍</button>
        </form>
      </section>

      <section class="profile-editor__panel"><h2>头像与简历</h2><p class="profile-editor__hint">从媒体库选择公开资源。头像需为图片，简历需为 PDF。</p>
        <div class="profile-editor__media"><div><img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="当前头像" /><p v-else>尚未设置头像</p><button type="button" :disabled="busy" @click="chooseMedia('avatar')">选择头像</button><button v-if="profile.avatarMediaAssetId" type="button" :disabled="busy" @click="clearMedia('avatar')">移除</button></div>
          <div><a v-if="profile.resumeUrl" :href="profile.resumeUrl" target="_blank" rel="noopener noreferrer">查看当前简历 ↗</a><p v-else>尚未设置公开简历</p><button type="button" :disabled="busy" @click="chooseMedia('resume')">选择 PDF 简历</button><button v-if="profile.resumeMediaAssetId" type="button" :disabled="busy" @click="clearMedia('resume')">移除</button></div></div>
      </section>

      <section class="profile-editor__panel"><h2>教育与经历</h2>
        <ol class="profile-editor__items"><li v-for="(item, index) in profile.experiences" :key="item.id"><div><strong>{{ item.title }}</strong><small>{{ item.experienceType }} · {{ item.organization || '—' }}</small></div><div class="profile-editor__actions"><button type="button" :disabled="index === 0 || busy" @click="move('experiences', index, -1)">↑</button><button type="button" :disabled="index === profile.experiences.length - 1 || busy" @click="move('experiences', index, 1)">↓</button><button type="button" @click="editExperience(item)">编辑</button><button type="button" @click="deleteExperience(item.id)">删除</button></div></li></ol>
        <form id="profile-experience-form" @submit.prevent="submitExperience"><h3>{{ experience.id ? '编辑经历' : '添加经历' }}</h3><div class="profile-editor__grid"><label>类型<select v-model="experience.experienceType"><option v-for="type in ['EDUCATION','PROJECT','CAREER','GROWTH','OTHER']" :key="type">{{ type }}</option></select></label><label>标题<input v-model.trim="experience.title" required maxlength="255" /></label><label>机构 / 场景<input v-model.trim="experience.organization" maxlength="255" /></label><label>开始日期<input v-model="experience.startDate" type="date" @click="$event.target.showPicker?.()" /></label><label>结束日期<input v-model="experience.endDate" type="date" :disabled="experience.isCurrent" @click="$event.target.showPicker?.()" /></label><label class="profile-editor__checkbox"><input v-model="experience.isCurrent" type="checkbox" />进行中</label></div><label>经历说明<textarea v-model="experience.descriptionMd" rows="4" /></label><button type="submit" :disabled="busy">{{ experience.id ? '保存经历' : '添加经历' }}</button><button v-if="experience.id" type="button" @click="resetExperience">取消编辑</button></form>
      </section>

      <section class="profile-editor__panel"><h2>技能、方向与兴趣</h2>
        <ol class="profile-editor__items"><li v-for="(item, index) in profile.skills" :key="item.id"><div><strong>{{ item.name }}</strong><small>{{ item.category }} · {{ item.proficiency || '未标注' }}</small></div><div class="profile-editor__actions"><button type="button" :disabled="index === 0 || busy" @click="move('skills', index, -1)">↑</button><button type="button" :disabled="index === profile.skills.length - 1 || busy" @click="move('skills', index, 1)">↓</button><button type="button" @click="editSkill(item)">编辑</button><button type="button" @click="deleteSkill(item.id)">删除</button></div></li></ol>
        <form id="profile-skill-form" @submit.prevent="submitSkill"><h3>{{ skill.id ? '编辑条目' : '添加条目' }}</h3><div class="profile-editor__grid"><label>类别<select v-model="skill.category"><option value="SKILL">技能</option><option value="DIRECTION">方向</option><option value="INTEREST">兴趣</option></select></label><label>名称<input v-model.trim="skill.name" required maxlength="120" /></label><label>熟悉程度（展示文字）<input v-model.trim="skill.proficiency" maxlength="30" /></label></div><label>说明<input v-model.trim="skill.description" maxlength="500" /></label><button type="submit" :disabled="busy">{{ skill.id ? '保存条目' : '添加条目' }}</button><button v-if="skill.id" type="button" @click="resetSkill">取消编辑</button></form>
      </section>

      <section class="profile-editor__panel"><h2>社交链接</h2>
        <ol class="profile-editor__items"><li v-for="(item, index) in profile.socialLinks" :key="item.id"><div><strong>{{ item.label }}</strong><small>{{ item.platformCode }} · {{ item.url }}</small></div><div class="profile-editor__actions"><button type="button" :disabled="index === 0 || busy" @click="move('socialLinks', index, -1)">↑</button><button type="button" :disabled="index === profile.socialLinks.length - 1 || busy" @click="move('socialLinks', index, 1)">↓</button><button type="button" @click="editSocial(item)">编辑</button><button type="button" @click="deleteSocial(item.id)">删除</button></div></li></ol>
        <form id="profile-social-form" @submit.prevent="submitSocial"><h3>{{ social.id ? '编辑链接' : '添加链接' }}</h3><div class="profile-editor__grid"><label>平台<select v-model="social.platformCode"><option v-for="platform in ['GITHUB','BILIBILI','GITEE','LINKEDIN','EMAIL_PAGE','OTHER']" :key="platform">{{ platform }}</option></select></label><label>显示名称<input v-model.trim="social.label" required maxlength="100" /></label><label>链接地址<input v-model.trim="social.url" type="url" required maxlength="1000" placeholder="https://" /></label></div><button type="submit" :disabled="busy">{{ social.id ? '保存链接' : '添加链接' }}</button><button v-if="social.id" type="button" @click="resetSocial">取消编辑</button></form>
      </section>

      <section class="profile-editor__panel"><h2>精选内容</h2><p class="profile-editor__hint">只能选择已公开内容；撤回后会自动从公开页隐藏，并在此提示。</p>
        <ol class="profile-editor__items"><li v-for="(item, index) in profile.featuredContents" :key="item.id"><div><strong>{{ item.title }}</strong><small>{{ item.contentType }} · {{ item.available ? '公开可用' : '引用对象不可公开' }}</small></div><div class="profile-editor__actions"><button type="button" :disabled="index === 0 || busy" @click="move('featuredContents', index, -1)">↑</button><button type="button" :disabled="index === profile.featuredContents.length - 1 || busy" @click="move('featuredContents', index, 1)">↓</button><button type="button" @click="deleteFeatured(item.id)">移除</button></div></li></ol>
        <form @submit.prevent="submitFeatured"><h3>添加精选</h3><div class="profile-editor__grid"><label>类型<select v-model="feature.contentType" @change="feature.contentId = ''"><option value="TUTORIAL">教程</option><option value="BLOG">博客</option><option value="PORTFOLIO">作品</option></select></label><label>已公开内容<select v-model="feature.contentId" required><option value="">请选择</option><option v-for="item in availableCandidates" :key="item.id" :value="item.id">{{ item.title }}</option></select></label><label>展示标题（选填）<input v-model.trim="feature.titleOverride" maxlength="255" /></label></div><button type="submit" :disabled="busy || !feature.contentId">添加精选</button></form>
      </section>
    </template>
    <p v-else-if="error" class="profile-editor__error" role="alert">{{ error }}</p>
    <MediaPicker :open="pickerOpen" :media-type="pickerType" @update:open="settle(null)" @select="settle" />
  </main>
</template>

<style scoped>
.profile-editor { max-width: 1180px; margin: auto; padding: 36px 36px 90px; }
.profile-editor__header { margin-bottom: 32px; }
.profile-editor__header p { color: var(--accent); font-size: 11px; font-weight: 700; letter-spacing: .14em; }
.profile-editor__header h1 { color: var(--text-primary); font-size: 36px; margin: 10px 0; }
.profile-editor__header span { color: var(--text-secondary); }
.profile-editor__header a { display: inline-block; margin-left: 20px; color: var(--primary); }
.profile-editor__state { padding: 60px; text-align: center; color: var(--text-muted); }
.profile-editor__error { color: #bd4e39; margin-bottom: 16px; }.profile-editor__notice { color: var(--primary); margin-bottom: 16px; }
.profile-editor__panel { border: 1px solid var(--border); border-radius: 18px; padding: 27px; background: var(--bg-surface); margin-bottom: 22px; }
.profile-editor__panel h2 { color: var(--text-primary); font-size: 24px; margin-bottom: 18px; }
.profile-editor__panel h3 { font-size: 17px; color: var(--text-primary); margin: 22px 0 12px; }
.profile-editor__hint { color: var(--text-muted); font-size: 13px; margin: -5px 0 18px; }
.profile-editor__grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; }
.profile-editor label { display: block; color: var(--text-secondary); font-size: 13px; font-weight: 600; margin: 12px 0; }
.profile-editor input:not([type=checkbox]), .profile-editor select, .profile-editor textarea { display: block; width: 100%; box-sizing: border-box; margin-top: 7px; border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; background: var(--bg-page); color: var(--text-primary); font: inherit; }
.profile-editor textarea { resize: vertical; }.profile-editor input:focus, .profile-editor select:focus, .profile-editor textarea:focus { outline: 2px solid var(--primary); outline-offset: 2px; }
.profile-editor button { border: 1px solid var(--border); border-radius: 8px; padding: 9px 14px; margin: 8px 8px 0 0; background: var(--bg-page); color: var(--text-primary); cursor: pointer; }
.profile-editor button:hover { color: var(--primary); border-color: var(--primary); }.profile-editor button:disabled { opacity: .45; cursor: default; }
.profile-editor__checkbox { display: flex !important; align-items: center; gap: 8px; align-self: end; }
.profile-editor__items { list-style: none; padding: 0; margin: 0; }.profile-editor__items li { display: flex; justify-content: space-between; align-items: center; gap: 18px; border-top: 1px solid var(--border); padding: 12px 0; }
.profile-editor__items strong { display: block; color: var(--text-primary); }.profile-editor__items small { display: block; color: var(--text-muted); margin-top: 4px; overflow-wrap: anywhere; }
.profile-editor__actions { display: flex; flex-wrap: wrap; justify-content: flex-end; }.profile-editor__actions button { margin: 0 0 4px 6px; }
.profile-editor__media { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }.profile-editor__media > div { border: 1px dashed var(--border); padding: 18px; border-radius: 10px; }.profile-editor__media img { max-width: 120px; aspect-ratio: 1; object-fit: cover; display: block; margin-bottom: 10px; }.profile-editor__media p { color: var(--text-muted); }.profile-editor__media a { color: var(--primary); }
@media(max-width: 700px) { .profile-editor { padding-inline: 16px; }.profile-editor__panel { padding: 18px; }.profile-editor__media { grid-template-columns: 1fr; }.profile-editor__items li { align-items: flex-start; flex-direction: column; } }
</style>
