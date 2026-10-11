/**
 * publicListState.js：跨模块基础模块的共享工具。
 * 职责范围：路由、应用装配、共享组件或样式能力。
 * 将跨模块使用的行为收敛到一个实现，调用方通过导出函数或组合式函数使用。
 * 阅读入口：publicPage（公开分页的处理函数）；publicPageSize（公开分页大小的处理函数）；sizeQuery（大小查询的处理函数）。
 */

export
  /**
   * 公开分页的处理函数。
   * 主要协作：Number.isSafeInteger。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function publicPage(value) { const n = Number(value); return Number.isSafeInteger(n) && n > 0 && n <= 100000 ? n : 1 }
export
  /**
   * 公开分页大小的处理函数。
   * 主要协作：sizes.includes。
   * @param value 值；默认值、解构字段与可选参数见函数签名。
   * @param defaultSize = 12 默认大小12；默认值、解构字段与可选参数见函数签名。
   * @param sizes = [12, 24, 48] sizes122448；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function publicPageSize(value, defaultSize = 12, sizes = [12, 24, 48]) { const n = Number(value); return sizes.includes(n) ? n : defaultSize }
export
  /**
   * 大小查询的处理函数。
   * @param query 查询；默认值、解构字段与可选参数见函数签名。
   * @param size 大小；默认值、解构字段与可选参数见函数签名。
   * @param defaultSize 默认大小；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function sizeQuery(query, size, defaultSize) { return { ...query, page: undefined, pageSize: size === defaultSize ? undefined : String(size) } }
