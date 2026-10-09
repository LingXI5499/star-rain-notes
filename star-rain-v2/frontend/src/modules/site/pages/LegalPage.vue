<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useViewMode } from '../../../shared/viewMode'

const props = defineProps({ kind: { type: String, required: true } })
const { isAccount, contentPath } = useViewMode()
const privacy = computed(() => props.kind === 'privacy')
const title = computed(() => privacy.value ? '隐私政策' : '用户协议')
// 法律说明仅使用静态文案，不读取会话、用户资料或学习记录。
const terms = [
  { title: '适用范围与服务', text: '星雨笔录提供教程、博客、作品展示和留言等服务。公开展示端支持浏览公开内容；账号服务另提供个人学习与内容管理功能。本文说明服务规则，浏览页面本身不表示你同意所有个人信息处理活动。' },
  { title: '内容使用与知识产权', text: '请尊重作者及原始权利人的知识产权。引用内容时注明作者、来源和原文链接；转载、再发布或商业使用应事先取得相应授权。代码、图片和引用资料有单独许可或来源说明的，以该说明为准。' },
  { title: '留言与提交内容', text: '请确保提交内容合法，并拥有必要的使用权。不要提交他人的个人信息、账号凭据或其他敏感资料。留言可能经审核后公开展示；请在提交前检查正文是否包含不希望公开的信息。站点可以对违法、侵权、骚扰或垃圾内容进行处理。' },
  { title: '学习内容与外部链接', text: '教程与博客用于学习和交流，内容可能随技术变化而更新。请结合自己的环境验证代码与操作步骤。访问外部链接后，其服务规则和隐私政策由对应网站提供。' },
  { title: '服务调整与争议处理', text: '维护、升级或故障可能导致暂时无法访问。涉及使用规则或个人信息处理的重要调整，将在相关页面说明。对内容、服务或本协议有疑问，可以通过站点公开联系渠道沟通；本协议不排除法律赋予你的权利。' },
]
const privacySections = [
  { title: '公开访问与必要记录', text: '公开页面不展示真实注册用户的账号资料或个人学习记录。网络请求在服务器及托管环境中可能产生访问与错误日志，包括 IP 地址、请求时间、访问路径及浏览器信息，用于提供服务、安全防护和排查故障。未登录访问不等于完全不产生数据。' },
  { title: '浏览器本地存储与 Cookie', text: '站点使用浏览器本地存储保存主题偏好；公开章节中的部分问题作答会保存在当前浏览器。你可以通过浏览器设置清除这些数据，清除后相应偏好或本地作答可能丢失。账号服务使用会话及安全校验机制；同域名下访问公开页面时，浏览器仍可能携带已有 Cookie，公开展示模式不代表 Cookie 被隔离。' },
  { title: '主动提交的信息', text: '你发送留言或联系站点时，提交的正文及你自愿提供的联系信息用于处理该次请求。不要在公开留言中填写密码、邮箱验证码、身份证号等信息。公开留言与私人的学习记录属于不同使用场景。' },
  { title: '账号服务的数据范围', text: '使用账号服务时，需要处理账号名称、邮箱、密码验证信息及必要的安全记录。个人学习功能还会处理学习计划、阅读进度、知识卡片评价、问题作答和学习历史，用于保存学习状态与安排复习。此处只说明信息类别，不列出任何真实用户资料；账号服务端提供更具体的说明。' },
  { title: '保存与安全', text: '浏览器本地数据保留至你清除数据或相应功能更新。账号与学习数据用于持续提供相应服务；计划完成、暂停或取消不会自动删除学习历史。服务器日志、审计和备份的具体保存与清理安排取决于运行环境，本文不承诺尚未核实的固定天数；可通过公开联系渠道咨询。超出处理目的所需的保存应予清理，依法需要保留的除外。' },
  { title: '服务提供方与用途变化', text: '网站托管、网络传输及邮件验证码发送会涉及相应服务提供方。点击外部链接会进入其他网站，请阅读其政策。新增第三方统计、AI 评价或向其他服务传送作答等功能时，应事先说明提供方、信息范围和用途，并在依法需要时取得相应同意；本政策不将这些未来功能视为已经获得授权。' },
  { title: '你的权利与联系', text: '你可以提出查询、更正、复制、删除个人信息、撤回相关同意或注销账号的请求。页面已有的修改功能可直接使用；尚未提供自助入口的事项，请通过站点公开联系渠道申请，站点需核验身份并说明处理结果。不要在公开留言中提交身份核验材料。法律要求保留的数据及暂无法从备份删除的数据，需另行说明处理限制。' },
  { title: '未成年人及政策更新', text: '不满十四周岁的未成年人请勿自行注册或提交个人信息；涉及此类信息的服务，需要监护人同意及专门保护规则。政策变更将在本页更新版本日期；涉及处理目的、方式或信息种类的重要变化，应另行告知并依法取得必要同意。' },
]
const accountTerms = [
  { title: '账号安全与权限', text: '请使用你有权使用的邮箱注册，妥善保管密码与验证码。账号权限决定可以使用的功能，内容编辑与管理功能仅向具备权限的账号开放。发现异常登录或权限问题时，请联系站点。' },
  { title: '个人学习与计划状态', text: '学习计划、卡片评价和问题作答用于记录学习进度。任务或计划完成后，可以退出进行中列表并保留历史记录；完成、暂停、取消均不等同于删除个人数据。复习安排是学习辅助，不能作为考试结果或能力认证。' },
]
const accountPrivacy = [
  { title: '账号与安全审计', text: '账号名称、邮箱及密码验证信息用于注册、登录、邮箱验证和找回密码。相关安全操作记录包含操作类型、结果、时间及 IP、浏览器信息，用于账号安全和异常追溯；这些记录不在公开政策页面展示。' },
  { title: '学习记录与访问范围', text: '个人学习数据与账号关联，包括所选章节、计划状态、阅读进度、卡片评价、问题作答与答题版本、复习和学习历史。公开展示端不列出这些私人记录。管理员按职责和权限处理必要的账号或运维信息，不因展示端不同而获得任意查看私人数据的授权。' },
]
const sections = computed(() => [
  ...(privacy.value ? privacySections : terms),
  ...(isAccount.value ? (privacy.value ? accountPrivacy : accountTerms) : []),
])
</script>

<template>
  <main class="legal-page">
    <header>
      <p class="public-eyebrow">{{ privacy ? 'PRIVACY POLICY' : 'TERMS OF USE' }}</p>
      <h1>{{ title }}</h1>
      <p class="legal-page__meta">版本 1.0 · 更新于 2026 年 10 月 7 日</p>
      <p>{{ isAccount ? '账号服务端 · 包含公开访问规则及账号、学习服务补充说明。' : '公开展示端 · 说明访问规则与信息处理范围，不展示任何真实注册用户信息。' }}</p>
      <nav aria-label="协议与政策">
        <RouterLink :to="contentPath('/terms')" :aria-current="!privacy ? 'page' : undefined">用户协议</RouterLink>
        <RouterLink :to="contentPath('/privacy')" :aria-current="privacy ? 'page' : undefined">隐私政策</RouterLink>
      </nav>
    </header>
    <div class="legal-page__content">
      <section v-for="(section, index) in sections" :key="section.title">
        <h2>{{ index + 1 }}. {{ section.title }}</h2>
        <p>{{ section.text }}</p>
      </section>
      <section>
        <h2>联系站点</h2>
        <p>本服务由星雨笔录站点作者维护。问题反馈与个人信息处理请求，请使用<RouterLink :to="contentPath('/about#contact')">关于页面的公开联系渠道</RouterLink>。</p>
        <p v-if="privacy">个人信息保护相关规定可查阅<a href="https://www.cac.gov.cn/2021-08/20/c_1631050028355286.htm" target="_blank" rel="noopener noreferrer">《中华人民共和国个人信息保护法》</a>。</p>
      </section>
    </div>
  </main>
</template>

<style scoped>
.legal-page { max-width: 960px; margin: 0 auto; padding: 64px 24px 80px; }
.legal-page header { border-bottom: 1px solid var(--border); padding-bottom: 28px; }
.legal-page h1 { font-size: clamp(32px, 5vw, 52px); margin: 16px 0; }
.legal-page p { color: var(--text-secondary); line-height: 1.9; margin: 14px 0; }
.legal-page__meta { font-size: 13px; }
.legal-page nav { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 24px; }
.legal-page nav a { padding: 10px 18px; border: 1px solid var(--border); border-radius: 999px; }
.legal-page nav a[aria-current="page"] { color: var(--primary); background: var(--surface); border-color: var(--primary); }
.legal-page__content section { margin-top: 32px; }
.legal-page h2 { font-size: 21px; line-height: 1.5; }
.legal-page__content a { color: var(--primary); text-decoration: underline; text-underline-offset: 3px; }
@media (max-width: 600px) { .legal-page { padding: 36px 20px 48px; } }
</style>
