/**
 * dateTimePicker.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 阅读入口：openDateTimePicker（打开日期时间选择器）。
 */

export
  /**
   * 打开日期时间选择器。
   * 主要协作：input.showPicker、input.focus。
   * @param event 事件；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function openDateTimePicker(event) {
  const input = event.currentTarget
  if (typeof input.showPicker !== 'function') return
  try {
    input.showPicker()
  } catch {
    input.focus()
  }
}
