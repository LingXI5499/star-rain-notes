"""Strict source cleanup integration checks; independent test database only."""
import hashlib
import importlib.util
import json
import os
import pathlib
import unittest

import pymysql

SPEC = importlib.util.spec_from_file_location('prune', pathlib.Path(__file__).with_name('prune-static-tutorials.py'))
PRUNE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PRUNE)
SYNC = PRUNE.SYNC


class StrictTutorialSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        _, options = SYNC.load_config(os.environ['STAR_RAIN_CONTENT_TEST_CONFIG'])
        if not options['database'].startswith('star_rain_content_sync_test_'):
            raise ValueError('An independent test database is required')
        cls.db = pymysql.connect(**options)
        cls.bundle = json.loads(SYNC.BUNDLE.read_text(encoding='utf-8'))
        cls.baseline = json.loads(pathlib.Path(os.environ['STAR_RAIN_PRUNE_BASELINE']).read_text(encoding='utf-8'))

    @classmethod
    def tearDownClass(cls):
        cls.db.close()

    def tearDown(self):
        self.db.rollback()

    def test_categories_courses_groups_and_chapters_exactly_match_source(self):
        categories = SYNC.rows(self.db, 'SELECT slug,name FROM sr_tutorial_category')
        self.assertEqual({(c['slug'], c['name']) for c in self.bundle['categories']}, {(c['slug'], c['name']) for c in categories})
        tutorials = SYNC.rows(self.db, 'SELECT id,slug FROM sr_tutorial')
        self.assertEqual(self.baseline['retainedTutorialIds'], {t['slug']: t['id'] for t in tutorials})
        for course in self.bundle['courses']:
            tid = next(t['id'] for t in tutorials if t['slug'] == course['slug'])
            chapters = SYNC.rows(self.db, 'SELECT slug FROM sr_tutorial_chapter WHERE tutorial_id=%s', (tid,))
            self.assertEqual({c['slug'] for g in course['groups'] for c in g['chapters']}, {c['slug'] for c in chapters})
            groups = SYNC.rows(self.db, 'SELECT title FROM sr_tutorial_group WHERE tutorial_id=%s', (tid,))
            self.assertCountEqual([g['title'] for g in course['groups']], [g['title'] for g in groups])

    def test_published_snapshots_have_no_extra_chapters(self):
        for course in self.bundle['courses']:
            row = SYNC.rows(self.db, 'SELECT r.snapshot_json FROM sr_tutorial t JOIN sr_tutorial_revision r ON r.id=t.published_revision_id WHERE t.slug=%s', (course['slug'],))[0]
            snapshot = json.loads(row['snapshot_json'])
            self.assertEqual({c['slug']: c['bodyMarkdown'] for g in course['groups'] for c in g['chapters']},
                             {c['slug']: c['bodyMarkdown'] for g in snapshot['groups'] for c in g['chapters']})

    def test_repeated_prune_removes_nothing(self):
        result = PRUNE.execute_prune(self.db, PRUNE.plan_prune(self.db, self.bundle))
        self.assertTrue(all(value == 0 for value in result.values()), result)

    def test_review_conflict_does_not_delete_content(self):
        slug = self.bundle['courses'][0]['slug']
        SYNC.write(self.db, "UPDATE sr_tutorial SET editing_status='IN_REVIEW' WHERE slug=%s", (slug,))
        with self.assertRaisesRegex(ValueError, 'in review'):
            PRUNE.plan_prune(self.db, self.bundle)
        self.db.rollback()
        self.assertEqual(31, SYNC.rows(self.db, 'SELECT COUNT(*) n FROM sr_tutorial')[0]['n'])

    def test_unrelated_data_and_remaining_history_are_preserved(self):
        digest = lambda v: hashlib.sha256(json.dumps(v, sort_keys=True, default=str, ensure_ascii=False).encode()).hexdigest()
        for table, expected in self.baseline['protected'].items():
            self.assertEqual(expected, digest(SYNC.rows(self.db, 'SELECT * FROM `' + table + '` ORDER BY 1')), table)
        revisions = {str(r['id']): r for r in SYNC.rows(self.db, 'SELECT id,tutorial_id,snapshot_json,summary_json,content_sha256 FROM sr_tutorial_revision')}
        for revision_id, expected in self.baseline['keptRevisions'].items():
            self.assertEqual(expected, digest(revisions[revision_id]))
        for table, before in self.baseline['learningPlanRows'].items():
            after = json.loads(json.dumps(SYNC.rows(self.db, 'SELECT * FROM `' + table + '` ORDER BY 1'), default=str))
            self.assertEqual([r['id'] for r in before], [r['id'] for r in after])
            for left, right in zip(before, after):
                self.assertEqual({k:v for k,v in left.items() if k not in ('status','updated_at')}, {k:v for k,v in right.items() if k not in ('status','updated_at')})
                if left['status'] != right['status']:
                    self.assertIn(right['status'], ('CANCELLED','SKIPPED'))


if __name__ == '__main__':
    unittest.main()
