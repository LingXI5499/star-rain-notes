/** Presentation only; taxonomy IDs and pagination remain server-owned. */
export function workTechnologies(work) {
 const technologies = (work?.techStack || '').split(/[,，、;；\n]+/).map(value => value.trim()).filter(Boolean)
 return [...new Set(technologies.length ? technologies : (work?.tags || []).map(tag => tag.name))]
}

export function workLinkLabel(link) {
 const labels = { GITHUB: '查看源码', LIVE: '在线体验', DEMO: '在线体验', DOWNLOAD: '下载作品',
  VIDEO: '观看演示', BILIBILI: '观看演示', DOUYIN: '观看演示', DOC: '查看文档', ARTICLE: '查看文档' }
 const label = labels[link.linkType]
 return label ? `${label}${link.label ? ` · ${link.label}` : ''}` : link.label || '相关链接'
}
