<script setup>
/**
 * VocabularyRecallCard.vue：英语学习模块的视图组件。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织模板和交互状态；输入属性与输出事件以本组件的 defineProps、defineEmits 声明为准。
 * 主要依赖：vue、../api/vocabularyLearningApi、../lib/vocabularyRecallAudio。
 * 组件输入：word、direction、memory、busy、locked；默认值与类型见 defineProps。
 * 组件输出事件：rate；负载由 emit 调用给出。
 * 主要响应式状态：props、emit、revealed、audioPlayed、playing、audioError、preview、audioMode。
 * 阅读入口：play（play的处理函数）。
 */


import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { masteryLabels, modeLabels } from '../api/vocabularyLearningApi'
import { createRecallPlayer } from '../lib/vocabularyRecallAudio'
/**
 * 组件输入；通过 defineProps 建立。
 */
const props = defineProps({ word: { type: Object, required: true }, direction: { type: String, required: true }, memory: Object, busy: Boolean, locked: Boolean })
/**
 * 事件发射器；通过 defineEmits 建立。
 */
const emit = defineEmits(['rate'])
/**
 * revealed；通过 ref 建立。
 * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
 */
const revealed = ref(false), audioPlayed = ref(false), playing = ref(false), audioError = ref('')
/**
 * 预览；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const preview = computed(() => props.direction === 'BILINGUAL_PREVIEW')
/**
 * 音频Mode；通过 computed 建立。
 * 该值由依赖自动重新计算，不作为独立可写状态维护。
 */
const audioMode = computed(() => props.direction === 'AUDIO_TO_BOTH')
const player = createRecallPlayer()
let request = 0
/**
 * 监听指定依赖的变化，并执行回调中的刷新或同步逻辑；监听源和选项决定触发时机。
 */
watch(() => [props.word.id, props.direction], () => {
  request += 1; player.stop(); revealed.value = false; audioPlayed.value = false; playing.value = false; audioError.value = ''
}, { immediate: true })
/**
 * play的处理函数。
 * 主要协作：player.play。
 * 更新的响应式状态包括 playing、audioError、audioPlayed；渲染由 Vue 的依赖跟踪触发。
 * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
 */
async function play() {
  const token = ++request
  playing.value = true; audioError.value = ''
  const ok = await player.play(props.word)
  if (token !== request) return
  playing.value = false
  audioPlayed.value ||= ok
  if (!ok) audioError.value = '发音无法播放。请重试，或切换其他方向；本次不会计入评分。'
}
/**
 * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
 */
onBeforeUnmount(() => { request += 1; player.stop() })
</script>

<template>
  <article class="recall-card" :aria-busy="busy">
    <header><span class="recall-card__mode">{{ modeLabels[direction] }}</span><span>{{ masteryLabels[memory?.masteryRank] || '未修习' }}</span></header>
    <p v-if="!preview && !revealed && !audioMode" class="recall-card__hint">{{ direction === 'EN_TO_ZH' ? '回忆这个单词的中文含义' : '回忆对应的英文单词' }}</p>
    <p v-if="preview || direction === 'EN_TO_ZH' || revealed" class="recall-card__word">{{ word.word }}</p>
    <p v-if="preview || direction === 'ZH_TO_EN' || revealed" class="recall-card__translation">{{ word.translation }}</p>
    <p v-if="audioMode && !revealed" class="recall-card__prompt">听发音，回忆英文词形与中文含义</p>
    <p v-if="preview || revealed" class="recall-card__phonetic">{{ word.phoneticUs || word.phoneticUk }}</p>
    <button v-if="audioMode || preview || revealed || direction === 'EN_TO_ZH'" type="button" :disabled="playing || busy || locked" @click="play">{{ playing ? '加载发音…' : '播放发音' }}</button>
    <p v-if="audioError" role="alert">{{ audioError }}</p>
    <p v-if="preview" class="recall-card__note">自由预览不计次数，不改变熟练度和复习时间。</p>
    <p v-else class="recall-card__note">{{ revealed ? '根据刚才的回忆，评价掌握程度' : audioMode && !audioPlayed ? '先听发音，再揭晓答案' : '想好答案后，点击下方揭晓' }}</p>
    <button v-if="!preview && !revealed" type="button" class="recall-card__reveal" :disabled="busy || locked || (audioMode && !audioPlayed)" @click="revealed = true">揭晓答案</button>
    <div v-else-if="!preview" class="recall-card__ratings" aria-label="回忆自评">
      <button type="button" :disabled="busy || locked" @click="emit('rate', { rating: 'FORGOT', audioPlayed })">忘记</button>
      <button type="button" :disabled="busy || locked" @click="emit('rate', { rating: 'UNCERTAIN', audioPlayed })">模糊</button>
      <button type="button" :disabled="busy || locked" @click="emit('rate', { rating: 'KNOW', audioPlayed })">掌握</button>
    </div>
  </article>
</template>

<style scoped>
/**
 * 组件局部样式：布局、状态和断点分别在以下规则处说明。
 */

/**
 * 样式范围：.recall-card。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.recall-card{max-width:700px;min-height:350px;margin:18px auto;padding:28px 32px 32px;border:1px solid var(--border);border-radius:20px;background:var(--bg-surface);box-shadow:var(--shadow-md);text-align:center}
/**
 * 样式范围：header。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
header{display:flex;justify-content:space-between;gap:12px;font-size:13px;color:var(--text-muted)}
/**
 * 样式范围：.recall-card__mode。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.recall-card__mode{color:var(--primary);background:var(--primary-soft);padding:5px 10px;border-radius:6px;font-size:12px}
  /**
   * 样式范围：header。
   */
  header{align-items:center}
  /**
   * 样式范围：.recall-card__hint。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .recall-card__hint{font-size:12px;color:var(--text-muted);margin:28px 0 0}
/**
 * 样式范围：.recall-card__word。
 */
.recall-card__word{margin:24px 0 14px;font-size:clamp(32px,6vw,52px);line-height:1.2;font-weight:600;letter-spacing:-.03em;overflow-wrap:anywhere}
/**
 * 样式范围：.recall-card__translation。
 */
.recall-card__translation{font-size:22px;line-height:1.6}
  /**
   * 样式范围：.recall-card__prompt。
   */
  .recall-card__prompt{padding:50px 0 20px;font-size:20px}
/**
 * 样式范围：.recall-card__phonetic,.recall-card__note。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.recall-card__phonetic,.recall-card__note{color:var(--text-muted);font-size:13px}
  /**
   * 样式范围：.recall-card__note。
   */
  .recall-card__note{margin-top:24px}
/**
 * 样式范围：button。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
button{padding:10px 16px;min-height:42px;border:1px solid var(--border-strong);border-radius:10px;background:var(--bg-subtle);color:var(--text-primary);cursor:pointer}
/**
 * 样式范围：button:disabled。
 */
button:disabled{opacity:.5;cursor:default}
  /**
   * 样式范围：button:focus-visible。
   * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  button:focus-visible{outline:2px solid var(--primary);outline-offset:3px}
  /**
   * 样式范围：.recall-card__reveal。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .recall-card__reveal{display:block;min-width:180px;margin:16px auto 0;background:var(--primary);color:var(--on-primary)}
/**
 * 样式范围：.recall-card__reveal:hover:not(:disabled)。
 * 此段覆盖交互或选中状态；实际改变的属性由以下声明决定。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
.recall-card__reveal:hover:not(:disabled){background:var(--primary);color:var(--on-primary);filter:brightness(1.1)}
/**
 * 样式范围：.recall-card__ratings。
 * 使用弹性布局排列子项，方向为 默认水平方向；对齐及换行由本规则声明控制。
 */
.recall-card__ratings{display:flex;justify-content:center;gap:12px;margin-top:16px}
  /**
   * 样式范围：.recall-card__ratings button。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .recall-card__ratings button{flex:1;max-width:160px;background:var(--bg-surface)}
  /**
   * 样式范围：.recall-card__ratings button:first-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .recall-card__ratings button:first-child{color:var(--accent)}
  /**
   * 样式范围：.recall-card__ratings button:last-child。
   * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
   */
  .recall-card__ratings button:last-child{background:var(--primary);color:var(--on-primary)}
/**
 * 样式范围：[role=alert]。
 * 颜色、字号或间距复用主题变量，变量取值由当前外壳与明暗主题决定。
 */
[role=alert]{color:var(--accent);font-size:14px}
  /**
   * 响应式或设备条件：(max-width:600px)；仅在该条件成立时覆盖基础样式。
   */
  @media(max-width:600px){
  /**
   * 样式范围：.recall-card。
   */
  .recall-card{padding:20px 14px}
  /**
   * 样式范围：.recall-card__ratings。
   */
  .recall-card__ratings{gap:8px}
  /**
   * 样式范围：.recall-card__ratings button。
   */
  .recall-card__ratings button{flex:1}}
</style>
