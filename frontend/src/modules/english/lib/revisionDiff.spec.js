/**
 * revisionDiff.spec.js：英语学习模块的自动化测试。
 * 职责范围：文档、词汇、语法、阅读听力及复习记录。
 * 使用模拟接口、固定输入或组件挂载验证行为，测试标题与断言说明期望结果。
 * 主要依赖：vitest、./revisionDiff。
 * 验证场景：immutable version comparison；preserves unchanged paragraphs around an insertion；marks removed and added text without parsing HTML。
 */

import { describe,expect,it } from 'vitest'
import { diffLines } from './revisionDiff'
describe('immutable version comparison',()=>{
 it('preserves unchanged paragraphs around an insertion',()=>{expect(diffLines('one\nthree','one\ntwo\nthree')).toEqual([{kind:'equal',text:'one'},{kind:'added',text:'two'},{kind:'equal',text:'three'}])})
 it('marks removed and added text without parsing HTML',()=>{const rows=diffLines('<script>old</script>','new');expect(rows).toContainEqual({kind:'removed',text:'<script>old</script>'});expect(rows).toContainEqual({kind:'added',text:'new'})})
})
