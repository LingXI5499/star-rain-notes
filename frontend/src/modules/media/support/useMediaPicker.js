import { ref } from 'vue'

/*
 * 媒体选择器状态。
 *
 * 供教程 / 博客 / 作品编辑器使用：pick() 打开选择器并返回一个 Promise，
 * 用户选中后 resolve 出媒体资产，取消则 resolve 出 null。
 * 这样调用方写的是 `const asset = await picker.pick('IMAGE')`，
 * 不需要在每个编辑器里各写一套弹窗状态。
 */
export function useMediaPicker() {
  const pickerOpen = ref(false)
  const pickerType = ref('')
  let resolvePick = null

  function pick(mediaType = '') {
    pickerType.value = mediaType
    pickerOpen.value = true
    return new Promise((resolve) => {
      resolvePick = resolve
    })
  }

  // asset 为空表示用户取消选择
  function settle(asset) {
    pickerOpen.value = false
    const resolve = resolvePick
    resolvePick = null
    resolve?.(asset || null)
  }

  return { pickerOpen, pickerType, pick, settle }
}
