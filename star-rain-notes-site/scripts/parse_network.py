# -*- coding: utf-8 -*-
"""解析《计算机网络教程》文档，生成「网络」教程 course.json + 章节 HTML 正文。"""
import json, os, re

SRC = r"F:/work/星雨笔录/博客/未归入专栏/网络.md"
BASE = r"E:/star-rain/star-rain-notes-site/src/content-data/tutorials"
COURSE_SLUG = 'network'
CAT_NAME = '计算机基础知识'
CAT_SLUG = '724893225146'
CAT_ID = '2'

with open(SRC, encoding='utf-8') as f:
    text = f.read()

def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

# ---------- 解析分组和章节 ----------
lines = text.split('\n')
groups = []
cur_group = None
cur_chapter = None

for line in lines:
    gm = re.match(r'^## 分组\s*(\d+)[｜|](.+)$', line)
    if gm:
        cur_group = {'num': int(gm.group(1)), 'title': gm.group(2).strip(), 'chapters': []}
        groups.append(cur_group)
        cur_chapter = None
        continue
    # 非分组的 ## 标题（参考资料、目录组织说明等）终止收集
    if re.match(r'^##\s+', line):
        cur_chapter = None
        continue
    cm = re.match(r'^### 章节\s*(\d+)\.(\d+)[｜|](.+)$', line)
    if cm:
        cur_chapter = {'num': int(cm.group(2)), 'title': cm.group(3).strip(), 'items': []}
        cur_group['chapters'].append(cur_chapter)
        continue
    if cur_chapter is not None:
        km = re.match(r'^\*\*(知识点(?:与实践任务|与验收要求)?)\*\*\s*(.*)$', line)
        if km:
            if km.group(2).strip():
                cur_chapter['items'].append(km.group(2).strip())
            continue
        if line.strip():
            cur_chapter['items'].append(line.strip())

# ---------- 拆分知识点条目 ----------
def split_items(raw_lines):
    joined = ' '.join(raw_lines).strip()
    # 仅在「空格 - 空格」处分隔，避免误拆连字符
    parts = re.split(r'\s+-\s+', joined)
    items = []
    for p in parts:
        p = p.strip().lstrip('-').strip()
        if p:
            items.append(p.replace('**', ''))
    return items

# ---------- 生成 HTML 正文 ----------
def chapter_html(title, items):
    segs = ['<h2>%s</h2>' % esc(title)]
    if items:
        segs.append('<ul>' + ''.join('<li>%s</li>' % esc(i) for i in items) + '</ul>')
    return '\n'.join(segs)

# ---------- 写 course.json 和章节文件 ----------
course_dir = os.path.join(BASE, COURSE_SLUG)
ch_dir = os.path.join(course_dir, 'chapters')
os.makedirs(ch_dir, exist_ok=True)

course_groups = []
total = 0
total_items = 0
for g in groups:
    gch = []
    for ch in g['chapters']:
        items = split_items(ch['items'])
        slug = '%d-%d' % (g['num'], ch['num'])
        html = chapter_html(ch['title'], items)
        with open(os.path.join(ch_dir, slug + '.html'), 'w', encoding='utf-8') as f:
            f.write(html + '\n')
        gch.append({
            'slug': slug,
            'title': ch['title'],
            'description': ch['title'],
            'path': '/tutorials/%s/%s/' % (COURSE_SLUG, slug),
            'wordCount': len(html),
            'hasBody': True,
        })
        total += 1
        total_items += len(items)
    course_groups.append({'title': g['title'], 'chapters': gch})

course = {
    'slug': COURSE_SLUG,
    'title': '网络',
    'description': '按照知识依赖顺序系统学习计算机网络：分层模型、IP 与路由、TCP/UDP、DNS、HTTP/HTTPS、Web 安全、企业网络排障，以及 Java 与 Python 网络编程。',
    'category': CAT_NAME,
    'categorySlug': CAT_SLUG,
    'publishedAt': '2026-10-22',
    'chapterCount': total,
    'wordCount': 0,
    'categoryId': CAT_ID,
    'sortOrder': 0,
    'groups': course_groups,
    'path': '/tutorials/%s/' % COURSE_SLUG,
}

with open(os.path.join(course_dir, 'course.json'), 'w', encoding='utf-8') as f:
    json.dump(course, f, ensure_ascii=False, indent=2); f.write('\n')

print('[OK] 生成 %s：%d 组 %d 章，知识点条目 %d 条' % (COURSE_SLUG, len(course_groups), total, total_items))
for g in course_groups:
    print('  %s -> %d 章' % (g['title'], len(g['chapters'])))
