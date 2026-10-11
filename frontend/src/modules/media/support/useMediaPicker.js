/**
 * useMediaPicker.js：媒体模块的业务支持工具。
 * 职责范围：上传校验、本地存储、访问级别、业务引用与文件清理。
 * 提供格式化、映射或计算能力，供模块页面与组件复用。
 * 主要依赖：vue。
 * 主要响应式状态：pickerOpen、pickerType。
 * 阅读入口：useMediaPicker（use媒体选择器的处理函数）；pick（pick的处理函数）；settle（settle的处理函数）。
 */

import { onBeforeUnmount, ref } from 'vue'

/*
 * 媒体选择器状态。
 *
 * 供教程 / 博客 / 作品编辑器使用：pick() 打开选择器并返回一个 Promise，
 * 用户选中后 resolve 出媒体资产，取消则 resolve 出 null。
 * 这样调用方写的是 `const asset = await picker.pick('IMAGE')`，
 * 不需要在每个编辑器里各写一套弹窗状态。
 */
export
  /**
   * use媒体选择器的处理函数。
   * 主要协作：settle、onBeforeUnmount、resolve。
   * 更新的响应式状态包括 pickerType、pickerOpen；渲染由 Vue 的依赖跟踪触发。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function useMediaPicker() {
  /**
   * 选择器Open；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const pickerOpen = ref(false)
  /**
   * 选择器类型；通过 ref 建立。
   * 用于驱动组件或组合式函数的状态变化；修改后由 Vue 更新相关视图。
   */
  const pickerType = ref('')
  let resolvePick = null

  /**
   * pick的处理函数。
   * 主要协作：settle。
   * 更新的响应式状态包括 pickerType、pickerOpen；渲染由 Vue 的依赖跟踪触发。
   * @param mediaType = '' 媒体类型；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function pick(mediaType = '') {
    settle(null)
    pickerType.value = mediaType
    pickerOpen.value = true
    return new Promise((resolve) => {
      resolvePick = resolve
    })
  }
  /**
   * 组件销毁前释放监听、定时器、请求或其它资源，具体清理对象见回调。
   */
  onBeforeUnmount(() => settle(null))

  // asset 为空表示用户取消选择
  /**
   * settle的处理函数。
   * 主要协作：resolve。
   * 更新的响应式状态包括 pickerOpen；渲染由 Vue 的依赖跟踪触发。
   * @param asset 资产；默认值、解构字段与可选参数见函数签名。
   */
  function settle(asset) {
    pickerOpen.value = false
    const resolve = resolvePick
    resolvePick = null
    resolve?.(asset || null)
  }

  return { pickerOpen, pickerType, pick, settle }
}
