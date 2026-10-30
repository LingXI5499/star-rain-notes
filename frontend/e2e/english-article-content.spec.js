import { test, expect } from './fixtures'

const english='The rain stopped. We set off!'
const chinese='雨停了。我们出发！'
const oldSummary='LEGACY_SUMMARY_SHOULD_NOT_APPEAR'

async function setup(page,fixture){
  fixture.user={...fixture.user,permissions:[...fixture.user.permissions,'english:content-read-admin','english:content-edit']}
  const state={created:[],saved:[],article:{id:'5',slug:'after-rain',title:'雨后',summary:oldSummary,bodyMarkdown:english,translationZhMarkdown:chinese,keywords:[],rowVersion:1,state:'DRAFT',visibility:'PRIVATE',publishStatus:'PUBLISHED'}}
  await page.route('**/api/public/english/library/**',route=>{
    const path=new URL(route.request().url()).pathname
    return route.fulfill({json:{code:'OK',data:path.endsWith('/taxonomy')?[]:path.endsWith('/counts')?{}:{worldArticles:1}}})
  })
  await page.route(/\/api\/(public|admin)\/english\/(essays|world|content\/reading)/,async route=>{
    const request=route.request(),path=new URL(request.url()).pathname,method=request.method()
    const ok=data=>route.fulfill({json:{code:'OK',data}})
    if(/\/(alignments|annotations|vocabulary)$/.test(path))return ok([])
    if(path.endsWith('/facets'))return ok({tags:[],months:[],genres:[],total:1})
    if(method==='POST'){
      const body=request.postDataJSON();state.created.push(body);state.article={...state.article,...body,summary:body.summary || ''};return ok(state.article)
    }
    if(method==='PUT'){
      const body=request.postDataJSON();state.saved.push(body);state.article={...state.article,...body,rowVersion:body.rowVersion+1};return ok(state.article)
    }
    return ok(/\/(5|after-rain)$/.test(path)?state.article:{items:[state.article],total:1})
  })
  return state
}

test('essay creation saves only the title and both texts and previews sentence pairs',async({page,fixture})=>{
  const state=await setup(page,fixture)
  await page.setViewportSize({width:1440,height:1000});await page.goto('/english/manage/essays/editor/new')
  await expect(page.getByLabel(/摘要/)).toHaveCount(0)
  await page.getByLabel('文章标题',{exact:true}).fill('雨后')
  const editors=page.locator('.vditor-ir')
  await expect(editors).toHaveCount(2)
  await editors.nth(0).click();await page.keyboard.insertText(chinese)
  await editors.nth(1).click();await page.keyboard.insertText(english)
  await page.getByRole('button',{name:'保存草稿',exact:true}).click()
  await expect(page).toHaveURL(/\/english\/manage\/essays\/editor\/5$/)
  expect(state.created).toHaveLength(1);expect(state.created[0]).not.toHaveProperty('summary')
  expect(state.created[0].bodyMarkdown).toContain(english);expect(state.created[0].translationZhMarkdown).toContain(chinese)
  await page.reload();await page.getByRole('button',{name:'双语预览',exact:true}).click()
  const parallel=page.getByRole('region',{name:'中英逐句对照'}),rows=parallel.locator('.article-parallel__row')
  await expect(rows).toHaveCount(2);await expect(rows.nth(0)).toContainText('The rain stopped.');await expect(rows.nth(0)).toContainText('雨停了。')
  await expect(rows.nth(0)).not.toContainText('We set off!');await expect(rows.nth(1)).toContainText('我们出发！')
  await page.screenshot({path:'../.local/english-article-sentences-desktop.png',fullPage:true})
  await page.setViewportSize({width:390,height:844});await parallel.scrollIntoViewIfNeeded()
  expect(await parallel.evaluate(element=>element.scrollWidth<=element.clientWidth)).toBe(true)
  await page.screenshot({path:'../.local/english-article-sentences-mobile.png',fullPage:true})
})

test('English article lists, details and world editing omit historical summaries',async({page,fixture})=>{
  await setup(page,fixture)
  for(const path of ['/english/manage/essays','/english/manage/world','/english/essays','/english/world','/english/essays/after-rain','/english/world/articles/after-rain']){
    await page.goto(path)
    await expect(page.getByRole('heading',{name:'雨后',exact:true})).toBeVisible()
    await expect(page.locator('main').first()).not.toContainText(oldSummary);await expect(page.locator('main').first()).not.toContainText('暂无摘要')
    if(path.includes('/after-rain'))await expect(page.locator('.article-parallel__row')).toHaveCount(2)
  }
  await page.goto('/english/manage/editor/world/new')
  await expect(page.getByRole('heading',{name:'英文原文',exact:true})).toBeVisible()
  await expect(page.getByRole('heading',{name:'中文翻译',exact:true})).toBeVisible()
  await expect(page.getByLabel(/摘要/)).toHaveCount(0)
})
