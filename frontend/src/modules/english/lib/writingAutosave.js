/**
 * writingAutosave.js：英语学习模块的应用配置与入口。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 阅读入口：createWritingAutosave（创建写作Autosave）；flush（flush的处理函数）。
 */

// One in-flight save. A newer local edit uses the CAS version returned by the previous request.
export
  /**
   * 创建写作Autosave。
   * 主要协作：clearTimeout、JSON.parse、JSON.stringify、read、onState。
   * @param { read, save, applyVersion, onState, delay=1000 } 读取saveapply版本触发状态delay1000；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function createWritingAutosave({ read, save, applyVersion, onState, delay=1000 }) {
 let timer,flight,dirty=false,stopped=false,disposed=false
 /**
  * flush的处理函数。
  * 主要协作：clearTimeout、JSON.parse、JSON.stringify、read、onState。
  * @returns {Promise} 异步处理结果；请求或校验失败时由 Promise 的拒绝路径报告。
  */
 async function flush() {
  clearTimeout(timer)
  if (stopped || disposed) return false
  if (flight) { await flight; if(stopped || disposed)return false }
  if (!dirty) return true
  dirty=false;const payload=JSON.parse(JSON.stringify(read()));onState('saving')
  flight=(async()=>{
   try { const saved=await save(payload);if(!disposed){applyVersion(saved);onState('saved')}return true }
   catch(cause) { dirty=true;stopped=true;if(!disposed)onState(cause?.response?.status===409?'conflict':'error',cause);return false }
   finally { flight=null }
  })()
  const okay=await flight
  if(okay && dirty && !disposed) return flush()
  return okay
 }
 return {
  change() { if(disposed)return;dirty=true;if(stopped)return;onState('dirty');clearTimeout(timer);timer=setTimeout(flush,delay) },
  flush,
  stop() { stopped=true;clearTimeout(timer) },
  resume() { stopped=false;return flush() },
  dispose() { disposed=true;clearTimeout(timer) },
  isDirty:()=>!disposed && (dirty || Boolean(flight)),
  isStopped:()=>stopped,
 }
}
