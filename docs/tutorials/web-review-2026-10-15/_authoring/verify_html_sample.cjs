// 验证实际交付的首章示例与离线验收页；不连接网站或数据库。
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { pathToFileURL } = require('node:url');
const { chromium } = require(path.resolve(__dirname, '../../../../frontend/node_modules/@playwright/test'));
const root = path.resolve(__dirname, '..');
const chapterDir = path.join(root, 'HTML/01-先认识HTML的组成');
const data = JSON.parse(fs.readFileSync(path.join(chapterDir, '01-学习数据.json'), 'utf8'));
const md = fs.readFileSync(path.join(chapterDir, data.chapterFile), 'utf8');
const report = { date: '2026-10-15', checks: [], failures: [] };
function check(name, fn) { return Promise.resolve().then(fn).then(() => report.checks.push(name)); }

async function main() {
  await check('问题、知识卡片与练习的本地键唯一且关联完整', () => {
    const ids = [...data.knowledgeCards, ...data.questions, ...data.exercises].map(x => x.key);
    assert.equal(new Set(ids).size, ids.length);
    const cardIds = new Set(data.knowledgeCards.map(c => c.key));
    for (const q of [...data.questions, ...data.exercises]) {
      assert(q.knowledgeCardKeys.length > 0);
      for (const k of q.knowledgeCardKeys) assert(cardIds.has(k), k);
    }
    assert.equal(data.questions.length, 7);
    assert.equal(data.knowledgeCards.length, 6);
  });
  await check('文稿问题与问题数据逐项一致', () => {
    const section = md.split('## 三、问题')[1].split('## 四、练习题')[0];
    const questions = [...section.matchAll(/^\d+\. (.+)$/gm)].map(m => m[1].replace(/`/g, ''));
    assert.deepEqual(questions, data.questions.map(q => q.questionText));
  });
  await check('文稿完整示例与交付的 index.html 一致', () => {
    const demo = fs.readFileSync(path.join(chapterDir, 'examples/01-html-syntax/index.html'), 'utf8').trim();
    const blocks = [...md.matchAll(/```html\n([\s\S]*?)\n```/g)].map(m => m[1]);
    assert(blocks.includes(demo));
  });
  const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  report.browser = browser.version();
  try {
    const page = await browser.newPage({ viewport: { width: 1360, height: 1000 } });
    const errors = [];
    page.on('pageerror', e => errors.push(e.message));
    await page.goto(pathToFileURL(path.join(root, 'HTML首章样稿.html')).href);
    await check('验收页能离线打开，四块正文与学习数据数量正确', async () => {
      assert.equal(await page.locator('article > h2').count(), 4);
      assert.equal(await page.locator('article > h3').count(), 7);
      assert.equal(await page.locator('#cards details').count(), 6);
      assert.equal(await page.locator('#answers details').count(), 7);
      assert.equal(await page.locator('#exercise-answers details').count(), 2);
      assert.deepEqual(errors, []);
    });
    await check('知识卡片与答案能够展开，目录与本地文件链接有效', async () => {
      const first = page.locator('#cards details').first();
      assert.equal(await first.getAttribute('open'), null);
      await first.locator('summary').click();
      assert(await first.evaluate(el => el.open));
      assert(await first.locator('.answer').isVisible());
      const question = page.locator('#answers details').first();
      await question.locator('summary').click();
      assert(await question.locator('.answer').isVisible());
      const links = await page.locator('a').evaluateAll(els => els.map(a => a.getAttribute('href')));
      for (const href of links) {
        if (href.startsWith('#')) assert.equal(await page.locator(href).count(), 1, href);
        else if (!/^https?:/.test(href)) assert(fs.existsSync(path.join(root, href)), href);
      }
    });
    await page.evaluate(() => { document.documentElement.style.scrollBehavior = 'auto'; document.querySelectorAll('details').forEach(d => d.open = false); scrollTo(0, 0); });
    await page.screenshot({ path: path.join(root, '_authoring/HTML首章-桌面.png'), fullPage: false });
    await check('320px 与 390px 窄屏均无整页水平溢出', async () => {
      for (const width of [320, 390]) {
        await page.setViewportSize({ width, height: 844 });
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), String(width));
      }
      await page.screenshot({ path: path.join(root, '_authoring/HTML首章-手机.png'), fullPage: false });
    });
    await page.goto(pathToFileURL(path.join(chapterDir, 'examples/01-html-syntax/index.html')).href);
    await check('独立示例显示一个标题和两个段落，无额外样式脚本依赖', async () => {
      assert.equal(await page.locator('h1').textContent(), '我的第一张网页');
      assert.deepEqual(await page.locator('p').allTextContents(), ['这是一个段落。', '这是另一个段落。']);
      assert.equal(await page.locator('link[rel="stylesheet"],style,script').count(), 0);
      assert(!((await page.locator('body').innerText()).includes('note')));
    });
    await check('三组修改的实测结果与正文预期一致', async () => {
      const first = page.locator('p').first();
      const before = await first.evaluate(el => { const s = getComputedStyle(el); return { color: s.color, fontSize: s.fontSize, fontWeight: s.fontWeight }; });
      await first.evaluate(el => el.textContent = '我知道元素是什么了');
      assert.equal(await first.textContent(), '我知道元素是什么了');
      assert.equal(await page.locator('p').nth(1).textContent(), '这是另一个段落。');
      assert.equal(await page.locator('h1').textContent(), '我的第一张网页');
      await first.evaluate(el => el.className = 'warning');
      const after = await first.evaluate(el => { const s = getComputedStyle(el); return { color: s.color, fontSize: s.fontSize, fontWeight: s.fontWeight }; });
      assert.deepEqual(after, before);
      await first.evaluate(el => { const heading = document.createElement('h2'); for (const attr of el.attributes) heading.setAttribute(attr.name, attr.value); heading.textContent = el.textContent; el.replaceWith(heading); });
      assert.equal(await page.locator('h2').count(), 1);
      assert.equal(await page.locator('p').count(), 1);
      const headingSize = await page.locator('h2').evaluate(el => getComputedStyle(el).fontSize);
      assert.notEqual(headingSize, before.fontSize);
    });
    await page.goto(pathToFileURL(path.join(chapterDir, 'examples/01-html-syntax/exercise-answer.html')).href);
    await check('练习参考页面符合正文验收条件', async () => {
      assert.equal(await page.locator('h1').textContent(), '我的 HTML 学习记录');
      assert.deepEqual(await page.locator('p').allTextContents(), ['今天我认识了标签、元素和属性。', '下一步学习元素之间的关系。']);
      assert.equal(await page.locator('p').first().getAttribute('class'), 'learning-note');
      assert.equal(await page.locator('p').first().getAttribute('title'), '第一天的记录');
      assert.equal(await page.locator('style,link[rel="stylesheet"],script').count(), 0);
      const colors = await page.locator('p').evaluateAll(els => els.map(el => getComputedStyle(el).color));
      assert.equal(colors[0], colors[1]);
      assert(!((await page.locator('body').innerText()).includes('这一段记录当天所学')));
    });
  } finally { await browser.close(); }
}
main().catch(e => { report.failures.push(e.stack || String(e)); process.exitCode = 1; }).finally(() => {
  fs.writeFileSync(path.join(__dirname, 'HTML首章校验.json'), JSON.stringify(report, null, 2) + '\n', 'utf8');
  console.log(JSON.stringify(report));
});
