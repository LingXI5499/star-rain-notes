/** Presentation only; taxonomy IDs and pagination remain server-owned. */
export function workTechnologies(work) {
 const technologies = (work?.techStack || '').split(/[,，、;；\n]+/).map(value => value.trim()).filter(Boolean)
 return [...new Set(technologies.length ? technologies : (work?.tags || []).map(tag => tag.name))]
}
