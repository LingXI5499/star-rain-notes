import { describe, it, expect } from 'vitest'
import { libraryPath, libraryQuery, wordExamples, wordEditRequest } from './libraryNavigation'

describe('English library navigation', () => {
  it('builds canonical paths and rejects cross-parent topics and traversal', () => {
    expect(libraryPath('vocabulary','T01','T01-02')).toBe('/english/vocabulary/t01/t01-02')
    expect(libraryPath('world','R16','R16-01')).toBe('/english/world/r16/r16-01')
    for (const values of [['world','T01'],['vocabulary','T01','T02-01'],['world','../r01'],['essays','R01']]) expect(()=>libraryPath(...values)).toThrow()
  })
  it('resets pagination while retaining search, language, size and unrelated filters', () => {
    expect(libraryQuery({q:'eyes',display:'en',size:'48',page:'4',status:'confirmed'},{q:'hand'})).toEqual({q:'hand',display:'en',size:'48',page:undefined,status:'confirmed'})
  })
  it('keeps real examples and tolerates empty historical records', () => {
    expect(wordExamples('[{"en":"A word.","zh":"一个单词。"}]')).toEqual(['A word. — 一个单词。'])
    expect(wordExamples('[]')).toEqual([])
    expect(wordExamples('A legacy example.')).toEqual(['A legacy example.'])
  })
  it('sends editable word fields while keeping response identities and learning counters out of the command',()=>{
    expect(wordEditRequest({id:'7',themeId:'1',word:'eye',translation:'眼睛',examples:'[]',memoryCount:100,topicCodes:['T01-01'],rowVersion:8,entryId:'legacy-1'})).toEqual({themeId:'1',word:'eye',translation:'眼睛',examples:'[]'})
  })
})
