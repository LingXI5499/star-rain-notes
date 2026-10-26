import test from 'node:test';
import assert from 'node:assert/strict';
import { validateMeta, validateHTML, isDate } from '../scripts/content-rules.mjs';
import { createStaticServer } from '../scripts/serve.mjs';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';

test('content gate rejects unsafe HTML even if a local author pastes it',()=>{
  const errors=validateHTML('<script>alert(1)</script><img src="https://bad.test/a.png" onerror="alert(1)"><a href="javascript:alert(1)">x</a>','unsafe');
  assert.ok(errors.some(e=>e.includes('executable')));assert.ok(errors.some(e=>e.includes('handler')));assert.ok(errors.some(e=>e.includes('unsafe href')));assert.ok(errors.some(e=>e.includes('local absolute')));
  assert.deepEqual(validateHTML('<h2 id="safe">说明</h2><p>正文与 <code>script</code> 示例。</p><img src="/images/safe.webp" alt="项目截图">','safe'),[]);
});
test('dates and public paths reject impossible dates and path traversal',()=>{
  assert.equal(isDate('2026-02-30'),false);assert.equal(isDate('2024-02-29'),true);
  assert.ok(validateMeta({title:'标题',description:'摘要',slug:'../private',path:'/../../private/',publishedAt:'2026-13-01'},'post').length>=3);
});
test('standalone static server returns 404, preserves query during redirect and refuses writes',async()=>{
  const root=fs.mkdtempSync(path.join(os.tmpdir(),'star-rain-static-'));
  fs.mkdirSync(path.join(root,'blog'));fs.writeFileSync(path.join(root,'index.html'),'<h1>首页</h1>');fs.writeFileSync(path.join(root,'blog/index.html'),'<h1>博客</h1>');fs.writeFileSync(path.join(root,'404.html'),'<h1>404</h1>');
  const server=createStaticServer(root);await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const base=`http://127.0.0.1:${server.address().port}`;
  try{const home=await fetch(base+'/',{headers:{'Accept-Encoding':'gzip'}});assert.equal(home.status,200);assert.equal(home.headers.get('content-encoding'),'gzip');assert.equal(await home.text(),'<h1>首页</h1>');const missing=await fetch(base+'/missing/');assert.equal(missing.status,404);assert.match(await missing.text(),/404/);const redirect=await fetch(base+'/blog?q=java',{redirect:'manual'});assert.equal(redirect.status,301);assert.equal(redirect.headers.get('location'),'/blog/?q=java');assert.equal((await fetch(base+'/',{method:'POST'})).status,405);assert.equal((await fetch(base+'/%E0%A4%A')).status,400);}finally{await new Promise(resolve=>server.close(resolve));fs.rmSync(root,{recursive:true,force:true});}
});
