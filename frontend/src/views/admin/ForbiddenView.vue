<template>
  <section class="forbidden-view">
    <p>403</p>
    <h1>没有权限访问此页面</h1>
    <span>当前账号没有执行该后台操作的权限。</span>
    <RouterLink to="/admin">返回仪表盘</RouterLink>
  </section>
</template>

<style scoped>
.forbidden-view { max-width: 520px; padding: 72px 0; }
.forbidden-view p { color: var(--accent); font-size: 12px; font-weight: 800; letter-spacing: .18em; }
.forbidden-view h1 { margin: 8px 0; font-size: 32px; }
.forbidden-view span { display: block; margin-bottom: 20px; color: var(--text-secondary); }
.forbidden-view a { color: var(--primary); font-weight: 650; }
</style>
