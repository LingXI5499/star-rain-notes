"""Synchronize reviewed static portfolio content into a local application database.

Preview rolls back by default. Apply saves a complete database backup and a
publication plan under history; public state is handled by the existing service.
"""
import argparse
import datetime
import hashlib
import importlib.util
import json
import pathlib
import re
import shutil

import pymysql
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('static_content', ROOT / 'tools/sync-static-content.py')
common = importlib.util.module_from_spec(spec)
spec.loader.exec_module(common)
rows, write, upsert = common.rows, common.write, common.upsert


def synchronize(connection, bundle, owner, copy_assets=False, media_root=None):
    media_root = (media_root or ROOT / 'media').resolve()
    if not media_root.is_relative_to(ROOT):
        raise ValueError('Media storage must stay inside the project')
    result = dict(worksCreated=0, worksChanged=0, sectionsChanged=0, mediaCreated=0,
                  publishPortfolioIds=[], portfolioPublishedAt={}, ownerAccountId=owner)
    serialized = json.dumps(bundle, ensure_ascii=False)
    urls = sorted(set(re.findall(r'/images/content/[\w.-]+', serialized)))
    assets = {}
    for url in urls:
        source = (ROOT / 'star-rain-notes-site/public' / url.lstrip('/')).resolve()
        if not source.is_relative_to(ROOT / 'star-rain-notes-site/public/images/content'):
            raise ValueError('Image must stay in the reviewed public content directory')
        content = source.read_bytes()
        digest = hashlib.sha256(content).hexdigest()
        key = 'static-portfolio/' + source.name
        destination = (media_root / key).resolve()
        if destination.exists() and destination.read_bytes() != content:
            raise ValueError('Conflicting local media file')
        if copy_assets and not destination.exists():
            destination.parent.mkdir(parents=True, exist_ok=True)
            with source.open('rb') as src, destination.open('xb') as dest:
                shutil.copyfileobj(src, dest)
        existing = rows(connection, 'SELECT * FROM sr_media_asset WHERE storage_key=%s FOR UPDATE', (key,))
        if existing and (existing[0]['sha256'] != digest or existing[0]['access_level'] != 'PUBLIC'
                         or existing[0]['status'] != 'ACTIVE'):
            raise ValueError('Protected or conflicting media record')
        with Image.open(source) as image:
            width, height = image.size
        aid, changed = upsert(connection, 'sr_media_asset', existing[0] if existing else None,
                             dict(original_name=source.name, media_type='IMAGE', mime_type='image/webp',
                                  file_extension='webp', size_bytes=len(content), sha256=digest,
                                  storage_provider='LOCAL', storage_key=key, width=width, height=height,
                                  access_level='PUBLIC', status='ACTIVE', uploaded_by_account_id=owner))
        result['mediaCreated'] += not existing
        assets[url] = aid

    def localize(text):
        for url, aid in assets.items():
            text = text.replace(url, f'/api/media/assets/{aid}/content')
        return text

    def reference(table, existing, values, source_type, usage):
        mid, changed = upsert(connection, table, existing, values)
        if existing and existing['media_asset_id'] != values['media_asset_id']:
            write(connection, 'DELETE FROM sr_media_reference WHERE source_module=%s AND source_type=%s AND source_id=%s',
                  ('PORTFOLIO', source_type, mid))
        bound = rows(connection, 'SELECT id FROM sr_media_reference WHERE media_asset_id=%s AND source_module=%s AND source_type=%s AND source_id=%s AND usage_code=%s',
                     (values['media_asset_id'], 'PORTFOLIO', source_type, mid, usage))
        if not bound:
            write(connection, 'INSERT INTO sr_media_reference (media_asset_id,source_module,source_type,source_id,usage_code) VALUES (%s,%s,%s,%s,%s)',
                  (values['media_asset_id'], 'PORTFOLIO', source_type, mid, usage))
        return changed

    for index, work in enumerate(bundle['works']):
        old = rows(connection, 'SELECT * FROM sr_portfolio_work WHERE slug=%s FOR UPDATE', (work['slug'],))
        previous = old[0] if old else None
        if previous and previous['status'] != 'PUBLISHED':
            raise ValueError('Existing unpublished work is protected: ' + work['slug'])
        category = rows(connection, 'SELECT id FROM sr_portfolio_category WHERE name=%s', (work['category'],))
        form = rows(connection, 'SELECT id FROM sr_portfolio_format WHERE name=%s', (work['format'],))
        if len(category) != 1 or len(form) != 1:
            raise ValueError('Unknown or ambiguous work taxonomy')
        values = dict(slug=work['slug'], title=work['title'], summary=work['description'],
                      category_id=category[0]['id'], format_id=form[0]['id'],
                      tech_stack=', '.join(work['techStack']), project_status=work['status'],
                      started_on=datetime.date.fromisoformat(work['startedOn']) if work['startedOn'] else None,
                      ended_on=datetime.date.fromisoformat(work['endedOn']) if work['endedOn'] else None,
                      featured=work['featured'], sort_order=(index+1)*10,
                      body_markdown=localize(work['bodyMarkdown']))
        if not previous:
            values.update(work_type='SOFTWARE', status='DRAFT', created_by_account_id=owner,
                          updated_by_account_id=owner)
        wid, changed = upsert(connection, 'sr_portfolio_work', previous, values, version=bool(previous))
        result['worksCreated'] += not previous
        detail = rows(connection, 'SELECT * FROM sr_portfolio_work_detail WHERE work_id=%s', (wid,))
        detail_value = json.dumps(dict(role=previous['role'] if previous else None,
                                      techStack=work['techStack'], projectStage=work['status']), ensure_ascii=False)
        if not detail or json.loads(detail[0]['detail_json']) != json.loads(detail_value):
            upsert(connection, 'sr_portfolio_work_detail', detail[0] if detail else None,
                   dict(work_id=wid, work_type=previous['work_type'] if previous else 'SOFTWARE', detail_json=detail_value))
            changed = True
        media = rows(connection, 'SELECT * FROM sr_portfolio_media WHERE work_id=%s ORDER BY sort_order,id', (wid,))
        desired_media = [('COVER', work['cover'], work['title'])] + [('SCREENSHOT', x['src'], x.get('alt', '')) for x in work['screenshots']]
        for order, (usage, url, caption) in enumerate(desired_media):
            matches = [x for x in media if x['usage_type'] == usage]
            position = 0 if usage == 'COVER' else order - 1
            existing = matches[position] if position < len(matches) else None
            changed |= reference('sr_portfolio_media', existing,
                                 dict(work_id=wid, media_asset_id=assets[url], usage_type=usage,
                                      caption=caption, sort_order=0 if usage == 'COVER' else order),
                                 'WORK_MEDIA', 'portfolio.' + usage.lower())
        links = rows(connection, 'SELECT * FROM sr_portfolio_link WHERE work_id=%s', (wid,))
        for order, link in enumerate(work['links']):
            if not re.match(r'^https?://', link['url']):
                raise ValueError('Unsupported public work link')
            matches = [x for x in links if x['url'] == link['url']]
            _, dirty = upsert(connection, 'sr_portfolio_link', matches[0] if matches else None,
                              dict(work_id=wid, link_type=link['type'], label=link['label'], url=link['url'],
                                   sort_order=order*10, status='ENABLED'))
            changed |= dirty
        sections = rows(connection, 'SELECT * FROM sr_portfolio_section WHERE work_id=%s', (wid,))
        for order, section in enumerate(work['sections']):
            matches = [x for x in sections if x['title'] == section['title'] and x['section_type'] == section['type']]
            if len(matches) > 1:
                raise ValueError('Ambiguous existing work section')
            data_json = json.dumps(section['data'], ensure_ascii=False, sort_keys=True)
            existing = matches[0] if matches else None
            if existing and json.loads(existing['data_json']) == section['data']:
                data_json = existing['data_json']
            sid, dirty = upsert(connection, 'sr_portfolio_section', existing,
                                dict(work_id=wid, section_type=section['type'], title=section['title'],
                                     content=localize(section['content']), data_json=data_json,
                                     block_version=1, visible=True, sort_order=order*10))
            result['sectionsChanged'] += dirty
            changed |= dirty
            attached = rows(connection, 'SELECT * FROM sr_portfolio_section_media WHERE section_id=%s', (sid,))
            for media_order, image in enumerate(section['media']):
                aid = assets[image['src']]
                match = next((x for x in attached if x['media_asset_id'] == aid), None)
                changed |= reference('sr_portfolio_section_media', match,
                                     dict(section_id=sid, media_asset_id=aid, caption=image.get('caption', ''), sort_order=media_order*10),
                                     'WORK_SECTION_MEDIA', 'portfolio.section')
        if changed:
            result['worksChanged'] += 1
        if not previous:
            result['publishPortfolioIds'].append(wid)
            result['portfolioPublishedAt'][str(wid)] = work['publishedAt']
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', default=str(ROOT / 'backend/application-local-secret.yml'))
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    config, options = common.load_config(args.config)
    media_root = pathlib.Path(config.get('STAR_RAIN_MEDIA_ROOT') or ROOT / 'media')
    bundle = json.loads((ROOT / 'backend/content/static-portfolio-content.json').read_text(encoding='utf-8'))
    connection = pymysql.connect(**options)
    try:
        owner = rows(connection, "SELECT a.id FROM sr_account a JOIN sr_account_role ar ON ar.account_id=a.id JOIN sr_role r ON r.id=ar.role_id WHERE r.code='SUPER_ADMIN' AND a.status='ACTIVE' ORDER BY a.id LIMIT 1")
        if not owner:
            raise ValueError('No existing active owner')
        preview = synchronize(connection, bundle, owner[0]['id'], media_root=media_root)
        connection.rollback()
        print(json.dumps({'mode': 'preview', **preview}), flush=True)
        if not args.apply:
            return
        directory = ROOT / 'history' / ('portfolio-sync-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
        directory.mkdir(parents=True)
        common.backup_database(options, directory / 'database-before.sql')
        result = synchronize(connection, bundle, owner[0]['id'], copy_assets=True, media_root=media_root)
        result['targetDatabase'] = options['database']
        connection.commit()
        (directory / 'pending-publication.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
        print(json.dumps({'mode': 'prepared', 'plan': str(directory / 'pending-publication.json'), **result}), flush=True)
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == '__main__':
    main()
