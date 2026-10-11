/**
 * workBlocks.js：作品模块的业务支持工具。
 * 职责范围：作品分类、展示区块、代码库链接及关联媒体。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 阅读入口：safeWorkUrl（安全作品地址的处理函数）；blockPayload（区块提交内容的处理函数）；emptyBlock（空白区块的处理函数）。
 */

export const blockTypes = { MARKDOWN: '正文', IMAGE: '图片', GALLERY: '画廊', AUDIO: '音频', CODE: '代码', TIMELINE: '时间线', LINKS: '链接组', QUOTE: '引用', FEATURE_LIST: '功能列表', TECH_STACK: '技术栈', STATS: '成果数据', CUSTOM: '自定义正文' }
export const itemFields = { TIMELINE: { date: '日期', title: '事件', description: '说明' }, LINKS: { label: '名称', url: '地址', description: '说明' }, FEATURE_LIST: { title: '功能', description: '说明' }, TECH_STACK: { name: '技术', group: '分组' }, STATS: { label: '指标', value: '数值' } }
export const stageLabels = { IDEA: '构想中', PLANNING: '规划中', DEVELOPING: '开发中', COMPLETED: '已完成', MAINTAINING: '维护中', ARCHIVED: '已归档', ONLINE: '已上线' }
export const linkTypes = { LIVE: '在线体验', GITHUB: 'GitHub', DOWNLOAD: '下载', VIDEO: '视频', DOC: '文档', OTHER: '其他', BILIBILI: '哔哩哔哩', DOUYIN: '抖音', DEMO: '演示', ARTICLE: '文章' }
export
  /**
   * 安全作品地址的处理函数。
   * 主要协作：.includes。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function safeWorkUrl(value) { try { const url = new URL(value); return ['http:', 'https:'].includes(url.protocol) && !url.username && !url.password } catch { return false } }
export
  /**
   * 区块提交内容的处理函数。
   * 主要协作：.map。
   * @param block 区块；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function blockPayload(block) { return { sectionType: block.sectionType, title: block.title || '', content: block.content || '', data: block.data || {}, visible: block.visible !== false, blockVersion: 1, media: (block.media || []).map(item => ({ mediaAssetId: item.mediaAssetId, caption: item.caption || '' })) } }
export
  /**
   * 空白区块的处理函数。
   * @param type = 'MARKDOWN' 类型；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function emptyBlock(type = 'MARKDOWN') { return { sectionType: type, title: '', content: '', data: itemFields[type] ? { items: [] } : {}, visible: true, media: [] } }
