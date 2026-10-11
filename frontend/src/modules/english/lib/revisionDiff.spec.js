import { describe,expect,it } from 'vitest'
import { diffLines } from './revisionDiff'
describe('immutable version comparison',()=>{
 it('preserves unchanged paragraphs around an insertion',()=>{expect(diffLines('one\nthree','one\ntwo\nthree')).toEqual([{kind:'equal',text:'one'},{kind:'added',text:'two'},{kind:'equal',text:'three'}])})
 it('marks removed and added text without parsing HTML',()=>{const rows=diffLines('<script>old</script>','new');expect(rows).toContainEqual({kind:'removed',text:'<script>old</script>'});expect(rows).toContainEqual({kind:'added',text:'new'})})
})
