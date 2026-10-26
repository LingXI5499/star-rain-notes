"""Integration checks against an already synchronized, independent test database."""
import importlib.util
import json
import os
import pathlib
import unittest
import hashlib

import pymysql

SPEC = importlib.util.spec_from_file_location('content_sync', pathlib.Path(__file__).with_name('sync-static-content.py'))
SYNC = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SYNC)


class ContentSyncIntegrationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        _, options = SYNC.load_config(os.environ['STAR_RAIN_CONTENT_TEST_CONFIG'])
        if not options['database'].startswith('star_rain_content_sync_test_'):
            raise ValueError('Tests require a dedicated content-sync test database')
        cls.db = pymysql.connect(**options)
        cls.bundle = json.loads(SYNC.BUNDLE.read_text(encoding='utf-8'))

    @classmethod
    def tearDownClass(cls):
        cls.db.close()

    def tearDown(self):
        self.db.rollback()

    def test_every_source_chapter_is_in_workspace_and_immutable_public_version(self):
        count = 0
        for course in self.bundle['courses']:
            row = SYNC.rows(self.db, 'SELECT t.id,r.snapshot_json FROM sr_tutorial t JOIN sr_tutorial_revision r ON r.id=t.published_revision_id WHERE t.slug=%s', (course['slug'],))[0]
            snapshot = json.loads(row['snapshot_json'])
            published = {c['slug']: c for g in snapshot['groups'] for c in g['chapters']}
            workspace = {c['slug']: c for c in SYNC.rows(self.db, 'SELECT slug,title,body_markdown FROM sr_tutorial_chapter WHERE tutorial_id=%s', (row['id'],))}
            self.assertEqual(course['title'], snapshot['title'])
            self.assertEqual(course['category'], snapshot['categoryName'])
            for group in course['groups']:
                for chapter in group['chapters']:
                    with self.subTest(course=course['slug'], chapter=chapter['slug']):
                        self.assertEqual(chapter['bodyMarkdown'], workspace[chapter['slug']]['body_markdown'])
                        self.assertEqual(chapter['bodyMarkdown'], published[chapter['slug']]['bodyMarkdown'])
                        self.assertEqual(chapter['title'], published[chapter['slug']]['title'])
                        self.assertEqual(str(next(c['id'] for c in SYNC.rows(self.db, 'SELECT id FROM sr_tutorial_chapter WHERE tutorial_id=%s AND slug=%s', (row['id'], chapter['slug'])))), published[chapter['slug']]['id'])
                        count += 1
        self.assertEqual(2659, count)

    def test_blogs_are_published_with_source_text_and_taxonomy(self):
        for blog in self.bundle['blogs']:
            row = SYNC.rows(self.db, 'SELECT id,title,body_markdown,status FROM sr_blog_post WHERE slug=%s', (blog['slug'],))[0]
            self.assertEqual('PUBLISHED', row['status'])
            self.assertEqual(blog['bodyMarkdown'], row['body_markdown'])
            self.assertEqual(blog['title'], row['title'])
            for taxon in blog['tags']:
                tags = SYNC.rows(self.db, 'SELECT t.name FROM sr_blog_tag t JOIN sr_blog_post_tag pt ON pt.tag_id=t.id WHERE pt.post_id=%s', (row['id'],))
                self.assertIn(taxon['name'].casefold(), [t['name'].casefold() for t in tags])

    def test_repeated_sync_is_noop(self):
        result = SYNC.synchronize(self.db, self.bundle, 1)
        for key, value in result.items():
            if key == 'tagAliases':
                continue
            self.assertEqual([] if key.startswith('publish') else 0, value, key)

    def test_review_conflict_rolls_back_all_workspace_changes(self):
        first_slug = self.bundle['courses'][0]['slug']
        before = SYNC.rows(self.db, 'SELECT name FROM sr_tutorial_category ORDER BY id')
        SYNC.write(self.db, "UPDATE sr_tutorial SET editing_status='IN_REVIEW' WHERE slug=%s", (first_slug,))
        broken = json.loads(json.dumps(self.bundle))
        broken['categories'][0]['name'] = 'ROLLBACK CHECK'
        with self.assertRaisesRegex(ValueError, 'Protected unpublished/review'):
            SYNC.synchronize(self.db, broken, 1)
        self.db.rollback()
        self.assertEqual(before, SYNC.rows(self.db, 'SELECT name FROM sr_tutorial_category ORDER BY id'))

    def test_existing_history_learning_and_other_modules_are_preserved(self):
        baseline = json.loads(pathlib.Path(os.environ['STAR_RAIN_CONTENT_BASELINE']).read_text(encoding='utf-8'))
        digest = lambda value: hashlib.sha256(json.dumps(value, sort_keys=True, default=str, ensure_ascii=False).encode()).hexdigest()
        for table, expected in baseline['protected'].items():
            with self.subTest(table=table):
                data = SYNC.rows(self.db, 'SELECT * FROM `' + table + '` ORDER BY 1')
                self.assertEqual(expected['rows'], len(data))
                self.assertEqual(expected['sha256'], digest(data))
        for revision in SYNC.rows(self.db, 'SELECT id,snapshot_json,summary_json,content_sha256 FROM sr_tutorial_revision ORDER BY id'):
            expected = baseline['existingRevisionHashes'].get(str(revision['id']))
            if expected:
                self.assertEqual(expected, digest(revision))
        for tutorial in baseline['unlistedTutorials']:
            row = SYNC.rows(self.db, 'SELECT * FROM sr_tutorial WHERE id=%s', (tutorial['id'],))[0]
            self.assertEqual(tutorial['sha256'], digest(row))


if __name__ == '__main__':
    unittest.main()
