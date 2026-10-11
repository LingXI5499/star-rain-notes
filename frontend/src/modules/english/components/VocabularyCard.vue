<script setup>
/**
 * VocabularyCard.vue：英语学习模块的视图组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../lib/vocabularyDisplay、../lib/vocabularyPronunciation、../api/vocabularyLearningApi。
 * 组件输入：word、memory、settings、busy、selectable、selected；默认值与类型见 defineProps。
 * 组件输出事件：start、display、reset；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、speaking、active、mode、visibility、showEnglish、showChinese。
 * 阅读入口：pronounce（pronounce的处理函数）；stop（停止的处理函数）；formatTime（格式化时间）。
 */


import { computed, ref } from 'vue'
import { resolveVocabularyVisibility } from '../lib/vocabularyDisplay'
import { playBrowserSpeech, playProfessionalPronunciation } from '../lib/vocabularyPronunciation'
import { masteryLabels } from '../api/vocabularyLearningApi'

/*
 * 主题页词卡。信息密度对齐 V1 VocabularyCard：
 *   左上状态字 / 右上单卡显示覆盖 / 词性 + 单词 + 发音入口 / 英式美式音标 / 词形
 *   虚线分隔 -> 释义 + 本主题用法 -> 底部 N 次记忆 + 加入计划按钮
 * 「N 次记忆」取个人记忆状态，不取词条上的内容侧统计：后者是全体读者的累计值。
 */
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({
  word: { type: Object, required: true },
  memory: { type: Object, default: null },
  settings: { type: Object, required: true },
  busy: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
})

/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['start', 'display', 'reset'])

/**
 * speaking；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const speaking = ref(false)
/**
 * 有效；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const active = computed(() => props.memory?.learningStatus === 'ACTIVE')
/**
 * mode；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const mode = computed(() => props.memory?.displayMode ?? 'FOLLOW_GLOBAL')
/**
 * visibility；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const visibility = computed(() => resolveVocabularyVisibility(mode.value, props.settings))
/**
 * show英语；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const showEnglish = computed(() => visibility.value.showEnglish)
/**
 * showChinese；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const showChinese = computed(() => visibility.value.showChinese)
/**
 * 上传；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const uploaded = computed(() => {
  const audios = props.word.audios ?? []
  return audios.find((audio) => audio.primary && audio.publicUrl) ?? audios.find((audio) => audio.publicUrl) ?? null
})
/**
 * examples；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const examples = computed(() => {
  if (!props.word.examples) return []
  try {
    const parsed = JSON.parse(props.word.examples)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

const displayOptions = [
  { value: 'FOLLOW_GLOBAL', label: '全局', title: '跟随全部词卡显示设置' },
  { value: 'BILINGUAL', label: '双语', title: '显示英文、音标和中文' },
  { value: 'ENGLISH_ONLY', label: '英文', title: '只显示英文和音标' },
  { value: 'CHINESE_ONLY', label: '中文', title: '只显示中文释义' },
]

/* 三级回退：已上传的授权音频 -> 后端代理 -> 浏览器合成 */
/**
 * pronounce的处理函数。
 * 主要协作：audio.play、playProfessionalPronunciation、playBrowserSpeech。
 * 更新的响应式状态包括 speaking；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function pronounce() {
  if (speaking.value) return
  speaking.value = true
  /**
   * 停止的处理函数。
   * 更新的响应式状态包括 speaking；渲染由 Vue 的依赖跟踪触发。
   */
  const stop = () => { speaking.value = false }
  if (uploaded.value) {
    try {
      const audio = new Audio(uploaded.value.publicUrl)
      audio.onended = stop
      audio.onerror = stop
      await audio.play()
      return
    } catch {
      /* 上传音频不可用时继续往下回退，不打断用户 */
    }
  }
  if (await playProfessionalPronunciation(props.word.word, stop)) return
  playBrowserSpeech(props.word.word, stop)
}

/**
 * 格式化时间。
 * 主要协作：.toLocaleString。
 * @param value 值；默认值、解构字段与可选参数见函数签名。
 * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
 */
function formatTime(value) {
  return value ? new Date(value).toLocaleString() : ''
}
</script>

<template>
  <article class="vocabulary-card" :class="{ 'vocabulary-card--selected': selectable && selected }">
    <div class="vocabulary-card__toolbar">
      <span :class="['vocabulary-card__status', { active }]">{{ selectable ? (masteryLabels[memory?.masteryRank] || '未修习') : active ? '已在计划' : '未加入' }}{{ memory?.inPlan ? ' · 当前计划内' : '' }}</span>
      <div class="vocabulary-card__display" role="group" aria-label="本卡显示方式">
        <button
          v-for="option in displayOptions"
          :key="option.value"
          type="button"
          :class="{ active: mode === option.value }"
          :aria-pressed="mode === option.value"
          :title="option.title"
          :disabled="busy"
          @click="emit('display', word, option.value)"
        >{{ option.label }}</button>
      </div>
    </div>

    <section v-if="showEnglish" class="vocabulary-card__english" aria-label="英文信息">
      <div class="vocabulary-card__heading">
        <span class="vocabulary-card__pos">{{ word.partOfSpeech }}</span>
        <h2>{{ word.word }}</h2>
        <button
          type="button"
          class="vocabulary-card__speak"
          :disabled="speaking"
          :aria-label="`朗读 ${word.word}`"
          @click="pronounce"
        >{{ speaking ? '播放中' : uploaded ? '播放真人发音' : '有道发音' }}</button>
      </div>
      <p v-if="word.phoneticUk || word.phoneticUs" class="vocabulary-card__phonetics">
        <span v-if="word.phoneticUk">英 {{ word.phoneticUk }}</span>
        <span v-if="word.phoneticUs">美 {{ word.phoneticUs }}</span>
      </p>
      <p v-if="word.inflections" class="vocabulary-card__muted">词形：{{ word.inflections }}</p>
      <div v-if="examples.length" class="vocabulary-card__examples">
        <p v-for="(example, index) in examples" :key="index">{{ example.sentence }}</p>
      </div>
    </section>

    <section v-if="showChinese" class="vocabulary-card__chinese" aria-label="中文信息">
      <p class="vocabulary-card__translation">{{ word.translation }}</p>
      <p v-if="word.sceneMeaning && word.sceneMeaning !== word.translation" class="vocabulary-card__scene">
        <span>本主题用法</span>{{ word.sceneMeaning }}
      </p>
      <div v-if="examples.some((item) => item.translation)" class="vocabulary-card__examples">
        <p v-for="(example, index) in examples" :key="index">{{ example.translation }}</p>
      </div>
    </section>

    <footer class="vocabulary-card__footer">
      <div class="vocabulary-card__progress">
        <strong>{{ memory?.memoryCount ?? 0 }}</strong>
        <span>次记忆 · {{ masteryLabels[memory?.masteryRank] || '未修习' }}</span>
        <span v-if="memory?.modeMemory?.length">英→中 {{ memory.modeMemory.find(m => m.direction === 'EN_TO_ZH')?.ratingCount || 0 }} 次 · 中→英 {{ memory.modeMemory.find(m => m.direction === 'ZH_TO_EN')?.ratingCount || 0 }} 次 · 听音 {{ memory.modeMemory.find(m => m.direction === 'AUDIO_TO_BOTH')?.ratingCount || 0 }} 次</span>
        <span v-if="memory?.memoryCount && !memory?.masteryRank">历史次数已保留，熟练度待评价</span>
        <span v-if="memory?.lastReviewedAt">上次 {{ formatTime(memory.lastReviewedAt) }}</span>
        <span v-if="memory?.nextReviewAt">下次 {{ formatTime(memory.nextReviewAt) }}</span>
      </div>
      <div class="vocabulary-card__actions">
        <button v-if="selectable" type="button" :class="{ selected }" :aria-pressed="selected" :disabled="busy" @click="emit('start', word)"><span aria-hidden="true">{{ selected ? '✓' : '+' }}</span> {{ selected ? '移出待选' : '加入待选' }}</button>
        <button v-else-if="!active" type="button" :disabled="busy" @click="emit('start', word)">加入记忆计划</button>
        <template v-else>
          <button type="button" class="joined" disabled>已在计划</button>
          <button type="button" class="danger" :disabled="busy" @click="emit('reset', word)">重新开始</button>
        </template>
      </div>
    </footer>
  </article>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.vocabulary-card。
 * 使用弹性布局排列子项，方向为 column；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card{display:flex;flex-direction:column;gap:18px;min-height:330px;padding:22px;border:1px solid var(--border);border-radius:18px;background:var(--bg-surface);box-shadow:var(--shadow-sm);transition:border-color .18s,box-shadow .18s}
/**
 * 样式范围：.vocabulary-card:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card:hover{border-color:var(--border-strong);box-shadow:var(--shadow-md)}
  /**
   * 样式范围：.vocabulary-card.vocabulary-card--selected。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocabulary-card.vocabulary-card--selected{border-color:var(--primary);box-shadow:0 0 0 1px var(--primary)}
/**
 * 样式范围：.vocabulary-card__toolbar,.vocabulary-card__heading,.vocabulary-card__footer,.vocabulary-card__actions,.vocabulary-card__phonetics。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.vocabulary-card__toolbar,.vocabulary-card__heading,.vocabulary-card__footer,.vocabulary-card__actions,.vocabulary-card__phonetics{display:flex;align-items:center;gap:10px}
/**
 * 样式范围：.vocabulary-card__toolbar,.vocabulary-card__footer。
 */
.vocabulary-card__toolbar,.vocabulary-card__footer{justify-content:space-between}
/**
 * 样式范围：.vocabulary-card__display。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__display{display:inline-flex;padding:3px;border:1px solid var(--border);border-radius:10px;background:var(--bg-subtle)}
/**
 * 样式范围：.vocabulary-card__display button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__display button{min-height:30px;padding:4px 8px;border:0;border-radius:7px;background:transparent;color:var(--text-muted);font-size:12px;cursor:pointer}
/**
 * 样式范围：.vocabulary-card__display button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__display button:hover{color:var(--text-primary)}
/**
 * 样式范围：.vocabulary-card__display button.active。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__display button.active{background:var(--bg-surface);color:var(--primary)}
/**
 * 样式范围：.vocabulary-card__display button:focus-visible。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__display button:focus-visible{outline:2px solid var(--primary);outline-offset:1px}
/**
 * 样式范围：.vocabulary-card__display button:disabled。
 */
.vocabulary-card__display button:disabled{cursor:wait;opacity:.65}
/**
 * 样式范围：.vocabulary-card__status。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__status{font-size:11px;color:var(--text-secondary);padding:4px 8px;border-radius:6px;background:var(--bg-subtle)}
/**
 * 样式范围：.vocabulary-card__status.active。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__status.active{color:var(--primary)}
/**
 * 样式范围：.vocabulary-card__heading。
 */
.vocabulary-card__heading{flex-wrap:wrap}
/**
 * 样式范围：.vocabulary-card__heading h2。
 */
.vocabulary-card__heading h2{margin:0;font-size:26px;line-height:1.2}
/**
 * 样式范围：.vocabulary-card__pos。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__pos{font-size:12px;color:var(--accent)}
/**
 * 样式范围：.vocabulary-card__speak。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__speak{margin-left:auto;border:0;background:transparent;color:var(--primary);cursor:pointer}
/**
 * 样式范围：.vocabulary-card__speak:disabled。
 */
.vocabulary-card__speak:disabled{cursor:wait;opacity:.7}
/**
 * 样式范围：.vocabulary-card__phonetics。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__phonetics{flex-wrap:wrap;color:var(--text-muted);font-size:13px;margin-top:8px}
/**
 * 样式范围：.vocabulary-card__muted。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__muted{font-size:13px;color:var(--text-muted)}
/**
 * 样式范围：.vocabulary-card__examples。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 6px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__examples{display:grid;gap:6px;margin-top:12px;font-size:14px;line-height:1.65;color:var(--text-secondary)}
/**
 * 样式范围：.vocabulary-card__chinese。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__chinese{padding-top:14px;border-top:1px dashed var(--border)}
/**
 * 样式范围：.vocabulary-card__translation。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__translation{font-size:17px;line-height:1.7;color:var(--text-primary)}
/**
 * 样式范围：.vocabulary-card__scene。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 4px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__scene{display:grid;gap:4px;margin-top:10px;font-size:14px;line-height:1.6;color:var(--text-secondary)}
/**
 * 样式范围：.vocabulary-card__scene span。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__scene span{font-size:11px;color:var(--text-muted);letter-spacing:.06em}
/**
 * 样式范围：.vocabulary-card__footer。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__footer{align-items:flex-end;margin-top:auto;padding-top:14px;border-top:1px solid var(--border)}
/**
 * 样式范围：.vocabulary-card__progress。
 * 使用网格布局组织内容；列定义为 子规则或自动网格决定，间距由 3px 控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__progress{display:grid;gap:3px;font-size:12px;color:var(--text-muted)}
/**
 * 样式范围：.vocabulary-card__progress strong。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__progress strong{color:var(--text-primary);font-size:20px}
/**
 * 样式范围：.vocabulary-card__actions。
 */
.vocabulary-card__actions{flex-wrap:wrap;justify-content:flex-end}
/**
 * 样式范围：.vocabulary-card__actions button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__actions button{min-height:36px;padding:7px 12px;border:1px solid var(--primary);border-radius:9px;background:transparent;color:var(--primary);cursor:pointer}
/**
 * 样式范围：.vocabulary-card__actions button:hover。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__actions button:hover{background:var(--primary-soft)}
  /**
   * 样式范围：.vocabulary-card__actions button.selected。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocabulary-card__actions button.selected{background:var(--primary);color:var(--on-primary)}
  /**
   * 样式范围：.vocabulary-card__actions button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .vocabulary-card__actions button:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
/**
 * 样式范围：.vocabulary-card__actions button.joined。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__actions button.joined{border-color:var(--border-strong);color:var(--text-muted);cursor:default}
/**
 * 样式范围：.vocabulary-card__actions button.danger。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.vocabulary-card__actions button.danger{border-color:var(--accent);color:var(--accent)}
/**
 * 样式范围：.vocabulary-card__actions button:disabled。
 */
.vocabulary-card__actions button:disabled{cursor:default;opacity:.8}
/**
 * 响应式或设备条件：(max-width:420px)；仅在该条件成立时覆盖基础样式。
 */
@media (max-width:420px){
  /**
   * 样式范围：.vocabulary-card。
   */
  .vocabulary-card{min-height:0;padding:18px}
  /**
   * 样式范围：.vocabulary-card__toolbar。
   */
  .vocabulary-card__toolbar{align-items:flex-start;flex-direction:column}
  /**
   * 样式范围：.vocabulary-card__display。
   */
  .vocabulary-card__display{width:100%}
  /**
   * 样式范围：.vocabulary-card__display button。
   */
  .vocabulary-card__display button{flex:1}
  /**
   * 样式范围：.vocabulary-card__footer。
   */
  .vocabulary-card__footer{align-items:flex-start;flex-direction:column}
  /**
   * 样式范围：.vocabulary-card__actions。
   */
  .vocabulary-card__actions{width:100%;justify-content:stretch}
  /**
   * 样式范围：.vocabulary-card__actions button。
   */
  .vocabulary-card__actions button{flex:1}
}
/**
 * 响应式或设备条件：(prefers-reduced-motion:reduce)；仅在该条件成立时覆盖基础样式。
 */
@media(prefers-reduced-motion:reduce){
  /**
   * 样式范围：.vocabulary-card。
   */
  .vocabulary-card{transition:none}}
</style>
