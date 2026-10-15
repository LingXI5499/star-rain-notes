/** Manually authored acceptance records based on ai-coding-portfolio READMEs, 2026-10-15.
 * These live only in intercepted browser services and never create production records.
 */
const repository = 'https://github.com/LingXI5499/ai-coding-portfolio/tree/main/'
export const portfolioCases = [
 { slug: 'atlasflow', title: 'AtlasFlow · 全球地图相册', techStack: 'Vue, TypeScript, MapLibre, MediaPipe',
  summary: '拖动三维地球，选择经纬度，浏览附近的 Wikimedia Commons 公开照片，并进入动态照片墙。',
  bodyMarkdown: '## 从地图到照片\n\n作品把三维地球、真实地理照片和照片墙串联起来，支持来源查看、高清预览和分页加载。\n\n## 手势交互\n\n摄像头手势可以让照片球聚合或散开，需要用户主动授权。地图与照片依赖外网，照片保留作者、许可与来源署名。' },
 { slug: 'frontend-lab', title: 'Frontend Lab · 界面实验室', techStack: 'HTML, CSS, JavaScript',
  summary: '包含 263 个可运行实验、技术速查和开发手册的前端交互学习工作台。',
  bodyMarkdown: '## 学习工作台\n\n支持源码编辑、隔离预览、控制台和响应式宽度调整。\n\n## 长文阅读与开发手册\n\n' +
   '实验和开发手册共同组织学习内容，草稿与进度保存在浏览器中。独立 HTML 导出便于继续练习。\n\n'.repeat(55) + '## 本地运行\n\n```powershell\nnpm ci\nnpm run dev\n```' },
 { slug: 'java-web-lab', title: 'Java Web Lab · Java 全栈链路与业务实验室', techStack: 'JavaScript, Three.js, SQLite WASM',
  summary: '从 HTTP 请求到高并发架构的交互教学网站，包含 46 个业务 SQL 实验和六阶段架构演示。',
  bodyMarkdown: '## 交互教学\n\n代码阅读器支持高亮与节点联动，SQL 在浏览器内执行。\n\n## SQL 练习\n\n```sql\nSELECT * FROM products WHERE stock > 0;\n```\n\n> 这里的 SQL 是验收排版示例。Java 为只读讲解片段，并发与库存等内容是教学模型。' },
 { slug: 'resume-studio', title: 'Resume Studio · 个人简历工作室', techStack: 'Vue, TypeScript, Express, Playwright',
  summary: '支持 A4 实时预览、岗位版本、IndexedDB 自动保存、JSON 备份和中文文本 PDF 导出的本地简历编辑器。',
  bodyMarkdown: '## 编辑与预览\n\n在一个工作台中管理岗位版本，并实时检查 A4 排版。\n\n## 备份与导出\n\n通过 JSON 备份资料，PDF 导出依赖本地服务、Chromium 和中文字体。集合中的简历资料为虚构演示内容，头像是中性占位图。' },
 { slug: 'asterway', title: 'Asterway · 星行纪', techStack: 'Vue, TypeScript, Pinia, Zod', projectStatus: 'DEVELOPING',
  summary: '覆盖目标、任务、计划、习惯、专注、复盘、知识库和成长社区的个人成长产品前端原型。',
  bodyMarkdown: '## 产品原型\n\n18 个页面串联目标、计划、专注和复盘。业务数据保存在当前浏览器，成长伙伴采用本地规则。\n\n## 阅读界面\n\n![星行纪阅读界面](/portfolio-test-assets/asterway-reading.png)\n\n> 社区、消息、会员与账号是演示，没有真实后端或支付。' },
 { slug: 'musiclingxi', title: 'MusicLingXi · Android 离线音乐播放器', techStack: 'JavaScript, Capacitor, Android',
  summary: '支持搜索、播放队列、同步歌词，以及 Android 后台、通知与锁屏控制的离线音乐播放器。',
  bodyMarkdown: '## 播放体验\n\n支持列表、单曲与随机模式，LRC 同步歌词和点击歌词跳转，明暗主题与状态记忆。\n\n## Android\n\nCapacitor 8 与 Android 原生媒体会话提供后台与锁屏能力，需要真机验证。集合只提供空音频占位文件，没有真实歌曲或歌词。' },
 { slug: 'ai-intelligence-daily', title: 'AI Intelligence Daily · AI 情报日报', techStack: 'Python, FastAPI, SQLite, httpx',
  summary: '聚合技术、AI、社会热点、政策和秋招公开信息，提供去重、刷新进度和日报摘要。',
  bodyMarkdown: '## 信息聚合\n\n服务运行期间，每天北京时间 08:00 刷新，也支持手动刷新，共享任务锁。\n\n## 可选摘要\n\nDeepSeek Key 可选，未配置时使用本地规则摘要。采集失败保留最近数据。外部数据源的可用性需要联网验证。\n\n```powershell\npython main.py\n```' },
 { slug: 'starfall-ruins', title: 'Starfall Ruins · 星陨遗迹', techStack: 'Unity, C#, Blender', projectStatus: 'DEVELOPING',
  summary: '以移动、跳跃、碎片收集、祭坛激活和传送门为核心流程的 Unity 3D 玩法原型。',
  bodyMarkdown: '## 玩法流程\n\n使用 WASD 移动、空格跳跃和鼠标视角，收集星碎片，激活祭坛并解锁传送门。\n\n## 创作资源\n\n项目保留玩法脚本、场景、关卡构建工具以及 Blender 岩石源文件。游戏运行效果需在 Unity 中继续验收。' },
].map((item, index) => ({ id: String(index + 1), rowVersion: 0, workType: 'SOFTWARE', categoryId: '1', formatId: '2',
 category: { id: '1', name: '工具' }, format: { id: '2', name: 'Web 应用' }, tags: [], media: [],
 hasSections: false, sections: [], status: 'DRAFT', featured: index < 3, projectStatus: 'COMPLETED',
 coverUrl: null, ...item,
 links: [{ id: `source-${index}`, linkType: 'GITHUB', label: item.slug, url: repository + item.slug, enabled: true, sortOrder: 0 }] }))
