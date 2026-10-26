from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib, json
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artifacts'
OUT.mkdir(exist_ok=True)
report=json.loads((ROOT/'docs/build-verification.json').read_text(encoding='utf8'))
if report['status']!='PASS': raise SystemExit('Build verification must pass before packaging')
excluded={'node_modules','dist','.cache','.astro','.git','artifacts'}
local_scripts={'gen_tutorials.py','parse_algorithm.py','parse_network.py','set_tutorial_order.py','split_enterprise.py'}
local_reports={'reference-style-manifest.json','runtime-verification.json','asset-report.json','download-report.json'}
for kind in ['static','source']:
    target=OUT/f'star-rain-notes-site-{kind}.zip'
    with ZipFile(target,'w',ZIP_DEFLATED,compresslevel=7) as archive:
        if kind=='static':
            for file in (ROOT/'dist').rglob('*'):
                if file.is_file(): archive.write(file,file.relative_to(ROOT/'dist'))
        else:
            for file in ROOT.rglob('*'):
                rel=file.relative_to(ROOT)
                if any(part in excluded for part in rel.parts):continue
                if rel.parts[0]=='scripts' and file.name in local_scripts:continue
                if rel.parts[0]=='docs' and (file.name.startswith('lighthouse-') or file.name in local_reports or file.suffix in {'.jpg','.png'}):continue
                if file.name.startswith('.env'):continue
                if file.is_file() and file.suffix!='.log': archive.write(file,Path('star-rain-notes-site')/rel)
    with ZipFile(target) as archive:
        bad=archive.testzip()
        if bad: raise SystemExit(f'Corrupt archive member: {bad}')
    print(f'{target}: {target.stat().st_size:,} bytes; integrity PASS')
hashes=''.join(hashlib.sha256(file.read_bytes()).hexdigest()+'  '+file.name+'\n' for file in sorted(OUT.glob('*.zip')))
(OUT/'SHA256SUMS.txt').write_text(hashes,encoding='utf8')
