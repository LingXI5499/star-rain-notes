import {afterEach,expect,it,vi} from 'vitest'
import {countEditorText,outlinePreferenceKey,readOutlinePreference,saveOutlinePreference,readLayoutPreference,saveLayoutPreference} from './editorViewState'
afterEach(()=>{localStorage.clear();vi.restoreAllMocks()})
it('bounds saved layout values and tolerates corrupt or blocked storage',()=>{
  saveLayoutPreference('a',{outlineWidth:320,canvasWidth:800})
  expect(readLayoutPreference('a')).toEqual({outlineWidth:320,canvasWidth:800})
  localStorage.setItem('b:layout','{"outlineWidth":9999,"canvasWidth":-2}')
  expect(readLayoutPreference('b')).toEqual({outlineWidth:480,canvasWidth:0})
  localStorage.setItem('a:layout','broken')
  expect(readLayoutPreference('a')).toEqual({outlineWidth:224,canvasWidth:0})
  vi.spyOn(Storage.prototype,'getItem').mockImplementation(()=>{throw Error('blocked')})
  expect(readLayoutPreference('a')).toEqual({outlineWidth:224,canvasWidth:0})
})
it('keeps outline preferences separate by account, module and bilingual field',()=>{
 const context={accountId:1,module:'english-writing',field:'bodyMarkdown'},key=outlinePreferenceKey(context)
 expect(readOutlinePreference(key,false)).toBe(false);saveOutlinePreference(key,true);expect(readOutlinePreference(key,false)).toBe(true)
 expect(outlinePreferenceKey({...context,field:'translationZhMarkdown'})).not.toBe(key)
 expect(readOutlinePreference(outlinePreferenceKey({...context,accountId:2}),false)).toBe(false)
})
it('continues with defaults when browser storage is unavailable',()=>{vi.spyOn(Storage.prototype,'getItem').mockImplementation(()=>{throw Error('blocked')});expect(readOutlinePreference('x',false)).toBe(false)})
it('counts Chinese characters, English words and lines consistently',()=>{expect(countEditorText('中文 English words\nNext line.')).toEqual({words:6,lines:2});expect(countEditorText(null)).toEqual({words:0,lines:0});expect(countEditorText('\n')).toEqual({words:0,lines:2})})
