"""Align the local English library with reviewed static content; preview rolls back by default."""
import argparse
import datetime
import importlib.util
import json
import pathlib

import pymysql

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('content_sync', ROOT / 'tools/sync-static-content.py')
common = importlib.util.module_from_spec(spec)
spec.loader.exec_module(common)
rows, write, upsert = common.rows, common.write, common.upsert


def synchronize(connection, bundle):
    """Caller owns the transaction. Never delete words, private work, revisions or learning data."""
    result = {'taxonomyChanged': 0, 'wordsChanged': 0, 'mappingsChanged': 0, 'grammarChanged': 0,
              'worldWithdrawn': 0, 'essaysWithdrawn': 0, 'records': len(bundle['entries'])}
    terms = {}
    for module, dimension, key in [('vocabulary', 'VOCABULARY', 'vocabularyTaxonomy'), ('world', 'TOPIC', 'worldTaxonomy')]:
        active = []
        for category in bundle[key]['categories']:
            for node, parent in [(category, None), *((topic, category['code']) for topic in category['topics'])]:
                parent_id = terms[parent] if parent else None
                old = rows(connection, 'SELECT * FROM sr_english_taxonomy_term WHERE library_module=%s AND code=%s FOR UPDATE', (module, node['code']))
                if not old and module == 'world':
                    old = rows(connection, 'SELECT * FROM sr_english_taxonomy_term WHERE dimension=%s AND name=%s AND parent_id <=> %s AND library_module IS NULL FOR UPDATE', (dimension, node['title'], parent_id))
                if len(old) > 1:
                    raise ValueError('Ambiguous taxonomy identity: ' + node['code'])
                values = dict(parent_id=parent_id, dimension=dimension, library_module=module, code=node['code'],
                              name=node['title'], name_en=node.get('titleEn') or node['title'], enabled=1,
                              sort_order=node['order'], is_world_baseline=int(module == 'world'))
                if not old:
                    values['slug'] = module + '-' + node['code'].lower()
                tid, changed = upsert(connection, 'sr_english_taxonomy_term', old[0] if old else None, values)
                terms[node['code']] = tid
                active.append(tid)
                result['taxonomyChanged'] += changed
        # Disabled terms keep historical references; source taxonomy alone defines the new directory.
        marks = ','.join(['%s'] * len(active))
        write(connection, f'UPDATE sr_english_taxonomy_term SET enabled=0 WHERE dimension=%s AND id NOT IN ({marks})', (dimension, *active))

    existing = rows(connection, "SELECT w.* FROM sr_english_vocabulary_word w WHERE NOT EXISTS(SELECT 1 FROM sr_english_vocabulary_mapping m WHERE m.word_id=w.id AND m.entry_id LIKE 'manual-%%') FOR UPDATE")
    index = {}
    for word in existing:
        key = (str(word['theme_id']), word['sort_order'])
        if key in index:
            raise ValueError('Ambiguous source vocabulary position')
        index[key] = word
    for entry in bundle['entries']:
        source = entry['source']
        old = index.get((source['legacyThemeId'], source['legacyIndex'] + 1))
        if not old or old['word'] != entry['word']:
            raise ValueError('Source word identity requires manual review: ' + entry['entryId'])
        _, changed = upsert(connection, 'sr_english_vocabulary_word', old,
                            {**{key: entry[key] for key in ['word']},
                             'part_of_speech': entry['partOfSpeech'], 'phonetic_us': entry['phoneticUs'],
                             'phonetic_uk': entry['phoneticUk'], 'translation': entry['translation'],
                             'scene_meaning': entry['sceneMeaning'], 'examples': json.dumps(entry['examples'], ensure_ascii=False)})
        result['wordsChanged'] += changed
        mapped = rows(connection, 'SELECT word_id AS id, m.* FROM sr_english_vocabulary_mapping m WHERE word_id=%s FOR UPDATE', (old['id'],))
        values = dict(word_id=old['id'], entry_id=entry['entryId'], legacy_index=source['legacyIndex'],
                      migration_status=entry['migrationStatus'], migration_reason=entry['migrationReason'])
        if mapped:
            delta = {key: value for key, value in values.items() if mapped[0].get(key) != value}
            if delta:
                clause = ','.join(f'{key}=%s' for key in delta)
                write(connection, f'UPDATE sr_english_vocabulary_mapping SET {clause},row_version=row_version+1 WHERE word_id=%s', (*delta.values(), old['id']))
        else:
            columns = ','.join(values)
            write(connection, f"INSERT INTO sr_english_vocabulary_mapping({columns}) VALUES({','.join(['%s']*len(values))})", tuple(values.values()))
            delta = values
        previous = {row['term_id'] for row in rows(connection, 'SELECT term_id FROM sr_english_vocabulary_topic WHERE word_id=%s', (old['id'],))}
        expected = {terms[code] for code in entry['topicCodes']} if entry['migrationStatus'] == 'confirmed' else set()
        if previous != expected:
            write(connection, 'DELETE FROM sr_english_vocabulary_topic WHERE word_id=%s', (old['id'],))
            for term in expected:
                write(connection, 'INSERT INTO sr_english_vocabulary_topic(word_id,term_id) VALUES(%s,%s)', (old['id'], term))
        result['mappingsChanged'] += bool(delta or previous != expected)

    overview = rows(connection, 'SELECT * FROM sr_english_overview WHERE id=1 FOR UPDATE')
    if overview:
        upsert(connection, 'sr_english_overview', overview[0], bundle['overview'])
    course = rows(connection, 'SELECT * FROM sr_english_grammar_course WHERE id=1 FOR UPDATE')
    if course:
        upsert(connection, 'sr_english_grammar_course', course[0],
               {key: bundle['grammar'].get(key) for key in ['title', 'subtitle', 'summary', 'introduction']})
    for lesson in bundle['grammar']['lessons']:
        old = rows(connection, 'SELECT * FROM sr_english_grammar_lesson WHERE slug=%s FOR UPDATE', (lesson['slug'],))
        if len(old) != 1 or old[0]['publish_status'] != 'PUBLISHED':
            raise ValueError('Protected or missing grammar lesson: ' + lesson['slug'])
        _, changed = upsert(connection, 'sr_english_grammar_lesson', old[0],
                            dict(title=lesson['title'], summary=lesson['description'], body_markdown=lesson['bodyMarkdown']))
        result['grammarChanged'] += changed

    # This reviewed source has no published world articles or essays. Withdraw public examples;
    # retain all source bodies, rights, versions, private drafts and author ownership for recovery.
    if bundle['world'] or bundle['essays']:
        raise ValueError('New static articles require rights/ownership review before content import')
    world = rows(connection, "SELECT id FROM sr_english_reading_article WHERE publish_status='PUBLISHED' AND deleted_at IS NULL FOR UPDATE")
    essays = rows(connection, "SELECT id FROM sr_english_writing_article WHERE visibility='PUBLIC' AND deleted_at IS NULL FOR UPDATE")
    write(connection, "UPDATE sr_english_reading_article SET publish_status='WITHDRAWN',row_version=row_version+1 WHERE publish_status='PUBLISHED' AND deleted_at IS NULL")
    write(connection, "UPDATE sr_english_writing_article SET visibility='PRIVATE',published_at=NULL,row_version=row_version+1 WHERE visibility='PUBLIC' AND deleted_at IS NULL")
    result.update(worldWithdrawn=len(world), essaysWithdrawn=len(essays))
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--config', type=pathlib.Path, default=ROOT / 'backend/application-local-secret.yml')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    _, options = common.load_config(args.config)
    bundle = json.loads((ROOT / 'backend/content/static-english-content.json').read_text(encoding='utf-8'))
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    output = ROOT / 'history' / ('english-alignment-' + stamp)
    output.mkdir(parents=True)
    if args.apply:
        common.backup_database(options, output / 'database-before.sql')
    with pymysql.connect(**options) as connection:
        try:
            result = synchronize(connection, bundle)
            (output / 'alignment.json').write_text(json.dumps({'applied': args.apply, **result}, ensure_ascii=False, indent=2), encoding='utf-8')
            if args.apply:
                connection.commit()
            else:
                connection.rollback()
        except BaseException:
            connection.rollback()
            raise
    print(json.dumps({'applied': args.apply, **result}, ensure_ascii=False))


if __name__ == '__main__':
    main()
