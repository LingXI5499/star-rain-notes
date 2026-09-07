/**
 * Map free-form portfolio techStack tags into architecture layers
 * for Case Study presentation (Backend / Frontend / Data / Infrastructure).
 */

export type TechStackGroup = {
  label: string
  items: string[]
}

const RULES: Array<{ label: string; match: RegExp }> = [
  { label: 'Backend', match: /java|spring|mybatis|hibernate|kotlin|go\b|node|express|nestjs|django|flask|fastapi|quarkus|micronaut/i },
  { label: 'Frontend', match: /vue|react|angular|svelte|typescript|javascript|vite|webpack|pinia|redux|nuxt|next\.?js|css|html|tailwind/i },
  { label: 'Data', match: /mysql|postgres|mariadb|mongo|redis|elasticsearch|flyway|liquibase|kafka|rabbit|sqlite/i },
  { label: 'Infrastructure', match: /nginx|docker|kubernetes|k8s|linux|aliyun|aws|azure|ecs|cdn|oss|ci|github actions|gitlab/i },
]

const ORDER = ['Backend', 'Frontend', 'Data', 'Infrastructure', 'Other'] as const

export function categorizeTechStack(raw: string[] | null | undefined): TechStackGroup[] {
  const buckets = new Map<string, string[]>()
  for (const label of ORDER) buckets.set(label, [])

  for (const item of raw ?? []) {
    const trimmed = item?.trim()
    if (!trimmed) continue
    const rule = RULES.find((entry) => entry.match.test(trimmed))
    buckets.get(rule?.label ?? 'Other')!.push(trimmed)
  }

  return ORDER
    .map((label) => ({ label, items: buckets.get(label) ?? [] }))
    .filter((group) => group.items.length > 0)
}
