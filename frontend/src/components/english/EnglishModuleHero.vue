<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    tag: string
    title: string
    subtitle?: string
    description?: string
  }>(),
  { subtitle: '', description: '' },
)
</script>

<template>
  <header class="module-hero">
    <div class="module-hero__text">
      <p class="module-hero__tag">{{ props.tag }}</p>
      <h1 class="module-hero__title">{{ props.title }}</h1>
      <span v-if="props.subtitle" class="module-hero__subtitle">{{ props.subtitle }}</span>
      <span v-if="props.description" class="module-hero__desc">{{ props.description }}</span>
    </div>
    <slot />
  </header>
</template>

<style scoped>
.module-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--space-4);
  padding: 26px 30px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary) 10%, var(--bg-surface)), var(--bg-surface));
  margin-bottom: 28px;
}
.module-hero__text { display: flex; flex-direction: column; }
.module-hero__tag {
  color: var(--accent);
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 0.14em;
  margin: 0;
}
.module-hero__title { font-size: 30px; margin: 7px 0 4px; }
.module-hero__subtitle { color: var(--text-primary); font-size: 15px; }
.module-hero__desc { color: var(--text-secondary); font-size: 13px; margin-top: 6px; }
@media (max-width: 640px) {
  .module-hero { flex-direction: column; align-items: flex-start; }
}
</style>
