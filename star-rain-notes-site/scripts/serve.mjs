import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createGzip } from 'node:zlib';
export function createStaticServer(root){
  const types={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json; charset=utf-8','.txt':'text/plain; charset=utf-8','.xml':'application/xml; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.webp':'image/webp','.jpg':'image/jpeg','.ico':'image/x-icon','.woff2':'font/woff2','.woff':'font/woff','.ttf':'font/ttf'};
  return http.createServer((req,res)=>{
    let pathname;try{pathname=decodeURIComponent(new URL(req.url,'http://localhost').pathname)}catch{res.writeHead(400);res.end();return;}
    let file=path.resolve(root,'.'+pathname);
    if(!file.startsWith(path.resolve(root)+path.sep)&&file!==path.resolve(root)){res.writeHead(403);res.end();return;}
    if(!['GET','HEAD'].includes(req.method)){res.writeHead(405);res.end();return;}
    if(fs.existsSync(file)&&fs.statSync(file).isDirectory()){
      if(!pathname.endsWith('/')){res.writeHead(301,{Location:pathname+'/'+new URL(req.url,'http://localhost').search});res.end();return;}
      file=path.join(file,'index.html');
    }
    let status=200;if(!fs.existsSync(file)||!fs.statSync(file).isFile()){status=404;file=path.join(root,'404.html');}
    if(!fs.existsSync(file)){res.writeHead(404);res.end('Not found');return;}
    const contentType=types[path.extname(file)]||'application/octet-stream';
    const compressed=/\bgzip\b/.test(req.headers['accept-encoding']||'')&&/^(text\/|application\/(javascript|json|xml))/.test(contentType);
    res.writeHead(status,{'Content-Type':contentType,'Cache-Control':'no-cache','X-Content-Type-Options':'nosniff','Vary':'Accept-Encoding',...(compressed?{'Content-Encoding':'gzip'}:{})});
    if(req.method==='HEAD')res.end();else if(compressed)fs.createReadStream(file).pipe(createGzip()).pipe(res);else fs.createReadStream(file).pipe(res);
  });
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../dist');
  if(!fs.existsSync(path.join(root,'index.html')))throw new Error('Run npm run build first');
  const port=Number(process.env.PORT||4321);
  createStaticServer(root).listen(port,'127.0.0.1',()=>console.log(`Static preview: http://127.0.0.1:${port}/ (dist only; no backend)`));
}
