<script setup>
import { ref } from 'vue'

/*
 * 主题主视觉 —— 从 V1 `components/visual/ThemeHero.vue` 移植。
 *
 * 首页、英语首页、关于页三处共用同一套「整块主题插画 + 左侧文字遮罩」的做法：
 * 插画绝对定位铺满区块，左侧用一层由不透明渐变到透明的幕布压住画面，
 * 保证左栏文字在任何一张主题图上都可读，图片加载失败时退回纯色渐变而不是留下破图。
 *
 * V1 用 `withDefaults` + TypeScript 泛型声明 props；V2 是纯 JS，改成运行时声明，语义不变。
 */
const props = defineProps({
  src: { type: String, required: true },
  alt: { type: String, default: '' },
  /* 主题插画的构图焦点：'right' 保留右侧景物，'center' 用于居中式构图。 */
  focus: { type: String, default: 'right' },
})

const failed = ref(false)

function onError() {
  failed.value = true
}
</script>

<template>
  <div
    class="theme-stage"
    :class="{
      'theme-stage--failed': failed,
      'theme-stage--focus-center': props.focus === 'center',
    }"
    aria-hidden="true"
  >
    <img
      v-if="!failed"
      class="theme-stage__image"
      :src="props.src"
      alt=""
      decoding="async"
      fetchpriority="high"
      @error="onError"
    />
    <div v-else class="theme-stage__fallback" />
    <!-- 幕布压掉插画里可能自带的文字，避免与编辑性标题抢读 -->
    <div class="theme-stage__veil" />
    <div class="theme-stage__fog" />
  </div>
</template>

<style scoped>
.theme-stage {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background: var(--bg-page);
}

.theme-stage__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 78% center;
}

.theme-stage--focus-center .theme-stage__image {
  object-position: 62% center;
}

.theme-stage__fallback {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, color-mix(in srgb, var(--primary) 14%, var(--bg-page)), var(--bg-page));
}

.theme-stage__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    var(--bg-page) 0%,
    var(--bg-page) 34%,
    color-mix(in srgb, var(--bg-page) 94%, transparent) 46%,
    color-mix(in srgb, var(--bg-page) 55%, transparent) 58%,
    color-mix(in srgb, var(--bg-page) 18%, transparent) 70%,
    transparent 82%
  );
}

.theme-stage__fog {
  position: absolute;
  inset: auto 0 0;
  height: 32%;
  background: linear-gradient(180deg, transparent, var(--bg-page));
  opacity: 0.98;
}

[data-theme='dark'] .theme-stage__veil {
  background: linear-gradient(
    90deg,
    var(--bg-page) 0%,
    var(--bg-page) 36%,
    color-mix(in srgb, var(--bg-page) 90%, transparent) 48%,
    color-mix(in srgb, #000 45%, transparent) 62%,
    color-mix(in srgb, #000 12%, transparent) 76%,
    transparent 88%
  );
}

[data-theme='dark'] .theme-stage__image {
  filter: brightness(0.74) saturate(0.9);
}

@media (max-width: 720px) {
  .theme-stage__image {
    object-position: 70% 30%;
    opacity: 0.42;
  }

  .theme-stage__veil {
    background: linear-gradient(
      180deg,
      color-mix(in srgb, var(--bg-page) 70%, transparent) 0%,
      color-mix(in srgb, var(--bg-page) 88%, transparent) 40%,
      var(--bg-page) 100%
    );
  }
}
</style>
