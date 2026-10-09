"""Local, reviewed V1 content refresh. Plan by default; --apply requires a verified backup.

Operational sessions, V1 Flyway history, and retired English tables are never imported.
Private plans may contain account hashes: write them only below the ignored .local folder.
"""
from pathlib import Path
import argparse, datetime, hashlib, json, os, re, subprocess

PROJECT = Path(__file__).resolve().parents[2]
MYSQL = Path('D:/DevelopTool/Language/MySQL/mysql-8.0.34-winx64/bin/mysql.exe')
SOURCE = 'sr_v1_authoritative_20261009105000'
PREVIEW = 'sr_english_retirement_restore_20261009090312'
SECRET = dict(line.split(':', 1) for line in (PROJECT/'backend/application-local-secret.yml').read_text(encoding='utf-8-sig').splitlines() if line.startswith('STAR_RAIN_DB_'))
SECRET = {k:v.strip().strip('\"').strip("'") for k,v in SECRET.items()}
assert re.match(r'^jdbc:mysql://(?:127\.0\.0\.1|localhost):3306/star_rain_v2(?:\?|$)', SECRET['STAR_RAIN_DB_URL'])
ENV = dict(os.environ, MYSQL_PWD=SECRET['STAR_RAIN_DB_PASSWORD'])
ARGS = [str(MYSQL), '-h', '127.0.0.1', '-P', '3306', '-u', SECRET['STAR_RAIN_DB_USERNAME'], '--default-character-set=utf8mb4', '--batch', '--raw']

def run(sql, db):
    assert re.fullmatch(r'[a-zA-Z0-9_]+', db)
    r = subprocess.run(ARGS+[db], input=sql, encoding='utf-8', env=ENV, capture_output=True)
    if r.returncode: raise RuntimeError(r.stderr)
    return r.stdout

def query(sql, db):
    lines = run(sql, db).strip().splitlines()
    if not lines: return []
    return [dict(zip(lines[0].split('\t'), line.split('\t'))) for line in lines[1:]]

def schema(db):
    return {list(x.values())[0]: [c['Field'] for c in query('SHOW COLUMNS FROM `'+list(x.values())[0]+'`',db)] for x in query('SHOW TABLES',db)}

def rows(table, db, cols, where='1=1'):
    assert re.fullmatch(r'[a-zA-Z0-9_]+',table)
    expr=','.join("'"+c+"',`"+c+'`' for c in cols)
    return [json.loads(r['j']) for r in query('SELECT JSON_OBJECT('+expr+') j FROM `'+table+'` WHERE '+where,db)]

def val(v):
    if v is None:return 'NULL'
    if isinstance(v,(dict,list)):v=json.dumps(v,ensure_ascii=False,separators=(',',':'))
    if isinstance(v,(int,float)):return str(v)
    # Hex literals prevent quoting, escaping, SQL-mode, and shell interpolation hazards.
    return "CONVERT(X'"+str(v).encode('utf-8').hex()+"' USING utf8mb4)"

def ids(items):return ','.join(str(int(i)) for i in items) or '-1'

def media_catalog():
    manifest=json.loads((PROJECT/'.local/v1-content-refresh-20261009/media-manifest.json').read_text(encoding='utf-8'))
    assets=rows('media_asset',SOURCE,['id','public_url','asset_type'],"asset_type='IMAGE'")
    hashes={x['sourceId']:x['sha256'] for x in manifest}
    assert set(hashes)=={x['id'] for x in assets}
    return [(x['public_url'],hashes[x['id']]) for x in assets]

def canonical_expr(expression,catalog):
    for legacy,digest in catalog:
        replacement="CONCAT('/api/media/assets/',(SELECT id FROM sr_media_asset WHERE sha256="+val(digest)+" ORDER BY id LIMIT 1),'/content')"
        for address in ['https://yulanlin.cn'+legacy,legacy]:expression='REPLACE('+expression+','+val(address)+','+replacement+')'
    return expression

def main():
    p=argparse.ArgumentParser();p.add_argument('--target',required=True);p.add_argument('--output',required=True);p.add_argument('--apply',action='store_true');p.add_argument('--backup-report');a=p.parse_args()
    assert a.target in ['star_rain_v2',PREVIEW] or re.fullmatch(r'sr_content_cleanup_probe_[0-9]{14}_[ab]',a.target)
    out=Path(a.output).resolve();assert out.is_relative_to((PROJECT/'.local').resolve());out.mkdir(parents=True,exist_ok=True)
    dstschema=schema(a.target);srcschema=schema(SOURCE)
    src={t:rows(t,SOURCE,cs) for t,cs in srcschema.items() if t in ['blog_post','blog_tag','blog_post_tag','tutorial','tutorial_category','tutorial_node','portfolio_project','portfolio_project_media','profile','profile_selected_content','site_setting','user_account','account_profile','media_asset','vocabulary_theme','vocabulary_word','english_grammar_course','english_grammar_section','english_grammar_lesson']}
    dst={t:rows(t,a.target,cs) for t,cs in dstschema.items() if t!='flyway_schema_history' and t not in ['sr_analytics_event','sr_seo_page_snapshot','sr_search_document','sr_english_vocabulary_word']}
    # Fixture identities are proven by names/slugs and dependency closure, never arbitrary ID ranges.
    bad={}
    bad['tutorial_id']=[x['id'] for x in dst['sr_tutorial'] if x['slug'] in ['learning-v2-20261007','learning-plan-sql-test-20261007']]
    bad['chapter_id']=[x['id'] for x in dst['sr_tutorial_chapter'] if x['tutorial_id'] in bad['tutorial_id']]
    bad['group_id']=[x['id'] for x in dst['sr_tutorial_group'] if x['tutorial_id'] in bad['tutorial_id']]
    bad['knowledge_card_id']=[x['id'] for x in dst['sr_tutorial_knowledge_card'] if x['chapter_id'] in bad['chapter_id'] or (x['front_text']=='测试' and x['back_markdown']=='测试')]
    bad['question_id']=[x['id'] for x in dst['sr_tutorial_question'] if x['chapter_id'] in bad['chapter_id'] or (x['question_text']=='测试' and x['reference_answer']=='测试')]
    bad['account_id']=[x['id'] for x in dst['sr_account'] if re.fullmatch(r'(qa_|mailqa_|ui_).+',x['username'])]
    bad['work_id']=[x['id'] for x in dst['sr_portfolio_work'] if x['slug'] in ['works-v11-acceptance','works-v11-template-draft']]
    bad['section_id']=[x['id'] for x in dst['sr_portfolio_section'] if x['work_id'] in bad['work_id']]
    source_slugs={x['slug'] for x in src['blog_post']}
    placeholder_slugs={'post-ec371ed6b059','post-663d51332d94','post-048ea4b15543','post-08da1b49a3b9','post-7f0c3900f2b6','post-6611d939f7b4','post-aa4b63f60ef0'}
    bad['post_id']=[x['id'] for x in dst['sr_blog_post'] if x['slug'] not in source_slugs and x['slug'] in placeholder_slugs]
    bad['review_request_id']=[x['id'] for x in dst['sr_review_request'] if x['target_module']=='DEMO']
    for table,key,conditions in [
        ('sr_study_plan','plan_id',[('tutorial_id','tutorial_id')]),
        ('sr_study_task','study_task_id',[('plan_id','plan_id'),('tutorial_id','tutorial_id')]),
        ('sr_review_schedule','schedule_id',[('knowledge_card_id','knowledge_card_id')]),
        ('sr_review_task','review_task_id',[('schedule_id','schedule_id'),('knowledge_card_id','knowledge_card_id')]),
        ('sr_learning_session','session_id',[('tutorial_id','tutorial_id'),('study_plan_id','plan_id')]),
        ('sr_user_question_answer','answer_id',[('question_id','question_id')])]:
        bad[key]=[x['id'] for x in dst.get(table,[]) if any(x.get(c) in bad[k] for c,k in conditions) or x.get('account_id') in bad['account_id']]
    # Standalone review sessions containing fixture cards also belong to the test closure.
    fixture_sessions={x['session_id'] for x in dst.get('sr_learning_session_item',[]) if x.get('knowledge_card_id') in bad['knowledge_card_id']}
    bad['session_id']=sorted(set(bad['session_id'])|fixture_sessions)
    completed=[x for x in dst['sr_learning_session'] if x['id'] in bad['session_id'] and x['completed_at']]
    bad['history_id']=[h['id'] for h in dst['sr_learning_history'] if h['event_type']=='REVIEW_SESSION_COMPLETED' and any(h['account_id']==s['account_id'] and 0<=(datetime.datetime.fromisoformat(h['occurred_at'])-datetime.datetime.fromisoformat(s['completed_at'])).total_seconds()<=0.005 for s in completed)]
    badreading=[x['id'] for x in dst['sr_english_reading_article'] if '【隔离预览】' in x['title'] or '【本地验收' in x['title']]
    badwriting=[x['id'] for x in dst['sr_english_writing_article'] if x['title'].startswith('【本地验收') or (x['title']=='Untitled Article' and not x['body_markdown'].strip())]
    sql=['START TRANSACTION;','CREATE TEMPORARY TABLE refresh_guard (ok INT CHECK (ok=1));']
    def emit(s):sql.append(s+';')
    def insert(table,data,update=True):
        cs=list(data);tail=' ON DUPLICATE KEY UPDATE '+','.join('`'+c+'`=VALUES(`'+c+'`)' for c in cs if c not in ['id','created_at']) if update else ''
        emit('INSERT INTO `'+table+'` ('+','.join('`'+c+'`' for c in cs)+') VALUES ('+','.join(val(data[c]) for c in cs)+')'+tail)
    aliases={'study_plan_id':'plan_id','actor_account_id':'account_id','target_account_id':'account_id','created_by_account_id':'account_id','updated_by_account_id':'account_id'}
    own={'sr_tutorial':'tutorial_id','sr_tutorial_group':'group_id','sr_tutorial_chapter':'chapter_id','sr_tutorial_knowledge_card':'knowledge_card_id','sr_tutorial_question':'question_id','sr_study_plan':'plan_id','sr_study_task':'study_task_id','sr_review_schedule':'schedule_id','sr_review_task':'review_task_id','sr_learning_session':'session_id','sr_learning_history':'history_id','sr_user_question_answer':'answer_id','sr_account':'account_id','sr_review_request':'review_request_id','sr_portfolio_work':'work_id','sr_portfolio_section':'section_id','sr_blog_post':'post_id'}
    deleted={}
    for table,cs in dstschema.items():
        if table=='flyway_schema_history':continue
        clauses=[]
        for c in cs:
            k=own.get(table) if c=='id' else aliases.get(c,c)
            if k=='section_id' and not table.startswith('sr_portfolio_'):continue
            if k in bad and bad[k]:clauses.append('`'+c+'` IN ('+ids(bad[k])+')')
        if table.startswith('sr_english_reading_') and 'article_id' in cs:clauses.append('article_id IN ('+ids(badreading)+')')
        if table.startswith('sr_english_writing_') and 'article_id' in cs:clauses.append('article_id IN ('+ids(badwriting)+')')
        if table=='sr_english_reading_article':clauses.append('id IN ('+ids(badreading)+')')
        if table=='sr_english_writing_article':clauses.append('id IN ('+ids(badwriting)+')')
        if table in ['sr_message','sr_message_action']:clauses=['1=1']
        if clauses:
            condition=' OR '.join(clauses);deleted[table]=int(query('SELECT COUNT(*) n FROM `'+table+'` WHERE '+condition,a.target)[0]['n']);emit('DELETE FROM `'+table+'` WHERE '+condition)
    emit("DELETE pt FROM sr_blog_post_tag pt JOIN sr_blog_tag t ON t.id=pt.tag_id WHERE t.slug='test'")
    emit("DELETE FROM sr_blog_tag WHERE slug='test'")
    emit('DELETE r FROM sr_tutorial_question_card r LEFT JOIN sr_tutorial_question q ON q.id=r.question_id LEFT JOIN sr_tutorial_knowledge_card c ON c.id=r.knowledge_card_id WHERE q.id IS NULL OR c.id IS NULL')
    emit('DELETE r FROM sr_blog_post_tag r LEFT JOIN sr_blog_post b ON b.id=r.post_id LEFT JOIN sr_blog_tag t ON t.id=r.tag_id WHERE b.id IS NULL OR t.id IS NULL')
    # Derived documents/counters and content references cannot point at deleted fixtures.
    content={'BLOG':bad['post_id'],'TUTORIAL':bad['tutorial_id'],'CHAPTER':bad['chapter_id'],'PORTFOLIO':bad['work_id']}
    for table in ['sr_profile_featured_content','sr_search_document','sr_seo_page_snapshot','sr_analytics_event','sr_analytics_daily_content']:
        for typ,values in content.items():emit('DELETE FROM '+table+' WHERE content_type='+val(typ)+' AND content_id IN ('+ids(values)+')')
    emit('DELETE FROM sr_seo_page_snapshot');emit('DELETE FROM sr_search_document')
    bad_work_media=[x['id'] for x in dst['sr_portfolio_media'] if x['work_id'] in bad['work_id']]
    bad_section_media=[x['id'] for x in dst['sr_portfolio_section_media'] if x['section_id'] in bad['section_id']]
    emit("DELETE FROM sr_media_reference WHERE (source_type='WORK_MEDIA' AND source_id IN ("+ids(bad_work_media)+")) OR (source_type='WORK_SECTION_MEDIA' AND source_id IN ("+ids(bad_section_media)+")) OR (source_type='WORK_PROTOTYPE' AND source_id IN ("+ids(bad['work_id'])+"))")
    qaemails=[x['email'] for x in dst['sr_account'] if x['id'] in bad['account_id']]
    if qaemails:emit('DELETE FROM sr_email_verification WHERE email IN ('+','.join(val(x) for x in qaemails)+')')
    # Remove only known sample asset records after checking every direct reference column.
    badmedia=[x['id'] for x in dst['sr_media_asset'] if x['original_name'].startswith('sample.') or x['original_name']=='works-v11-prototype.zip']
    for mid in badmedia:
        checks=[]
        for table,cs in dstschema.items():
            for c in cs:
                if c.endswith('media_asset_id') or c=='prototype_asset_id':checks.append('(SELECT COUNT(*) FROM `'+table+'` WHERE `'+c+'`='+str(mid)+')')
        emit('INSERT INTO refresh_guard VALUES (IF('+('+'.join(checks) or '0')+'=0,1,0))')
        emit('DELETE FROM sr_media_asset WHERE id='+str(mid))
    # Identity matching uses email. Existing V2 credentials and roles remain intact.
    nextaccount=max(x['id'] for x in dst['sr_account'])+1
    amap={};display={x['account_id']:x['display_name'] for x in src['account_profile']}
    for u in src['user_account']:
        matches=[x for x in dst['sr_account'] if x['email']==u['email'] and x['id'] not in bad['account_id']]
        if matches:amap[u['id']]=matches[0]['id'];continue
        aid=nextaccount;nextaccount+=1;amap[u['id']]=aid
        insert('sr_account',dict(id=aid,username='v1_user_'+str(u['id']),email=u['email'],display_name=display.get(u['id'],'V1 用户 '+str(u['id'])),status='ACTIVE' if u['account_status']=='ACTIVE' else 'DISABLED',email_verified_at=u['email_verified_at'],created_at=u['created_at'],updated_at=u['updated_at']),False)
        insert('sr_account_credential',dict(account_id=aid,password_hash=u['password_hash'],password_changed_at=u['password_changed_at'] or u['created_at']),False)
        roles={x['code']:x['id'] for x in dst['sr_role']};insert('sr_account_role',dict(account_id=aid,role_id=roles[u['role']],granted_by=1),False)
    owner=next(x['id'] for x in dst['sr_account'] if x['username']=='Lingxi')
    manifest=json.loads((PROJECT/'.local/v1-content-refresh-20261009/media-manifest.json').read_text(encoding='utf-8'));by_source={r['sourceId']:r for r in manifest};catalog=media_catalog()
    for r in src['media_asset']:
        if r['asset_type']!='IMAGE':continue
        recovered=by_source[r['id']];image=(PROJECT/'media'/recovered['storageKey']).resolve();assert image.is_relative_to((PROJECT/'media').resolve());assert hashlib.sha256(image.read_bytes()).hexdigest()==recovered['sha256']
        data=dict(original_name=r['original_name'],media_type='IMAGE',mime_type='image/png',file_extension='png',size_bytes=recovered['sizeBytes'],sha256=recovered['sha256'],storage_provider='LOCAL',storage_key=recovered['storageKey'],width=recovered['width'],height=recovered['height'],access_level='PUBLIC',status='ACTIVE',uploaded_by_account_id=owner,created_at=r['created_at'])
        emit('INSERT INTO sr_media_asset ('+','.join(data)+') SELECT '+','.join(val(x) for x in data.values())+' WHERE NOT EXISTS(SELECT 1 FROM sr_media_asset WHERE sha256='+val(recovered['sha256'])+')')
        emit('SET @media_'+str(r['id'])+'=(SELECT id FROM sr_media_asset WHERE sha256='+val(recovered['sha256'])+' ORDER BY id LIMIT 1)')
        emit("UPDATE sr_media_asset SET access_level='PUBLIC',status='ACTIVE' WHERE id=@media_"+str(r['id']))
    for r in src['blog_tag']:insert('sr_blog_tag',dict(slug=r['slug'],name=r['name'],status='ENABLED',created_at=r['created_at'],updated_at=r['updated_at']))
    for r in src['blog_post']:insert('sr_blog_post',dict(slug=r['slug'],title=r['title'],summary=r['summary'],body_markdown=r['body_markdown'],status=r['publish_status'],published_at=r['published_at'],created_by_account_id=owner,updated_by_account_id=owner,created_at=r['created_at'],updated_at=r['updated_at']))
    bslug={r['id']:r['slug'] for r in src['blog_post']};tslug={r['id']:r['slug'] for r in src['blog_tag']}
    emit('DELETE p FROM sr_blog_post_tag p JOIN sr_blog_post b ON b.id=p.post_id WHERE b.slug IN ('+','.join(val(s) for s in bslug.values())+')')
    for r in src['blog_post_tag']:emit('INSERT INTO sr_blog_post_tag (post_id,tag_id) SELECT b.id,t.id FROM sr_blog_post b JOIN sr_blog_tag t ON t.slug='+val(tslug[r['blog_tag_id']])+' WHERE b.slug='+val(bslug[r['blog_post_id']]))
    # Existing source IDs were verified before migration, preserving study links.
    for table,source,fields in [('sr_tutorial_category','tutorial_category',['id','name','slug','sort_order','created_at','updated_at']),('sr_english_vocabulary_theme','vocabulary_theme',dstschema['sr_english_vocabulary_theme']),('sr_english_grammar_course','english_grammar_course',dstschema['sr_english_grammar_course']),('sr_english_grammar_section','english_grammar_section',dstschema['sr_english_grammar_section']),('sr_english_grammar_lesson','english_grammar_lesson',dstschema['sr_english_grammar_lesson'])]:
        for r in src[source]:insert(table,{c:r[c] for c in fields if c in r})
    for r in src['vocabulary_word']:
        data={c:r[c] for c in dstschema['sr_english_vocabulary_word'] if c in r and c not in ['memory_count','last_memory_at']};insert('sr_english_vocabulary_word',data)
    emit('UPDATE sr_english_vocabulary_word d JOIN '+SOURCE+'.vocabulary_word s ON s.id=d.id SET d.memory_count=GREATEST(d.memory_count,s.memory_count),d.last_memory_at=GREATEST(COALESCE(d.last_memory_at,s.last_memory_at),COALESCE(s.last_memory_at,d.last_memory_at))')
    for r in src['tutorial']:insert('sr_tutorial',dict(id=r['id'],category_id=r['category_id'],slug=r['slug'],title=r['title'],summary=r['summary'],sort_order=r['sort_order'],publication_status=r['publish_status'],editing_status='DRAFT',published_at=r['published_at'],created_by_account_id=owner,updated_by_account_id=owner,created_at=r['created_at'],updated_at=r['updated_at']))
    for r in src['tutorial_node']:
        if r['node_type']=='GROUP':insert('sr_tutorial_group',dict(id=r['id'],tutorial_id=r['tutorial_id'],title=r['title'],description=r['summary'],sort_order=r['sort_order'],status='ACTIVE',created_at=r['created_at'],updated_at=r['updated_at']))
        else:insert('sr_tutorial_chapter',dict(id=r['id'],tutorial_id=r['tutorial_id'],group_id=r['parent_id'],slug=r['slug'],title=r['title'],summary=r['summary'],body_markdown=r['body_markdown'],sort_order=r['sort_order'],status=r['publish_status'],created_at=r['created_at'],updated_at=r['updated_at']))
    # Rebase retained published snapshots onto authoritative source text; prune fixture cards.
    nodes={str(r['id']):r for r in src['tutorial_node']};tutorials={r['id']:r for r in src['tutorial']};cats={r['id']:r for r in src['tutorial_category']}
    for rev in dst['sr_tutorial_revision']:
        if rev['tutorial_id'] not in tutorials:continue
        snap=rev['snapshot_json'];snap=json.loads(snap) if isinstance(snap,str) else snap;tr=tutorials[rev['tutorial_id']];cat=cats[tr['category_id']]
        snap.update(title=tr['title'],summary=tr['summary'],categoryId=str(cat['id']),categoryName=cat['name'],categorySlug=cat['slug'])
        for g in snap.get('groups',[]):
            if g['id'] in nodes:g['title']=nodes[g['id']]['title']
            for ch in g.get('chapters',[]):
                if ch['id'] in nodes:
                    nr=nodes[ch['id']];ch.update(title=nr['title'],summary=nr['summary'],bodyMarkdown=nr['body_markdown'])
                ch['cards']=[c for c in ch.get('cards',[]) if int(c['id']) not in bad['knowledge_card_id']];ch['questions']=[q for q in ch.get('questions',[]) if int(q['id']) not in bad['question_id']]
                ch['cardCount']=len(ch['cards']);ch['questionCount']=len(ch['questions'])
        j=json.dumps(snap,ensure_ascii=False,separators=(',',':'));emit('UPDATE sr_tutorial_revision SET snapshot_json='+val(j)+',content_sha256='+val(hashlib.sha256(j.encode()).hexdigest())+',revision_reason='+val('V1 2026-10-09 数据校准；清除验收卡片')+' WHERE id='+str(rev['id']))
    for r in src['portfolio_project']:
        data=dict(slug=r['slug'],work_type='SOFTWARE',title=r['title'],summary=r['summary'],body_markdown=r['body_markdown'],status=r['publish_status'],role=r['role'],tech_stack=r['tech_stack'],project_status=r['project_status'],started_on=r['started_at'],ended_on=r['completed_at'],seo_title=r['seo_title'],seo_description=r['seo_description'],featured=int(r['title']=='星雨笔录'),sort_order=r['sort_order'],published_at=r['published_at'],created_by_account_id=owner,updated_by_account_id=owner,created_at=r['created_at'],updated_at=r['updated_at']);insert('sr_portfolio_work',data)
        emit('UPDATE sr_portfolio_work_detail d JOIN sr_portfolio_work w ON w.id=d.work_id SET d.detail_json='+val(dict(techStack=r['tech_stack'],role=r['role'],projectStage=r['project_status']))+' WHERE w.slug='+val(r['slug']))
        for kind,url in [('GITHUB',r['repository_url']),('DEMO',r['demo_url'])]:
            if url:
                emit('UPDATE sr_portfolio_link l JOIN sr_portfolio_work w ON w.id=l.work_id SET l.url='+val(url)+' WHERE w.slug='+val(r['slug'])+' AND l.link_type='+val(kind))
                emit('INSERT INTO sr_portfolio_link(work_id,link_type,label,url,sort_order,status) SELECT w.id,'+','.join(val(x) for x in [kind,'查看代码' if kind=='GITHUB' else '访问作品',url,0,'ENABLED'])+' FROM sr_portfolio_work w WHERE w.slug='+val(r['slug'])+' AND NOT EXISTS(SELECT 1 FROM sr_portfolio_link l WHERE l.work_id=w.id AND l.link_type='+val(kind)+')')
        if r['cover_media_id'] is not None:
            emit('SET @work=(SELECT id FROM sr_portfolio_work WHERE slug='+val(r['slug'])+')')
            emit('UPDATE sr_portfolio_media SET media_asset_id=@media_'+str(r['cover_media_id'])+',caption='+val(r['title'])+" WHERE work_id=@work AND usage_type='COVER'")
            emit('INSERT INTO sr_portfolio_media(work_id,media_asset_id,usage_type,caption,sort_order) SELECT @work,@media_'+str(r['cover_media_id'])+",'COVER',"+val(r['title'])+",0 WHERE NOT EXISTS(SELECT 1 FROM sr_portfolio_media WHERE work_id=@work AND usage_type='COVER')")
    project_slugs={r['id']:r['slug'] for r in src['portfolio_project']}
    for r in src['portfolio_project_media']:
        emit('SET @work=(SELECT id FROM sr_portfolio_work WHERE slug='+val(project_slugs[r['project_id']])+')')
        caption=' · '.join(str(x) for x in [r['title'],r['description'] or r['alt_text']] if x)
        emit('INSERT INTO sr_portfolio_media(work_id,media_asset_id,usage_type,caption,sort_order) SELECT @work,@media_'+str(r['media_asset_id'])+",'SCREENSHOT',"+val(caption)+','+str(r['sort_order'])+' WHERE NOT EXISTS(SELECT 1 FROM sr_portfolio_media WHERE work_id=@work AND media_asset_id=@media_'+str(r['media_asset_id'])+')')
    emit("DELETE r FROM sr_media_reference r JOIN sr_portfolio_media m ON m.id=r.source_id WHERE r.source_module='PORTFOLIO' AND r.source_type='WORK_MEDIA'")
    emit("INSERT IGNORE INTO sr_media_reference(media_asset_id,source_module,source_type,source_id,usage_code) SELECT media_asset_id,'PORTFOLIO','WORK_MEDIA',id,IF(usage_type='COVER','portfolio.cover','portfolio.screenshot') FROM sr_portfolio_media")
    # V1 author/site business text supersedes placeholder configuration, without importing server URLs.
    pr=src['profile'][0];bio='\n\n'.join(x for x in [pr['bio'],('## 技术方向\n\n'+pr['technical_direction_markdown']) if pr['technical_direction_markdown'] else '',('## 学习与实践\n\n'+pr['journey_markdown']) if pr['journey_markdown'] else ''] if x)
    emit('UPDATE sr_profile SET display_name='+val(pr['display_name'])+',headline='+val(pr['headline'])+',bio_markdown='+val(bio)+" WHERE profile_key='OWNER'")
    emit("DELETE FROM sr_profile_skill WHERE profile_id=1 AND category='DIRECTION'")
    focus=pr['current_focus'];focus=json.loads(focus) if isinstance(focus,str) else focus
    for n,name in enumerate(focus or []):insert('sr_profile_skill',dict(profile_id=1,category='DIRECTION',name=name,sort_order=n+1,status='ENABLED'))
    for platform,url in [('GITHUB',pr['github_url']),('EMAIL_PAGE','mailto:'+pr['public_email'] if pr['public_email'] else None)]:
        if url:emit('UPDATE sr_profile_social_link SET url='+val(url)+' WHERE profile_id=1 AND platform_code='+val(platform))
    site=src['site_setting'][0];emit('UPDATE sr_site_config SET site_name='+val(site['site_name'])+',site_title='+val(site['site_name'])+',tagline='+val(site['tagline'])+',site_description='+val(site['default_seo_description'])+',footer_text='+val(site['footer_text'])+" WHERE config_key='PRIMARY'")
    emit("DELETE f FROM sr_profile_featured_content f JOIN sr_portfolio_work w ON w.id=f.content_id WHERE f.content_type='PORTFOLIO' AND w.title<>'星雨笔录'")
    emit("UPDATE sr_portfolio_work SET featured=(title='星雨笔录')")
    # Only attachment URLs change: the imported text otherwise remains the authoritative V1 text.
    for table,columns in [('sr_blog_post',['body_markdown']),('sr_tutorial_chapter',['body_markdown']),('sr_portfolio_work',['body_markdown']),('sr_profile',['bio_markdown']),('sr_english_grammar_lesson',['body_markdown'])]:
        for c in columns:emit('UPDATE '+table+' SET '+c+'='+canonical_expr(c,catalog)+" WHERE "+c+" LIKE '%/uploads/%'")
    emit('UPDATE sr_tutorial_revision SET snapshot_json='+canonical_expr('CAST(snapshot_json AS CHAR CHARACTER SET utf8mb4)',catalog)+",content_sha256=SHA2(CAST(snapshot_json AS CHAR CHARACTER SET utf8mb4),256) WHERE CAST(snapshot_json AS CHAR CHARACTER SET utf8mb4) LIKE '%/uploads/%'")
    # Register imported body references using the modules' existing source/usage conventions.
    for table,source,id_key,module,source_type,usage in [('sr_blog_post','blog_post','slug','BLOG','POST','blog.content'),('sr_tutorial_chapter','tutorial_node','id','TUTORIAL','CHAPTER','tutorial.chapter-content')]:
        for original in src[source]:
            if source=='tutorial_node' and original['node_type']!='CHAPTER':continue
            for image in src['media_asset']:
                if image['asset_type']=='IMAGE' and image['public_url'] in (original['body_markdown'] or ''):
                    emit('INSERT IGNORE INTO sr_media_reference(media_asset_id,source_module,source_type,source_id,usage_code) SELECT @media_'+str(image['id'])+','+','.join(val(x) for x in [module,source_type])+',id,'+val(usage)+' FROM '+table+' WHERE '+id_key+'='+val(original[id_key]))
    emit("INSERT IGNORE INTO sr_media_reference(media_asset_id,source_module,source_type,source_id,usage_code) SELECT DISTINCT m.id,'TUTORIAL','REVISION',r.id,'tutorial.chapter-content' FROM sr_tutorial_revision r JOIN sr_media_asset m ON CAST(r.snapshot_json AS CHAR) LIKE CONCAT('%/api/media/assets/',m.id,'/content%')")
    # All concrete world topics receive one short, original, bilingual reading and writing sample.
    examples=json.loads((Path(__file__).parent/'examples.json').read_text(encoding='utf-8'))
    leaves={x['id'] for x in dst['sr_english_taxonomy_term'] if x['enabled'] and x['dimension']=='TOPIC' and x['parent_id'] is not None}
    assert {e['topicId'] for e in examples}==leaves and len(examples)==len(leaves)==144
    for e in examples:
        tid=e['topicId'];slug='topic-'+str(tid)+'-reading-example';wslug='topic-'+str(tid)+'-writing-example'
        insert('sr_english_reading_article',dict(title=e['title'],slug=slug,summary=e['readingZh'],body_markdown=e['reading'],translation_zh_markdown=e['readingZh'],primary_topic_id=tid,content_origin='ORIGINAL',source_name='星雨笔录 · 原创主题示例',publish_status='PUBLISHED',sort_order=tid,published_at=datetime.datetime.now().isoformat(sep=' ',timespec='seconds')))
        emit('SET @rid=(SELECT id FROM sr_english_reading_article WHERE slug='+val(slug)+')')
        emit("INSERT INTO sr_english_reading_rights (article_id,original_author,rights_status,rights_basis,license_notice) VALUES (@rid,'星雨笔录（AI 辅助编写）','CLEARED','本次迁移编写的原创教学示例','虚构场景，仅作英语阅读示例') ON DUPLICATE KEY UPDATE rights_status='CLEARED'")
        emit('INSERT IGNORE INTO sr_english_reading_tag VALUES (@rid,235),(@rid,255)')
        alignments=[]
        emit('DELETE FROM sr_english_reading_alignment WHERE article_id=@rid')
        for lang,text in [('EN',e['reading']),('ZH',e['readingZh'])]:
            h=hashlib.sha256(text.encode('utf-8')).hexdigest();end=len(text.encode('utf-16-le'))//2
            item=dict(groupKey='example-'+str(tid),language=lang,paragraphIndex=0,rangeStart=0,rangeEnd=end,expectedText=text,paragraphHash=h,sourceMarkdownHash=h,status='ACTIVE',sortOrder=0);alignments.append(item)
            emit('INSERT INTO sr_english_reading_alignment(article_id,group_key,language,paragraph_index,range_start,range_end,expected_text,paragraph_hash,source_markdown_hash,alignment_status,sort_order) VALUES (@rid,'+','.join(val(x) for x in [item['groupKey'],lang,0,0,end,text,h,h,'ACTIVE',0])+')')
        article=dict(slug=slug,title=e['title'],summary=e['readingZh'],bodyMarkdown=e['reading'],translationZhMarkdown=e['readingZh'],primaryTopicId=str(tid),otherTopicIds=[],genreIds=['235'],purposeIds=['255'],keywords=[],rowVersion=0,contentOrigin='ORIGINAL',sourceName='星雨笔录 · 原创主题示例',publishStatus='PUBLISHED',sortOrder=tid,rights=dict(rightsStatus='CLEARED',rightsBasis='本次迁移编写的原创教学示例',originalAuthor='星雨笔录（AI 辅助编写）',licenseNotice='虚构场景，仅作英语阅读示例'))
        frozen=dict(article=article,alignments=alignments,annotations=[],vocabulary=[])
        emit('INSERT INTO sr_english_reading_revision(article_id,revision_no,snapshot_json,editor_account_id,change_note) SELECT @rid,1,'+val(frozen)+','+str(owner)+",'主题示例初始版本' WHERE NOT EXISTS(SELECT 1 FROM sr_english_reading_revision WHERE article_id=@rid)")
        insert('sr_english_writing_article',dict(owner_account_id=owner,title=e['writingTitle'],slug=wslug,summary=e['writingZh'],body_markdown=e['writing'],translation_zh_markdown=e['writingZh'],primary_topic_id=tid,state='COMPLETED',visibility='PUBLIC',public_eligible=1,keywords_json=['原创示例'],published_at=datetime.datetime.now().isoformat(sep=' ',timespec='seconds')))
        emit('SET @wid=(SELECT id FROM sr_english_writing_article WHERE slug='+val(wslug)+')');emit('INSERT IGNORE INTO sr_english_writing_article_tag VALUES (@wid,163),(@wid,259)')
        writing=dict(slug=wslug,title=e['writingTitle'],summary=e['writingZh'],bodyMarkdown=e['writing'],translationZhMarkdown=e['writingZh'],primaryTopicId=str(tid),otherTopicIds=[],genreIds=['163'],purposeIds=['259'],keywords=['原创示例'],rowVersion=0,state='COMPLETED',visibility='PUBLIC')
        emit('INSERT INTO sr_english_writing_revision(article_id,revision_no,snapshot_json,editor_account_id,change_note) SELECT @wid,1,'+val(writing)+','+str(owner)+",'主题示例初始版本' WHERE NOT EXISTS(SELECT 1 FROM sr_english_writing_revision WHERE article_id=@wid)")
    # Daily summaries match surviving visit events after fixture removal.
    emit('DELETE FROM sr_analytics_daily_content');emit("INSERT INTO sr_analytics_daily_content(stat_date,content_type,content_id,view_count,generated_at) SELECT DATE(occurred_at),content_type,content_id,COUNT(*),NOW(3) FROM sr_analytics_event WHERE event_type='CONTENT_VIEW' AND content_type IS NOT NULL AND content_id IS NOT NULL GROUP BY DATE(occurred_at),content_type,content_id")
    emit('DELETE FROM sr_analytics_daily_site');emit("INSERT INTO sr_analytics_daily_site(stat_date,page_view_count,content_view_count,generated_at) SELECT DATE(occurred_at),SUM(event_type='PAGE_VIEW'),SUM(event_type='CONTENT_VIEW'),NOW(3) FROM sr_analytics_event GROUP BY DATE(occurred_at)")
    emit('DELETE FROM sr_analytics_daily_referrer');emit("INSERT INTO sr_analytics_daily_referrer(stat_date,referrer_category,view_count,generated_at) SELECT DATE(occurred_at),referrer_category,COUNT(*),NOW(3) FROM sr_analytics_event WHERE event_type='PAGE_VIEW' GROUP BY DATE(occurred_at),referrer_category")
    # Abort rolls back the entire transaction if counts/text/protected data fail verification.
    for table,source,key,body in [('sr_blog_post','blog_post','slug','body_markdown'),('sr_tutorial_chapter','tutorial_node','id','body_markdown'),('sr_english_grammar_lesson','english_grammar_lesson','id','body_markdown')]:
        where=" WHERE s.node_type='CHAPTER'" if source=='tutorial_node' else ''
        emit('INSERT INTO refresh_guard VALUES (IF((SELECT COUNT(*) FROM '+SOURCE+'.'+source+' s LEFT JOIN '+table+' d ON d.'+key+'=s.'+key+where+(' AND ' if where else ' WHERE ')+"(d.id IS NULL OR NOT(SHA2(d."+body+',256)<=>SHA2('+canonical_expr('s.'+body,catalog)+',256))))=0,1,0))')
    for table,n in [('sr_blog_post',13),('sr_tutorial',39),('sr_tutorial_chapter',2181),('sr_english_vocabulary_word',7053),('sr_english_grammar_lesson',42),('sr_message',0)]:emit('INSERT INTO refresh_guard VALUES (IF((SELECT COUNT(*) FROM '+table+')='+str(n)+',1,0))')
    emit('INSERT INTO refresh_guard VALUES (IF((SELECT COUNT(*) FROM sr_english_reading_article WHERE slug LIKE '+val('topic-%-reading-example')+')=144,1,0))')
    emit('INSERT INTO refresh_guard VALUES (IF((SELECT COUNT(*) FROM sr_english_writing_article WHERE slug LIKE '+val('topic-%-writing-example')+')=144,1,0))')
    for table in ['sr_english_vocabulary_memory','sr_english_vocabulary_mode_memory','sr_english_vocabulary_review_log']:
        if table in dstschema:
            retained=[x for x in dst[table] if x.get('account_id') not in bad['account_id']]
            emit('INSERT INTO refresh_guard VALUES (IF((SELECT COUNT(*) FROM '+table+')='+str(len(retained))+',1,0))')
    sql.append('COMMIT;')
    (out/'plan.sql').write_text('\n'.join(sql),encoding='utf-8')
    report=dict(source=SOURCE,target=a.target,fixtureIds=bad,deletedCounts=deleted,readingExamples=144,writingExamples=144,recoveredImageSources=len(manifest),pendingMedia=[],originalPortfolioMedia=src['portfolio_project_media'],operationalTablesNotImported=['flyway_schema_history','account_session','admin_invitation','email_verification_challenge','admin_audit_log','content_review_request'],applied=False)
    if a.apply:
        assert a.backup_report,'--apply requires an independently verified restore report'
        backup=json.loads(Path(a.backup_report).read_text(encoding='utf-8'));assert backup['restoredAllTables'] and (a.target==backup['database'] or a.target==backup['probe'])
        dump=Path(backup['backup']);dump=dump if dump.is_absolute() else PROJECT.parent/dump
        assert hashlib.sha256(dump.read_bytes()).hexdigest()==backup['sha256']
        run('\n'.join(sql),a.target);report['applied']=True
    (out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({k:report[k] for k in ['target','deletedCounts','readingExamples','writingExamples','applied']},ensure_ascii=False))

if __name__=='__main__':main()
