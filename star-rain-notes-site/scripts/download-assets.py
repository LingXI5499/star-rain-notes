from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.request import urlopen, Request
from urllib.parse import urljoin, urlparse
import json, re, hashlib, shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / '.cache/source'
DEST = ROOT / 'public/images/content'
DEST.mkdir(parents=True, exist_ok=True)
shutil.copytree(ROOT.parent / 'frontend/public/brand', ROOT / 'public/brand', dirs_exist_ok=True)
urls = set()

def scan(value, key=''):
    if isinstance(value, dict):
        for k,v in value.items(): scan(v,k)
    elif isinstance(value, list):
        for v in value: scan(v,key)
    elif isinstance(value, str):
        value = re.sub(r'```[\s\S]*?```|~~~[\s\S]*?~~~|`[^`\n]*`', '', value)
        if key in ['coverUrl','url','avatarUrl'] and re.search(r'(/api/media/assets/\d+/content|\.(png|jpe?g|webp|gif|svg|avif)(\?|$))', value, re.I): urls.add(value)
        for u in re.findall(r'!\[[^\]]*\]\(([^\s)]+)', value): urls.add(u)
        for u in re.findall(r'<img\b[^>]*?src=[\'"]([^\'"]+)', value): urls.add(u)

for file in SOURCE.rglob('*.json'):
    if file.name not in ['asset-map.json','asset-report.json']: scan(json.loads(file.read_text(encoding='utf8')))

def download(url):
    absolute = urljoin('https://yulanlin.cn', url)
    if not absolute.startswith(('https://','http://')): return url,None,'Unsupported URL'
    digest = hashlib.sha256(absolute.encode()).hexdigest()[:16]
    existing = list(DEST.glob(digest+'.*'))
    if existing: return url,'/images/content/'+existing[0].name,None
    try:
        with urlopen(Request(absolute,headers={'User-Agent':'StarRainStaticMigration/1.0'}),timeout=30) as response:
            mime = response.headers.get_content_type()
            ext = {'image/png':'.png','image/jpeg':'.jpg','image/webp':'.webp','image/gif':'.gif','image/svg+xml':'.svg','image/avif':'.avif'}.get(mime)
            if not ext: raise ValueError('Not an image: '+mime)
            data = response.read(20*1024*1024+1)
            if len(data)>20*1024*1024: raise ValueError('Image exceeds 20MB')
        target = DEST / (digest+ext)
        target.write_bytes(data)
        if ext in ['.png','.jpg']:
            from PIL import Image
            with Image.open(target) as image:
                if image.width>1920: image.thumbnail((1920,1920))
                webp = target.with_suffix('.webp')
                image.save(webp,'WEBP',quality=86)
                target.unlink(); target=webp
        return url,'/images/content/'+target.name,None
    except Exception as error: return url,None,str(error)

mapping = {}; errors=[]
with ThreadPoolExecutor(max_workers=5) as pool:
    for url,local,error in pool.map(download,sorted(urls)):
        if local: mapping[url]=local
        else: errors.append({'url':url,'error':error})
(SOURCE/'asset-map.json').write_text(json.dumps(mapping,ensure_ascii=False,indent=2),encoding='utf8')
(ROOT/'docs/asset-report.json').write_text(json.dumps({'images':len(mapping),'failures':errors},ensure_ascii=False,indent=2),encoding='utf8')
print(f'Localized {len(mapping)} images. Failures: {len(errors)}',flush=True)
for error in errors: print(error,flush=True)
if errors: raise SystemExit(1)
