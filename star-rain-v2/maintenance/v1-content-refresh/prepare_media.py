"""Recover the V1 dump's public PNGs from its documented Star Rain demo origin.

Writes only beneath the local media directory and the ignored migration workspace.
The SQL dump contains asset metadata, not the image bytes themselves.
"""
import concurrent.futures, hashlib, json, struct, urllib.parse, urllib.request
from refresh import PROJECT, SOURCE, rows

def main():
    records=rows('media_asset',SOURCE,['id','public_url','storage_path','size_bytes'],"asset_type='IMAGE'")
    projects=rows('portfolio_project',SOURCE,['title','demo_url'],"title='星雨笔录'")
    assert len(projects)==1 and projects[0]['demo_url'].rstrip('/')=='https://yulanlin.cn'
    root=(PROJECT/'media').resolve()
    def recover(r):
        key='v1-import/'+r['storage_path'];path=(root/key).resolve();assert path.is_relative_to(root)
        url=urllib.parse.urljoin('https://yulanlin.cn/',r['public_url']);assert urllib.parse.urlparse(url).hostname=='yulanlin.cn'
        with urllib.request.urlopen(url,timeout=20) as response:
            assert urllib.parse.urlparse(response.url).hostname=='yulanlin.cn'
            data=response.read(16*1024*1024+1)
        assert len(data)==r['size_bytes'] and data[:8]==b'\x89PNG\r\n\x1a\n'
        width,height=struct.unpack('>II',data[16:24]);digest=hashlib.sha256(data).hexdigest()
        if path.exists():assert hashlib.sha256(path.read_bytes()).hexdigest()==digest
        else:path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
        return dict(sourceId=r['id'],storageKey=key,sha256=digest,sizeBytes=len(data),width=width,height=height)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:manifest=list(pool.map(recover,records))
    output=PROJECT/'.local/v1-content-refresh-20261009/media-manifest.json';output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(dict(images=len(manifest),bytes=sum(x['sizeBytes'] for x in manifest),uniqueHashes=len({x['sha256'] for x in manifest}))))

if __name__=='__main__':main()
