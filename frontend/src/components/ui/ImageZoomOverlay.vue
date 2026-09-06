<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

/**
 * 作品详情原图放大遮罩：可滚动查看完整截图。
 * 支持 Escape、关闭按钮、焦点恢复与背景滚动锁定，手机端完整适配。
 */
const props = defineProps<{ open: boolean; src: string; alt: string }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const closeButton = ref<HTMLButtonElement | null>(null)
let previouslyFocused: HTMLElement | null = null
let previousOverflow = ''

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close')
}

watch(
  () => props.open,
  async (open) => {
    if (open) {
      previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null
      previousOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      document.addEventListener('keydown', handleKeydown)
      await nextTick()
      closeButton.value?.focus()
    } else {
      document.removeEventListener('keydown', handleKeydown)
      document.body.style.overflow = previousOverflow
      previouslyFocused?.focus()
      previouslyFocused = null
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = previousOverflow
})
</script>

<template>
  <Teleport to="body">
    <Transition name="zoom-fade">
      <div v-if="open" class="image-zoom" role="dialog" aria-modal="true" :aria-label="`放大查看：${alt}`" @click.self="emit('close')">
        <button ref="closeButton" type="button" class="image-zoom__close" aria-label="关闭放大视图" @click="emit('close')">
          ×
        </button>
        <div class="image-zoom__scroll">
          <img :src="src" :alt="alt" />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.image-zoom {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px 24px;
  background: rgb(8 12 11 / 0.86);
  backdrop-filter: blur(6px);
}

.image-zoom__close {
  position: absolute;
  top: max(12px, env(safe-area-inset-top));
  right: max(14px, env(safe-area-inset-right));
  z-index: 1;
  width: 42px;
  height: 42px;
  border: 1px solid rgb(255 255 255 / 0.28);
  border-radius: 50%;
  color: #fff;
  background: rgb(255 255 255 / 0.08);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.image-zoom__close:hover {
  background: rgb(255 255 255 / 0.18);
}

.image-zoom__scroll {
  max-width: 100%;
  max-height: 100%;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.image-zoom__scroll img {
  display: block;
  max-width: none;
  max-height: none;
  width: auto;
  height: auto;
}

@media (max-width: 640px) {
  .image-zoom {
    padding: 56px 10px 10px;
  }

  .image-zoom__scroll {
    display: block;
  }

  .image-zoom__scroll img {
    width: 100%;
    height: auto;
  }
}

.zoom-fade-enter-active,
.zoom-fade-leave-active {
  transition: opacity 0.18s ease;
}

.zoom-fade-enter-from,
.zoom-fade-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .zoom-fade-enter-active,
  .zoom-fade-leave-active {
    transition: none;
  }
}
</style>
