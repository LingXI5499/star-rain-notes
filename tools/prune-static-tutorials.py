"""Remove tutorials absent from the reviewed static source; preview by default.

Requires the source bundle to have been synchronized first. --apply saves a full
local database backup. Historical learning events and frozen plan membership are
retained; affected active plans are cancelled using the normal cleanup semantics.
"""
import argparse
import datetime
import importlib.util
import json
import os
import pathlib
import subprocess

import pymysql

SPEC = importlib.util.spec_from_file_location('content_sync', pathlib.Path(__file__).with_name('sync-static-content.py'))
SYNC = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SYNC)


def plan_prune(db, bundle):
    tutorials = SYNC.rows(db, 'SELECT id,slug,title,editing_status FROM sr_tutorial ORDER BY id FOR UPDATE')
    chapters = SYNC.rows(db, 'SELECT id,tutorial_id,group_id,slug,title FROM sr_tutorial_chapter ORDER BY id FOR UPDATE')
    groups = SYNC.rows(db, 'SELECT id,tutorial_id,title FROM sr_tutorial_group ORDER BY id FOR UPDATE')
    desired = {c['slug']: c for c in bundle['courses']}
    by_slug = {t['slug']: t for t in tutorials}
    if set(desired) - set(by_slug):
        raise ValueError('Synchronize all source tutorials before pruning')
    keep_chapters, keep_groups = set(), set()
    for slug, course in desired.items():
        tutorial = by_slug[slug]
        if tutorial['editing_status'] == 'IN_REVIEW':
            raise ValueError('A source tutorial is in review')
        available = {c['slug']: c for c in chapters if c['tutorial_id'] == tutorial['id']}
        for group in course['groups']:
            if any(c['slug'] not in available for c in group['chapters']):
                raise ValueError('Synchronize all source chapters before pruning')
            members = [available[c['slug']] for c in group['chapters']]
            gids = {c['group_id'] for c in members}
            if not members:
                gids = {g['id'] for g in groups if g['tutorial_id'] == tutorial['id'] and g['title'] == group['title']}
            if len(gids) != 1:
                raise ValueError('Source group membership is missing or ambiguous')
            keep_groups.update(gids)
            keep_chapters.update(c['id'] for c in members)
    removed_tutorials = [t for t in tutorials if t['slug'] not in desired]
    if any(t['editing_status'] == 'IN_REVIEW' for t in removed_tutorials):
        raise ValueError('An unlisted tutorial is in review')
    removed_chapters = [c for c in chapters if c['id'] not in keep_chapters]
    removed_groups = [g for g in groups if g['id'] not in keep_groups]
    category_slugs = {c['slug'] for c in bundle['categories']}
    removed_categories = [c for c in SYNC.rows(db, 'SELECT id,slug,name FROM sr_tutorial_category ORDER BY id FOR UPDATE') if c['slug'] not in category_slugs]
    removed_tids = {t['id'] for t in removed_tutorials}
    changed = {c['tutorial_id'] for c in removed_chapters} | {g['tutorial_id'] for g in removed_groups}
    return dict(tutorials=removed_tutorials, chapters=removed_chapters, groups=removed_groups, categories=removed_categories,
                publishTutorialIds=sorted(changed - removed_tids), publishBlogIds=[])


def matches(column, ids):
    ids = sorted(set(ids))
    if not ids:
        return '1=0', ()
    return f'{column} IN (' + ','.join(['%s'] * len(ids)) + ')', tuple(ids)


def execute_prune(db, plan):
    tids = [r['id'] for r in plan['tutorials']]
    cids = [r['id'] for r in plan['chapters']]
    gids = [r['id'] for r in plan['groups']]
    chapter_clause, chapter_args = matches('chapter_id', cids)
    # No linked instructional exercises may be dropped silently. The current
    # source cleanup concerns outline-only chapters and has no such exercises.
    for table in ['sr_tutorial_knowledge_card', 'sr_tutorial_question']:
        if SYNC.rows(db, f'SELECT COUNT(*) n FROM {table} WHERE {chapter_clause}', chapter_args)[0]['n']:
            raise ValueError('An unlisted chapter contains linked cards or questions')
    tutorial_clause, tutorial_args = matches('p.tutorial_id', tids)
    member_clause, member_args = matches('pc.chapter_id', cids)
    scope_chapter, scope_chapter_args = matches('p.scope_id', cids)
    scope_group, scope_group_args = matches('p.scope_id', gids)
    affected_plans = SYNC.rows(db, f"SELECT p.id FROM sr_study_plan p WHERE {tutorial_clause} OR EXISTS(SELECT 1 FROM sr_study_plan_chapter pc WHERE pc.plan_id=p.id AND {member_clause}) OR (p.scope_type='CHAPTER' AND {scope_chapter}) OR (p.scope_type='GROUP' AND {scope_group}) FOR UPDATE",
                               tutorial_args + member_args + scope_chapter_args + scope_group_args)
    plan_clause, plan_args = matches('id', [r['id'] for r in affected_plans])
    SYNC.write(db, f"UPDATE sr_study_plan SET status='CANCELLED' WHERE status IN ('DRAFT','ACTIVE','PAUSED') AND {plan_clause}", plan_args)
    task_clause, task_args = matches('plan_id', [r['id'] for r in affected_plans])
    SYNC.write(db, f"UPDATE sr_study_task SET status='SKIPPED' WHERE status IN ('PENDING','IN_PROGRESS','TODO','OVERDUE') AND {task_clause}", task_args)
    rev_clause, rev_args = matches('tutorial_id', tids)
    revisions = SYNC.rows(db, f'SELECT id FROM sr_tutorial_revision WHERE {rev_clause}', rev_args)
    for source_type, ids in [('CHAPTER', cids), ('REVISION', [r['id'] for r in revisions])]:
        clause, args = matches('source_id', ids)
        SYNC.write(db, f"DELETE FROM sr_media_reference WHERE source_module='TUTORIAL' AND source_type=%s AND {clause}", (source_type, *args))
    for table, ids in [('sr_tutorial_chapter', cids), ('sr_tutorial_group', gids)]:
        clause, args = matches('id', ids)
        SYNC.write(db, f'DELETE FROM {table} WHERE {clause}', args)
    SYNC.write(db, f'DELETE FROM sr_tutorial_revision WHERE {rev_clause}', rev_args)
    for table, ids in [('sr_tutorial', tids), ('sr_tutorial_category', [r['id'] for r in plan['categories']])]:
        clause, args = matches('id', ids)
        SYNC.write(db, f'DELETE FROM {table} WHERE {clause}', args)
    return dict(deletedTutorials=len(tids), deletedChapters=len(cids), deletedGroups=len(gids),
                deletedCategories=len(plan['categories']), deletedRevisions=len(revisions), affectedPlans=len(affected_plans))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', default=str(SYNC.ROOT / 'backend/application-local-secret.yml'))
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    _, options = SYNC.load_config(args.config)
    bundle = json.loads(SYNC.BUNDLE.read_text(encoding='utf-8'))
    db = pymysql.connect(**options)
    try:
        plan = plan_prune(db, bundle)
        counts = execute_prune(db, plan)
        db.rollback()
        print(json.dumps({'mode': 'preview', **counts, 'tutorials': plan['tutorials'], 'categories': plan['categories']}, ensure_ascii=False), flush=True)
        if not args.apply:
            return
        record = SYNC.ROOT / 'history' / ('tutorial-prune-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
        record.mkdir(parents=True, exist_ok=False)
        SYNC.backup_database(options, record / 'database-before.sql')
        plan = plan_prune(db, bundle)
        counts = execute_prune(db, plan)
        owner = SYNC.rows(db, "SELECT a.id FROM sr_account a JOIN sr_account_role ar ON ar.account_id=a.id JOIN sr_role r ON r.id=ar.role_id WHERE r.code='SUPER_ADMIN' ORDER BY a.id LIMIT 1")[0]['id']
        plan.update(ownerAccountId=owner, targetDatabase=options['database'])
        db.commit()
        plan_file = record / 'pending-publication.json'
        plan_file.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding='utf-8')
        command = ['powershell.exe', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(SYNC.ROOT / 'backend/tools/publish-static-content.ps1'), '-Plan', str(plan_file), '-Config', str(pathlib.Path(args.config).resolve())]
        with (record / 'publication.log').open('w', encoding='utf-8') as log:
            result = subprocess.run(command, cwd=SYNC.ROOT / 'backend', stdout=log, stderr=subprocess.STDOUT,
                                    creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
        if result.returncode:
            raise RuntimeError('Publication failed; the backup and pending plan are retained')
        counts.update(json.loads((record / 'publication-result.json').read_text(encoding='utf-8')))
        (record / 'result.json').write_text(json.dumps(counts, ensure_ascii=False, indent=2), encoding='utf-8')
        print(json.dumps({'mode': 'applied', **counts}, ensure_ascii=False), flush=True)
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


if __name__ == '__main__':
    main()
