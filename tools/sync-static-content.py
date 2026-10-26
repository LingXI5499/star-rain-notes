"""Synchronize the reviewed bundle into a local application database.

Preview is the default. Workspace writes form one transaction; a separate local
maintenance process calls the existing publication, search and SEO services.
"""
import argparse
import datetime
import hashlib
import json
import os
import pathlib
import re
import subprocess

import pymysql
import yaml

ROOT = pathlib.Path(__file__).resolve().parents[1]
BUNDLE = ROOT / 'backend/content/static-site-content.json'


def load_config(file):
    file = pathlib.Path(file).resolve()
    if not file.is_relative_to(ROOT):
        raise ValueError('Configuration must be inside the project')
    config = yaml.safe_load(file.read_text(encoding='utf-8-sig'))
    match = re.fullmatch(r'jdbc:mysql://(localhost|127\.0\.0\.1)(?::(\d+))?/([\w-]+)(?:\?.*)?', config['STAR_RAIN_DB_URL'])
    if not match:
        raise ValueError('Only an explicitly configured local MySQL database is supported')
    return config, dict(host=match[1], port=int(match[2] or 3306), database=match[3],
                        user=config['STAR_RAIN_DB_USERNAME'], password=str(config['STAR_RAIN_DB_PASSWORD']),
                        charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor, autocommit=False)


def rows(connection, query, args=()):
    with connection.cursor() as cursor:
        cursor.execute(query, args)
        return cursor.fetchall()


def write(connection, query, args=()):
    with connection.cursor() as cursor:
        cursor.execute(query, args)
        return cursor.lastrowid


def upsert(connection, table, existing, values, version=False):
    if existing:
        delta = {key: value for key, value in values.items() if existing.get(key) != value}
        if not delta:
            return existing['id'], False
        clause = ', '.join(f'`{key}`=%s' for key in delta)
        if version:
            clause += ', row_version=row_version+1'
        write(connection, f'UPDATE `{table}` SET {clause} WHERE id=%s', (*delta.values(), existing['id']))
        return existing['id'], True
    columns = ','.join(f'`{key}`' for key in values)
    placeholders = ','.join('%s' for _ in values)
    return write(connection, f'INSERT INTO `{table}` ({columns}) VALUES ({placeholders})', tuple(values.values())), True


def synchronize(connection, bundle, account_id):
    """Caller owns transaction boundaries. Existing IDs and unlisted rows survive."""
    result = {'categoriesChanged': 0, 'coursesCreated': 0, 'coursesChanged': 0,
              'groupsChanged': 0, 'chaptersCreated': 0, 'chaptersChanged': 0,
              'blogsCreated': 0, 'blogsChanged': 0, 'taxonomyChanged': 0,
              'tagAliases': {}, 'publishTutorialIds': [], 'publishBlogIds': []}
    category_ids = {}
    for index, category in enumerate(bundle['categories']):
        old = rows(connection, 'SELECT * FROM sr_tutorial_category WHERE slug=%s FOR UPDATE', (category['slug'],))
        cid, changed = upsert(connection, 'sr_tutorial_category', old[0] if old else None,
                              dict(slug=category['slug'], name=category['name'], sort_order=(index+1)*10))
        category_ids[category['slug']] = cid
        result['categoriesChanged'] += changed
    for course_index, course in enumerate(bundle['courses']):
        old = rows(connection, 'SELECT * FROM sr_tutorial WHERE slug=%s FOR UPDATE', (course['slug'],))
        previous = old[0] if old else None
        if previous and (previous['editing_status'] == 'IN_REVIEW' or previous['publication_status'] != 'PUBLISHED'):
            raise ValueError(f"Protected unpublished/review tutorial: {course['slug']}")
        values = dict(category_id=category_ids[course['categorySlug']], slug=course['slug'], title=course['title'],
                      summary=course['description'], sort_order=(course_index+1)*10)
        if not previous:
            values.update(created_by_account_id=account_id, updated_by_account_id=account_id)
        tid, dirty = upsert(connection, 'sr_tutorial', previous, values)
        result['coursesCreated'] += not previous
        result['coursesChanged'] += dirty
        current_chapters = {c['slug']: c for c in rows(connection, 'SELECT * FROM sr_tutorial_chapter WHERE tutorial_id=%s FOR UPDATE', (tid,))}
        current_groups = rows(connection, 'SELECT * FROM sr_tutorial_group WHERE tutorial_id=%s FOR UPDATE', (tid,))
        # Match group titles first, then unambiguously match by the chapters they
        # already contain. Renaming a group does not duplicate its old contents.
        used_groups = set()
        for group_index, group in enumerate(course['groups']):
            matches = [g for g in current_groups if g['title'] == group['title'] and g['id'] not in used_groups]
            if not matches:
                ids = {current_chapters[c['slug']]['group_id'] for c in group['chapters'] if c['slug'] in current_chapters}
                matches = [g for g in current_groups if g['id'] in ids and g['id'] not in used_groups]
                if len(matches) > 1:
                    matches = []
            gid, changed = upsert(connection, 'sr_tutorial_group', matches[0] if matches else None,
                                  dict(tutorial_id=tid, title=group['title'], sort_order=(group_index+1)*10, status='ACTIVE'))
            used_groups.add(gid)
            result['groupsChanged'] += changed
            dirty |= changed
            for chapter_index, chapter in enumerate(group['chapters']):
                previous_chapter = current_chapters.get(chapter['slug'])
                if previous_chapter and previous_chapter['status'] != 'PUBLISHED':
                    raise ValueError(f"Protected draft/withdrawn chapter: {course['slug']}/{chapter['slug']}")
                _, changed = upsert(connection, 'sr_tutorial_chapter', previous_chapter,
                                     dict(tutorial_id=tid, group_id=gid, slug=chapter['slug'], title=chapter['title'],
                                          summary=chapter['description'], body_markdown=chapter['bodyMarkdown'],
                                          sort_order=(chapter_index+1)*10, status='PUBLISHED'), version=True)
                result['chaptersCreated'] += previous_chapter is None
                result['chaptersChanged'] += changed
                dirty |= changed
        if dirty:
            write(connection, 'UPDATE sr_tutorial SET updated_by_account_id=%s WHERE id=%s', (account_id, tid))
            result['publishTutorialIds'].append(tid)
    for blog in bundle['blogs']:
        old = rows(connection, 'SELECT * FROM sr_blog_post WHERE slug=%s FOR UPDATE', (blog['slug'],))
        previous = old[0] if old else None
        if previous and previous['status'] != 'PUBLISHED':
            raise ValueError(f"Protected unpublished blog: {blog['slug']}")
        values = dict(slug=blog['slug'], title=blog['title'], summary=blog['description'], body_markdown=blog['bodyMarkdown'])
        if not previous:
            values.update(created_by_account_id=account_id, updated_by_account_id=account_id,
                          published_at=datetime.datetime.fromisoformat(blog['publishedAt']), status='DRAFT')
        pid, dirty = upsert(connection, 'sr_blog_post', previous, values, version=True)
        result['blogsCreated'] += previous is None
        result['blogsChanged'] += dirty
        for kind in ['tags', 'topics']:
            table = 'sr_blog_tag' if kind == 'tags' else 'sr_blog_topic'
            relation, foreign = ('sr_blog_post_tag', 'tag_id') if kind == 'tags' else ('sr_blog_topic_post', 'topic_id')
            for taxon in blog[kind]:
                old_taxon = rows(connection, f'SELECT * FROM {table} WHERE slug=%s FOR UPDATE', (taxon['slug'],))
                if not old_taxon and kind == 'tags':
                    old_taxon = rows(connection, f'SELECT * FROM {table} WHERE name=%s FOR UPDATE', (taxon['name'],))
                taxon_values = dict(name=taxon['name'], status='ENABLED')
                if kind == 'tags' and old_taxon and old_taxon[0]['slug'] != taxon['slug']:
                    # The application's unique tag-name collation merges case
                    # variants. Keep its existing canonical label and stable URL.
                    taxon_values['name'] = old_taxon[0]['name']
                    result['tagAliases'][taxon['slug']] = old_taxon[0]['slug']
                if not old_taxon:
                    taxon_values['slug'] = taxon['slug']
                taxon_id, changed = upsert(connection, table, old_taxon[0] if old_taxon else None, taxon_values)
                result['taxonomyChanged'] += changed
                bound = rows(connection, f'SELECT id FROM {relation} WHERE post_id=%s AND {foreign}=%s', (pid, taxon_id))
                if not bound:
                    write(connection, f'INSERT INTO {relation} (post_id,{foreign}) VALUES (%s,%s)', (pid, taxon_id))
                    result['taxonomyChanged'] += 1
                    dirty = True
        if dirty:
            write(connection, 'UPDATE sr_blog_post SET updated_by_account_id=%s WHERE id=%s', (account_id, pid))
            if not previous:
                result['publishBlogIds'].append(pid)
    return result


def backup_database(options, output):
    # A complete, restorable backup stays local and is never added to the bundle.
    binary = os.environ.get('STAR_RAIN_MYSQLDUMP')
    if not binary or not pathlib.Path(binary).is_file():
        raise ValueError('Set STAR_RAIN_MYSQLDUMP to the existing mysqldump executable')
    env = os.environ.copy()
    env['MYSQL_PWD'] = options['password']
    args = [binary, '--no-defaults', '--single-transaction', '--routines', '--triggers', '--hex-blob',
            '--set-gtid-purged=OFF', '--no-tablespaces', '--default-character-set=utf8mb4',
            f"--host={options['host']}", f"--port={options['port']}", f"--user={options['user']}",
            f'--result-file={output}', options['database']]
    completed = subprocess.run(args, env=env, capture_output=True)
    if completed.returncode or not output.exists() or output.stat().st_size == 0:
        raise RuntimeError('Local database backup failed; no content was changed')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', default=str(ROOT / 'backend/application-local-secret.yml'))
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    config, options = load_config(args.config)
    bundle = json.loads(BUNDLE.read_text(encoding='utf-8'))
    connection = pymysql.connect(**options)
    try:
        # Preflight rows are rolled back. MySQL can leave unused auto-increment IDs.
        owner = rows(connection, "SELECT a.id FROM sr_account a JOIN sr_account_role ar ON ar.account_id=a.id JOIN sr_role r ON r.id=ar.role_id WHERE r.code='SUPER_ADMIN' ORDER BY a.id LIMIT 1")
        if not owner:
            raise ValueError('No existing owner account')
        plan = synchronize(connection, bundle, owner[0]['id'])
        connection.rollback()
        print(json.dumps({'mode': 'preview', **plan}, ensure_ascii=False), flush=True)
        if not args.apply:
            return
        record = ROOT / 'history' / ('content-sync-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
        record.mkdir(parents=True, exist_ok=False)
        backup_database(options, record / 'database-before.sql')
        result = synchronize(connection, bundle, owner[0]['id'])
        result['ownerAccountId'] = owner[0]['id']
        result['targetDatabase'] = options['database']
        connection.commit()
        plan_file = record / 'pending-publication.json'
        plan_file.write_text(json.dumps(result, indent=2), encoding='utf-8')
        command = ['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
                   str(ROOT / 'backend/tools/publish-static-content.ps1'), '-Plan', str(plan_file),
                   '-Config', str(pathlib.Path(args.config).resolve())]
        with (record / 'publication.log').open('w', encoding='utf-8') as log:
            completed = subprocess.run(command, cwd=ROOT / 'backend', stdout=log, stderr=subprocess.STDOUT,
                                       creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
        if completed.returncode:
            raise RuntimeError('Publication failed; workspace data and the backup are retained. See the local publication log and pending plan.')
        result.update(json.loads((record / 'publication-result.json').read_text(encoding='utf-8')))
        result['bundleSha256'] = hashlib.sha256(BUNDLE.read_bytes()).hexdigest()
        (record / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
        print(json.dumps({'mode': 'applied', **result}, ensure_ascii=False), flush=True)
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


if __name__ == '__main__':
    main()
