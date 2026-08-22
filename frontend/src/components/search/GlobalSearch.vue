<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useGlobalSearch } from '@/composables/useGlobalSearch'
import SearchPanel from './SearchPanel.vue'

/**
 * Global Search UX V2 — persistent search widget for the public header.
 *
 * Desktop (≥1024px): always-visible search input with Ctrl/Cmd+K hint;
 * focus/typing opens a teleported, fixed-position result panel. Below that
 * the widget degrades to a search icon that opens a full-width search sheet
 * (tablet + mobile). All logic lives in useGlobalSearch; this component
 * only wires template refs, ARIA plumbing and layout.
 */
const {
  query,
  status,
  groups,
  activeItem,
  panelOpen,
  statusText,
  sheetOpen,
  isDesktop,
  shortcutLabel,
  desktopInputRef,
  sheetInputRef,
  rootRef,
  panelRef,
  sheetRef,
  panelStyle,
  open,
  close,
  clearQuery,
  retry,
  go,
  viewAll,
  activate,
  onInputKeydown,
  onCompositionStart,
  onCompositionEnd,
} = useGlobalSearch()

const PANEL_ID = 'global-search-panel'

/** Full-width input (with "搜索知识…" + shortcut hint) starts at 1280px. */
const fullDesktop = ref(window.matchMedia('(min-width: 1280px)').matches)
const fullDesktopQuery = window.matchMedia('(min-width: 1280px)')
function onFullDesktopChange(event: MediaQueryListEvent) {
  fullDesktop.value = event.matches
}

const placeholder = computed(() => (fullDesktop.value ? '搜索知识…' : '搜索…'))

const activeOptionId = computed(() => {
  const item = activeItem.value
  return item ? `${PANEL_ID}-${item.type}-${item.id}` : undefined
})

onMounted(() => fullDesktopQuery.addEventListener('change', onFullDesktopChange))
onBeforeUnmount(() => fullDesktopQuery.removeEventListener('change', onFullDesktopChange))
</script>

<template>
  <div ref="rootRef" class="global-search">
    <!-- Desktop: persistent inline search input -->
    <div
      v-if="isDesktop"
      class="global-search__input-wrap"
      :class="{ 'global-search__input-wrap--open': panelOpen }"
    >
      <svg
        class="global-search__icon"
        viewBox="0 0 24 24"
        width="15"
        height="15"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <circle cx="11" cy="11" r="7" />
        <path d="m21 21-4.3-4.3" />
      </svg>
      <input
        ref="desktopInputRef"
        v-model="query"
        class="global-search__input"
        type="text"
        maxlength="100"
        autocomplete="off"
        spellcheck="false"
        enterkeyhint="search"
        :placeholder="placeholder"
        aria-label="全局搜索"
        role="combobox"
        :aria-expanded="panelOpen"
        :aria-controls="PANEL_ID"
        aria-autocomplete="list"
        aria-haspopup="listbox"
        :aria-activedescendant="panelOpen ? activeOptionId : undefined"
        @focus="open()"
        @keydown="onInputKeydown"
        @compositionstart="onCompositionStart"
        @compositionend="onCompositionEnd"
      />
      <button
        v-if="query"
        type="button"
        class="global-search__clear"
        aria-label="清空搜索"
        @click="clearQuery()"
      >
        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>
      <kbd v-if="!query && fullDesktop" class="global-search__kbd">{{ shortcutLabel }}</kbd>
    </div>

    <!-- Tablet / mobile: search icon button -->
    <button
      v-else
      type="button"
      class="global-search__icon-btn"
      aria-label="全局搜索"
      @click="open()"
    >
      <svg
        viewBox="0 0 24 24"
        width="17"
        height="17"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <circle cx="11" cy="11" r="7" />
        <path d="m21 21-4.3-4.3" />
      </svg>
    </button>

    <!-- Visually hidden status announcement (announced only while the panel
         is open; avoids over-announcing on page load, UX V2 §28). -->
    <p v-if="panelOpen || sheetOpen" class="global-search__live" aria-live="polite">
      {{ statusText }}
    </p>

    <!-- Desktop result panel: teleported + fixed so it can never be clipped
         by the sticky header or push page content (UX V2 §10, §43). -->
    <Teleport to="body">
      <Transition name="search-fade">
        <div
          v-if="isDesktop && panelOpen"
          ref="panelRef"
          class="global-search__panel"
          :style="panelStyle"
        >
          <SearchPanel
            :id="PANEL_ID"
            :id-prefix="PANEL_ID"
            :status="status"
            :groups="groups"
            :query="query"
            :status-text="statusText"
            :active-item="activeItem"
            @select="go"
            @hover="activate"
            @retry="retry"
            @view-all="viewAll"
          />
        </div>
      </Transition>
    </Teleport>

    <!-- Tablet / mobile: full-width search sheet -->
    <Teleport to="body">
      <Transition name="search-sheet">
        <div v-if="sheetOpen" ref="sheetRef" class="global-search__sheet">
          <div class="global-search__sheet-backdrop" @pointerdown="close()" />
          <div class="global-search__sheet-panel" role="dialog" aria-modal="true" aria-label="全局搜索">
            <div class="global-search__sheet-bar">
              <button type="button" class="global-search__sheet-back" aria-label="关闭搜索" @click="close()">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="m15 18-6-6 6-6" />
                </svg>
              </button>
              <div class="global-search__sheet-input-wrap">
                <svg
                  class="global-search__icon"
                  viewBox="0 0 24 24"
                  width="15"
                  height="15"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  aria-hidden="true"
                >
                  <circle cx="11" cy="11" r="7" />
                  <path d="m21 21-4.3-4.3" />
                </svg>
                <input
                  ref="sheetInputRef"
                  v-model="query"
                  class="global-search__sheet-input"
                  type="text"
                  maxlength="100"
                  autocomplete="off"
                  spellcheck="false"
                  enterkeyhint="search"
                  placeholder="搜索知识…"
                  aria-label="全局搜索"
                  role="combobox"
                  aria-expanded="true"
                  :aria-controls="PANEL_ID"
                  aria-autocomplete="list"
                  aria-haspopup="listbox"
                  :aria-activedescendant="activeOptionId"
                  @focus="open()"
                  @keydown="onInputKeydown"
                  @compositionstart="onCompositionStart"
                  @compositionend="onCompositionEnd"
                />
                <button
                  v-if="query"
                  type="button"
                  class="global-search__clear"
                  aria-label="清空搜索"
                  @click="clearQuery()"
                >
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                    <path d="M18 6 6 18M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
            <div class="global-search__sheet-body">
              <SearchPanel
                :id="PANEL_ID"
                :id-prefix="PANEL_ID"
                :status="status"
                :groups="groups"
                :query="query"
                :status-text="statusText"
                :active-item="activeItem"
                @select="go"
                @hover="activate"
                @retry="retry"
                @view-all="viewAll"
              />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.global-search {
  position: relative;
  display: flex;
  align-items: center;
  margin-left: auto;
}

.global-search__live {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
  border: 0;
}

/* ---------------------------------------------------------------
   Desktop inline input (persistent, fixed footprint — no layout shift)
   --------------------------------------------------------------- */
.global-search__input-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: var(--search-input-height, 38px);
  width: clamp(280px, 22vw, 340px);
  padding-inline: var(--space-3) var(--space-2);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--bg-subtle);
  color: var(--text-secondary);
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease,
    background-color 150ms ease;
}

.global-search__input-wrap:hover {
  border-color: var(--text-muted);
}

/* Focus: accent ring instead of the browser default blue outline (UX V2 §8). */
.global-search__input-wrap--open,
.global-search__input-wrap:focus-within {
  border-color: var(--primary);
  background: var(--bg-surface);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 16%, transparent);
}

.global-search__icon {
  flex: none;
  color: var(--text-muted);
}

.global-search__input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1;
  outline: none; /* focus style is provided by the wrapper ring above */
}

.global-search__input::placeholder {
  color: var(--text-muted);
}

.global-search__clear {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: var(--bg-subtle);
  color: var(--text-muted);
  cursor: pointer;
}

.global-search__clear:hover {
  color: var(--text-primary);
  background: var(--border);
}

.global-search__kbd {
  flex: none;
  font-size: 11px;
  line-height: 1;
  color: var(--text-muted);
  border: 1px solid var(--border);
  border-bottom-color: var(--border-strong);
  border-radius: 4px;
  padding: 3px 5px;
  background: var(--bg-surface);
  white-space: nowrap;
}

/* Medium desktop: keep the input but shrink it (UX V2 §38). */
@media (max-width: 1279.98px) and (min-width: 1024px) {
  .global-search__input-wrap {
    width: 200px;
  }
}

/* ---------------------------------------------------------------
   Tablet / mobile: search icon button
   --------------------------------------------------------------- */
.global-search__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--search-input-height, 38px);
  height: var(--search-input-height, 38px);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    color 150ms ease,
    border-color 150ms ease;
}

.global-search__icon-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
}

/* ---------------------------------------------------------------
   Desktop panel (teleported, fixed)
   --------------------------------------------------------------- */
.global-search__panel {
  position: fixed;
  z-index: var(--z-search-panel, 60);
  max-height: min(60vh, calc(100vh - var(--header-height) - 24px));
  overflow-y: auto;
  overscroll-behavior: contain;
  background: var(--bg-surface);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  box-shadow: 0 12px 32px rgb(0 0 0 / 0.18);
}

/* ---------------------------------------------------------------
   Tablet / mobile: full-width search sheet
   --------------------------------------------------------------- */
.global-search__sheet {
  position: fixed;
  inset: 0;
  z-index: var(--z-search-sheet, 100);
  display: flex;
  flex-direction: column;
}

.global-search__sheet-backdrop {
  position: absolute;
  inset: 0;
  background: rgb(0 0 0 / 0.45);
}

.global-search__sheet-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
  border-bottom: 1px solid var(--border);
  max-height: 85vh;
}

.global-search__sheet-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--page-padding-x);
  border-bottom: 1px solid var(--border);
}

.global-search__sheet-back {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}

.global-search__sheet-back:hover {
  color: var(--primary);
  background: var(--bg-subtle);
}

.global-search__sheet-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: 40px;
  padding-inline: var(--space-3);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  color: var(--text-secondary);
}

.global-search__sheet-input-wrap:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 16%, transparent);
}

.global-search__sheet-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 16px; /* ≥16px prevents iOS focus zoom */
  outline: none;
}

.global-search__sheet-input::placeholder {
  color: var(--text-muted);
}

.global-search__sheet-body {
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: var(--space-4) var(--page-padding-x);
}

/* ---------------------------------------------------------------
   Entrances — fast, restrained, motion-safe (UX V2 §68)
   --------------------------------------------------------------- */
.search-fade-enter-active,
.search-fade-leave-active {
  transition:
    opacity 150ms ease,
    transform 150ms ease;
}

.search-fade-enter-from,
.search-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.search-sheet-enter-active,
.search-sheet-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.search-sheet-enter-from,
.search-sheet-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
