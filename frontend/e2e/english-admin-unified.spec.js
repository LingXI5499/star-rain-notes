import { test, expect } from './fixtures'

async function setup(page, fixture) {
  fixture.user = { ...fixture.user, permissions: [...fixture.user.permissions, 'english:content-read-admin','english:content-edit'] }
  const state = { deleted:[], orders:[], words:[], terms:[], sections:[
    {id:'1',title:'语法基础',sortOrder:10,lessons:[{id:'11',sectionId:'1',title:'名词',slug:'nouns',publishStatus:'DRAFT',sortOrder:10},{id:'12',sectionId:'1',title:'动词',slug:'verbs',publishStatus:'PUBLISHED',sortOrder:20}]},
    {id:'2',title:'句法结构',sortOrder:20,lessons:[]},
  ] }
  const tree=[{id:'101',dimension:'VOCABULARY',libraryModule:'vocabulary',code:'T01',name:'生活',nameEn:'Life',enabled:true,children:[{id:'102',parentId:'101',dimension:'VOCABULARY',libraryModule:'vocabulary',code:'T01-01',name:'健康',nameEn:'Health',enabled:true,children:[]}]}]
  const word={id:'1',word:'physical',themeId:'9',phoneticUk:'/ˈfɪzɪkəl/',phoneticUs:'ˈfɪzɪkəl',translation:'身体的；物理的',sceneMeaning:'身体健康',topicCodes:['T01-01'],migrationStatus:'confirmed',migrationReason:'',rowVersion:1,examples:'[]'}
  await page.route('**/api/account/english/vocabulary/**', route => {
    const path=new URL(route.request().url()).pathname
    const data=path.endsWith('/learning/words')?{items:[word],total:1}:path.endsWith('/learning/states')?[]:{status:'NONE',revision:0}
    return route.fulfill({json:{code:'OK',data}})
  })
  await page.route(/\/api\/(public|admin)\/english\/(grammar|taxonomy|library)/, async route => {
    const req=route.request(),url=new URL(req.url()),path=url.pathname,method=req.method(),ok=data=>route.fulfill({json:{code:'OK',data}})
    if(path==='/api/admin/english/grammar')return ok({course:{title:'英语语法完整教程',publishStatus:'PUBLISHED'},sections:state.sections})
    if(path==='/api/admin/english/grammar/sections/order'){
      const ids=req.postDataJSON().ids;state.orders.push(ids);state.sections=ids.map(id=>state.sections.find(s=>s.id===id));return ok(null)
    }
    if(path.match(/\/grammar\/sections\/\d+\/lessons\/order$/)){
      const section=state.sections.find(s=>path.includes('/sections/'+s.id+'/')),ids=req.postDataJSON().ids
      section.lessons=ids.map(id=>section.lessons.find(l=>l.id===id));state.orders.push(ids);return ok(null)
    }
    if(method==='DELETE'&&path.includes('/grammar/sections/')){const id=path.split('/').at(-1);state.deleted.push(id);state.sections=state.sections.filter(s=>s.id!==id);return ok(null)}
    if(path==='/api/admin/english/taxonomy'){
      if(method==='POST'){state.terms.push(req.postDataJSON());return ok({...req.postDataJSON(),id:'103'})}return ok(tree)
    }
    if(path.endsWith('/taxonomy'))return ok(tree)
    if(path.endsWith('/counts'))return ok({T01:1,'T01-01':1})
    if(path.endsWith('/overview'))return ok({confirmedRecords:1,vocabularyRecords:1})
    if(path==='/api/admin/english/library/vocabulary'&&method==='POST'){state.words.push(req.postDataJSON());return ok({id:'2'})}
    if(path.endsWith('/words'))return ok({items:[word],total:1})
    return ok(null)
  })
  return state
}

test('grammar provides sibling ordering and confirms deletion of populated sections',async({page,fixture})=>{
  const state=await setup(page,fixture);await page.setViewportSize({width:1440,height:1000})
  await page.goto('/english/manage/grammar?section=1&keep=yes#catalog')
  await expect(page.getByRole('button',{name:'删除章节 语法基础',exact:true})).toBeVisible()
  await expect(page.getByRole('button',{name:'发布',exact:true})).toBeVisible()
  await page.getByRole('button',{name:'下移课时 名词',exact:true}).click()
  await expect.poll(()=>state.orders.length).toBe(1);expect(state.orders[0]).toEqual(['12','11'])
  await page.getByRole('searchbox',{name:'搜索课时'}).fill('名词')
  await expect(page.getByRole('button',{name:'上移课时 名词',exact:true})).toBeDisabled()
  await page.getByRole('searchbox',{name:'搜索课时'}).fill('')
  await page.screenshot({path:'../.local/grammar-unified-desktop.png',fullPage:true})
  await page.getByRole('button',{name:'删除章节 语法基础',exact:true}).click()
  const dialog=page.getByRole('dialog',{name:'确认操作'})
  await expect(dialog).toContainText('全部 2 个课时');await dialog.getByRole('button',{name:'取消',exact:true}).click();expect(state.deleted).toHaveLength(0)
  await page.getByRole('button',{name:'删除章节 语法基础',exact:true}).click();await dialog.getByRole('button',{name:'删除章节及课时',exact:true}).click()
  await expect.poll(()=>state.deleted).toEqual(['1']);await expect(page.locator('.grammar-admin__section-select')).toHaveCount(1)
  await expect(page).toHaveURL(/keep=yes/);await expect(page).toHaveURL(/#catalog$/)
})

test('vocabulary creation and taxonomy use the current simple hierarchy and shared cards',async({page,fixture})=>{
  const state=await setup(page,fixture);await page.setViewportSize({width:1440,height:1000})
  await page.goto('/english/manage/vocabulary/source')
  await expect(page).toHaveURL(/\/english\/manage\/vocabulary\?create=word/)
  const form=page.getByRole('region',{name:'单词编辑'})
  await expect(form).toBeVisible();await form.getByLabel('一级分类',{exact:true}).selectOption('T01');await form.getByLabel('二级主题',{exact:true}).selectOption('T01-01')
  await form.getByLabel('单词',{exact:true}).fill('resilient');await form.getByRole('button',{name:'保存单词',exact:true}).click()
  await expect.poll(()=>state.words.length).toBe(1);expect(state.words[0]).toMatchObject({category:'T01',topic:'T01-01',word:{word:'resilient',translation:''}})
  await expect(page.getByRole('button',{name:'播放 physical 的发音',exact:true})).toBeVisible()
  await expect(page.locator('.library-word')).not.toContainText('旧主题');await expect(page.locator('.library-word')).not.toContainText('已映射')
  await page.screenshot({path:'../.local/vocabulary-admin-unified.png',fullPage:true})
  await page.getByRole('link',{name:'分类管理',exact:true}).click()
  await expect(page.getByRole('heading',{name:'词汇分类管理',exact:true})).toBeVisible()
  await expect(page.getByLabel('分类编号')).toHaveCount(0);await expect(page.getByLabel('稳定标识')).toHaveCount(0);await expect(page.getByLabel('维度')).toHaveCount(0)
  await page.getByLabel('父分类',{exact:true}).selectOption('101');await page.getByLabel('中文名称',{exact:true}).fill('饮食');await page.getByLabel('英文名称',{exact:true}).fill('Food')
  await page.getByRole('button',{name:'保存主题',exact:true}).click();await expect.poll(()=>state.terms.length).toBe(1)
  expect(state.terms[0]).toEqual({name:'饮食',nameEn:'Food',parentId:'101',dimension:'VOCABULARY',libraryModule:'vocabulary'})
  await page.screenshot({path:'../.local/vocabulary-taxonomy-simple.png',fullPage:true})
  await page.goto('/english/vocabulary')
  await expect(page.getByRole('region',{name:'单词记忆学习'})).toBeVisible()
  await expect(page.locator('.library-word')).toContainText('本主题用法')
  await page.getByRole('button',{name:'选择单词 physical',exact:true}).click()
  await expect(page.getByRole('region',{name:'单词记忆学习'})).toContainText('已选 1 词')
  await page.reload();await expect(page.getByRole('button',{name:'选择单词 physical',exact:true})).toHaveAttribute('aria-pressed','true')
  await expect(page.getByRole('button',{name:'查看待选并设置计划',exact:true})).toBeEnabled()
  await page.screenshot({path:'../.local/vocabulary-public-unified.png',fullPage:true})
})
