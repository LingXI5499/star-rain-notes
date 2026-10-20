<script setup>
/** One directory per shell, rebuilt when the route, content, language or disclosures change. */
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ReadingDrawer from '../../modules/blog/components/ReadingDrawer.vue'
import { collectReadingNavigation } from './collectReadingNavigation'

const props = defineProps({ root: { type: Object, default: null } })
const route = useRoute(), items = ref([]), courses = ref([])
let observer, frame = 0, disposed = false
function refresh() {
  frame = 0
  if (disposed) return
  const navigation = collectReadingNavigation(props.root, route.path)
  items.value = navigation.items
  courses.value = navigation.courses
}
function schedule() { if (!frame && !disposed) frame = requestAnimationFrame(refresh) }
watch(() => props.root, root => {
  observer?.disconnect()
  if (root) {
    observer = new MutationObserver(schedule)
    observer.observe(root, { subtree: true, childList: true, characterData: true,
      attributes: true, attributeFilter: ['hidden', 'style', 'open', 'aria-hidden', 'aria-busy'] })
  }
  schedule()
}, { immediate: true, flush: 'post' })
watch(() => route.path, async () => {
  items.value = []; courses.value = []
  await nextTick()
  schedule()
})
onMounted(() => window.addEventListener('resize', schedule))
onBeforeUnmount(() => { disposed = true; observer?.disconnect(); cancelAnimationFrame(frame); window.removeEventListener('resize', schedule) })
</script>
<template>
  <ReadingDrawer v-if="items.length || courses.length" :key="route.path" :items="items" :courses="courses" />
</template>
