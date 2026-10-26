"""One-off, read-only public content migration. Never needed to build or serve."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.request import urlopen, Request
from urllib.parse import quote
import json, time

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / '.cache' / 'source'
CACHE.mkdir(parents=True, exist_ok=True)
failures = []

def read(name):
    return json.loads((CACHE / f'{name}.json').read_text(encoding='utf-8'))

def fetch(name, path):
    target = CACHE / f'{name}.json'
    if target.exists():
        return read(name)
    for attempt in range(3):
        try:
            request = Request('https://yulanlin.cn/api/public' + path, headers={'User-Agent': 'StarRainStaticMigration/1.0'})
            with urlopen(request, timeout=35) as response:
                result = json.load(response)
            if result.get('code') != 'SUCCESS':
                raise ValueError(result.get('message'))
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(json.dumps(result['data'], ensure_ascii=False, indent=2), encoding='utf-8')
            return result['data']
        except Exception as error:
            if attempt == 2:
                failures.append({'source': path, 'error': str(error)})
                return None
            time.sleep(attempt + 1)

def batch(jobs):
    done = 0
    with ThreadPoolExecutor(max_workers=8) as pool:
        futures = {pool.submit(fetch, name, path): name for name, path in jobs}
        for future in as_completed(futures):
            future.result()
            done += 1
            if done % 100 == 0 or done == len(jobs):
                print(f'Downloaded {done}/{len(jobs)} public documents', flush=True)

initial = {'home':'/site/home','site':'/site/config','blogs':'/blog/posts?page=1&pageSize=100','tags':'/blog/tags','topics':'/blog/topics','tutorials':'/tutorials?page=1&pageSize=100','categories':'/tutorial-categories/tree','works':'/portfolio/works?page=1&pageSize=100','profile':'/profile','messages':'/messages?page=1&pageSize=100','themes':'/english/vocabulary/themes','grammar':'/english/grammar'}
batch(list(initial.items()))
jobs = []
for post in read('blogs')['items']:
    jobs.append((f"blogs/{post['slug']}", '/blog/posts/' + quote(post['slug'])))
for work in read('works')['items']:
    jobs.append((f"works/{work['slug']}", '/portfolio/works/' + quote(work['slug'])))
for course in read('tutorials')['items']:
    jobs.append((f"courses/{course['slug']}", '/tutorials/' + quote(course['slug'])))
for theme in read('themes'):
    jobs.append((f"words/{theme['id']}-1", f"/english/vocabulary/words?themeId={theme['id']}&page=1&size=100&pageSize=100"))
batch(jobs)
jobs = []
for course in read('tutorials')['items']:
    detail = CACHE / 'courses' / f"{course['slug']}.json"
    if not detail.exists():
        continue
    for group in read(f"courses/{course['slug']}").get('groups', []):
        for chapter in group.get('chapters', []):
            jobs.append((f"chapters/{course['slug']}/{chapter['slug']}", f"/tutorials/{quote(course['slug'])}/chapters/{quote(chapter['slug'])}"))
for theme in read('themes'):
    data = read(f"words/{theme['id']}-1")
    for page in range(2, data['totalPages'] + 1):
        jobs.append((f"words/{theme['id']}-{page}", f"/english/vocabulary/words?themeId={theme['id']}&page={page}&size=100&pageSize=100"))
batch(jobs)
(ROOT / 'docs' / 'download-report.json').write_text(json.dumps({'date': '2026-10-21', 'failures': failures}, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Finished. Failed documents: {len(failures)}', flush=True)
if failures:
    raise SystemExit(1)
