/**
 * revisionDiff.js：英语学习模块的应用配置与入口。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 组织前端装配、构建或入口行为，具体依赖和导出入口见下文。
 * 阅读入口：diffLines（diffLines的处理函数）。
 */

export
  /**
   * diffLines的处理函数。
   * 主要协作：before.split、after.split、a.map、b.map、Array.from。
   * @param before='' 之前；默认值、解构字段与可选参数见函数签名。
   * @param after='' 之后；默认值、解构字段与可选参数见函数签名。
   * @returns 本函数计算或组装的结果，具体结构由返回表达式定义。
   */
  function diffLines(before='',after='') {
 const a=before.split('\n'),b=after.split('\n')
 if(a.length*b.length>360000)return [...a.map(text=>({kind:'removed',text})),...b.map(text=>({kind:'added',text}))]
 const table=Array.from({length:a.length+1},()=>new Uint32Array(b.length+1))
 for(let i=a.length-1;i>=0;i--)for(let j=b.length-1;j>=0;j--)table[i][j]=a[i]===b[j]?table[i+1][j+1]+1:Math.max(table[i+1][j],table[i][j+1])
 const rows=[];let i=0,j=0
 while(i<a.length || j<b.length) {
  if(i<a.length && j<b.length && a[i]===b[j]){rows.push({kind:'equal',text:a[i++]});j++}
  else if(j<b.length && (i===a.length || table[i][j+1]>=table[i+1][j]))rows.push({kind:'added',text:b[j++]})
  else rows.push({kind:'removed',text:a[i++]})
 }
 return rows
}
