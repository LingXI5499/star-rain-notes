"""Independent, read-only integrity checks for the 2026-10-09 refresh."""
import argparse, hashlib, json
from pathlib import Path
from refresh import query, SOURCE, PROJECT, media_catalog, canonical_expr

def main():
    p=argparse.ArgumentParser();p.add_argument('--database',required=True);p.add_argument('--backup-report',required=True);p.add_argument('--output',required=True);a=p.parse_args()
    backup=json.loads(Path(a.backup_report).read_text(encoding='utf-8'));checks={}
    catalog=media_catalog()
    def count(sql):return int(query(sql,a.database)[0]['n'])
    def zero(name,sql):
        n=count(sql);checks[name]=n;assert n==0,(name,n)
    # Compare exact content hashes, not collation-dependent text equality.
    for target,source,key,extra in [('sr_blog_post','blog_post','slug',''),('sr_tutorial_chapter','tutorial_node','id',"s.node_type='CHAPTER' AND "),('sr_english_grammar_lesson','english_grammar_lesson','id',''),('sr_portfolio_work','portfolio_project','slug','')]:
        zero(target+'_source_text_mismatch','SELECT COUNT(*) n FROM '+SOURCE+'.'+source+' s LEFT JOIN '+target+' d ON d.'+key+'=s.'+key+' WHERE '+extra+'(d.id IS NULL OR NOT(SHA2(d.body_markdown,256)<=>SHA2('+canonical_expr('s.body_markdown',catalog)+',256)) OR NOT(SHA2(d.title,256)<=>SHA2(s.title,256)))')
    zero('vocabulary_source_mismatch','SELECT COUNT(*) n FROM '+SOURCE+'.vocabulary_word s LEFT JOIN sr_english_vocabulary_word d ON d.id=s.id WHERE d.id IS NULL OR NOT(SHA2(d.word,256)<=>SHA2(s.word,256)) OR NOT(SHA2(d.translation,256)<=>SHA2(s.translation,256)) OR d.theme_id<>s.theme_id')
    zero('blog_tag_source_mismatch','SELECT COUNT(*) n FROM '+SOURCE+'.blog_tag s LEFT JOIN sr_blog_tag d ON d.slug=s.slug WHERE d.id IS NULL OR NOT(SHA2(d.name,256)<=>SHA2(s.name,256))')
    zero('blog_tag_relations_missing','SELECT COUNT(*) n FROM '+SOURCE+'.blog_post_tag s JOIN '+SOURCE+'.blog_post b ON b.id=s.blog_post_id JOIN '+SOURCE+'.blog_tag t ON t.id=s.blog_tag_id JOIN sr_blog_post d ON d.slug=b.slug JOIN sr_blog_tag g ON g.slug=t.slug LEFT JOIN sr_blog_post_tag r ON r.post_id=d.id AND r.tag_id=g.id WHERE r.id IS NULL')
    relations=[('sr_blog_post_tag','post_id','sr_blog_post'),('sr_blog_post_tag','tag_id','sr_blog_tag'),('sr_blog_topic_post','post_id','sr_blog_post'),('sr_tutorial_group','tutorial_id','sr_tutorial'),('sr_tutorial_chapter','tutorial_id','sr_tutorial'),('sr_tutorial_chapter','group_id','sr_tutorial_group'),('sr_tutorial_knowledge_card','chapter_id','sr_tutorial_chapter'),('sr_tutorial_question_card','question_id','sr_tutorial_question'),('sr_tutorial_question_card','knowledge_card_id','sr_tutorial_knowledge_card'),('sr_portfolio_section','work_id','sr_portfolio_work'),('sr_portfolio_media','work_id','sr_portfolio_work'),('sr_portfolio_work_detail','work_id','sr_portfolio_work'),('sr_portfolio_link','work_id','sr_portfolio_work'),('sr_study_plan','tutorial_id','sr_tutorial'),('sr_study_task','plan_id','sr_study_plan'),('sr_study_task_chapter','study_task_id','sr_study_task'),('sr_learning_progress','chapter_id','sr_tutorial_chapter'),('sr_account_credential','account_id','sr_account'),('sr_account_role','account_id','sr_account'),('sr_english_reading_alignment','article_id','sr_english_reading_article'),('sr_english_reading_tag','article_id','sr_english_reading_article'),('sr_english_writing_article_tag','article_id','sr_english_writing_article'),('sr_english_writing_revision','article_id','sr_english_writing_article')]
    for child,col,parent in relations:zero(child+'_'+col+'_orphan','SELECT COUNT(*) n FROM '+child+' c LEFT JOIN '+parent+' p ON p.id=c.'+col+' WHERE c.'+col+' IS NOT NULL AND p.id IS NULL')
    zero('published_revision_missing',"SELECT COUNT(*) n FROM sr_tutorial t LEFT JOIN sr_tutorial_revision r ON r.id=t.published_revision_id WHERE t.publication_status='PUBLISHED' AND r.id IS NULL")
    zero('published_chapter_source_mismatch',"SELECT COUNT(*) n FROM sr_tutorial t JOIN sr_tutorial_revision r ON r.id=t.published_revision_id JOIN JSON_TABLE(r.snapshot_json,'$.groups[*].chapters[*]' COLUMNS(chapter_id BIGINT PATH '$.id',body LONGTEXT PATH '$.bodyMarkdown')) j JOIN "+SOURCE+".tutorial_node s ON s.id=j.chapter_id WHERE NOT(SHA2(j.body,256)<=>SHA2("+canonical_expr('s.body_markdown',catalog)+",256))")
    zero('fixture_cards_remaining',"SELECT COUNT(*) n FROM sr_tutorial_knowledge_card WHERE front_text='测试' AND back_markdown='测试'")
    zero('fixture_articles_remaining',"SELECT COUNT(*) n FROM sr_english_writing_article WHERE title LIKE '【本地验收%' OR title='Untitled Article'")
    zero('fixture_accounts_remaining',"SELECT COUNT(*) n FROM sr_account WHERE username REGEXP '^(qa_|mailqa_|ui_)'")
    zero('wrong_featured_work',"SELECT COUNT(*) n FROM sr_portfolio_work WHERE featured=1 AND title<>'星雨笔录'")
    expected={'sr_blog_post':13,'sr_blog_tag':57,'sr_blog_post_tag':68,'sr_tutorial':39,'sr_tutorial_group':324,'sr_tutorial_chapter':2181,'sr_tutorial_knowledge_card':2,'sr_portfolio_work':2,'sr_portfolio_media':5,'sr_english_vocabulary_theme':60,'sr_english_vocabulary_word':7053,'sr_english_grammar_section':10,'sr_english_grammar_lesson':42,'sr_english_reading_article':144,'sr_english_reading_alignment':288,'sr_english_writing_article':144,'sr_message':0}
    actual={t:count('SELECT COUNT(*) n FROM '+t) for t in expected};assert actual==expected,(actual,expected)
    for t in ['sr_english_reading_article','sr_english_writing_article']:
        zero(t+'_topic_coverage',"SELECT COUNT(*) n FROM sr_english_taxonomy_term t WHERE t.dimension='TOPIC' AND t.enabled=1 AND t.parent_id IS NOT NULL AND (SELECT COUNT(*) FROM "+t+" a WHERE a.primary_topic_id=t.id)<>1")
    protected=['sr_english_vocabulary_memory','sr_english_vocabulary_mode_memory','sr_english_vocabulary_review_log','sr_english_vocabulary_plan','sr_english_vocabulary_plan_item','sr_english_vocabulary_card_preference','flyway_schema_history']
    checksums=query('CHECKSUM TABLE '+','.join(protected)+' EXTENDED',a.database)
    for row in checksums:
        table=row['Table'].split('.')[-1];assert row['Checksum']==backup['before'][table]['checksum'],('protected checksum changed',table)
    alignments=query("SELECT language,expected_text,paragraph_hash,source_markdown_hash,range_end FROM sr_english_reading_alignment",a.database)
    # Single paragraph examples have the same source and visible text; verify persisted anchor hashes.
    for item in alignments:
        digest=hashlib.sha256(item['expected_text'].encode('utf-8')).hexdigest();assert digest==item['paragraph_hash']==item['source_markdown_hash'];assert len(item['expected_text'].encode('utf-16-le'))//2==int(item['range_end'])
    manifest=json.loads((PROJECT/'.local/v1-content-refresh-20261009/media-manifest.json').read_text(encoding='utf-8'))
    for item in manifest:
        asset=query("SELECT id,storage_key,sha256,size_bytes FROM sr_media_asset WHERE access_level='PUBLIC' AND status='ACTIVE' AND sha256='"+item['sha256']+"' ORDER BY id LIMIT 1",a.database)
        assert len(asset)==1;image=(PROJECT/'media'/asset[0]['storage_key']).resolve();assert image.is_relative_to((PROJECT/'media').resolve());assert hashlib.sha256(image.read_bytes()).hexdigest()==item['sha256'];assert image.stat().st_size==item['sizeBytes']
    out=Path(a.output).resolve();assert out.is_relative_to((PROJECT/'.local').resolve());out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps(dict(database=a.database,counts=actual,zeroChecks=checks,protectedChecksumsUnchanged=protected,validAnchors=len(alignments)),ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(dict(database=a.database,counts=actual,checksPassed=len(checks),protectedTablesUnchanged=len(protected),validAnchors=len(alignments)),ensure_ascii=False))

if __name__=='__main__':main()
