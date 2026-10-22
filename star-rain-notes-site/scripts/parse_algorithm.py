# -*- coding: utf-8 -*-
"""解析算法文档，生成「算法」教程 course.json + 章节 HTML 正文。"""
import json, os, re

SRC = r"F:/临时工作区/95E6AEBBE684B8E797B3E7BBBBE699A8E58C95E788.md"
BASE = r"E:/star-rain/star-rain-notes-site/src/content-data/tutorials"
COURSE_SLUG = 'algorithm'
CAT_NAME = '计算机基础知识'
CAT_SLUG = '724893225146'
CAT_ID = '2'

with open(SRC, encoding='utf-8') as f:
    text = f.read()
lines = text.split('\n')

def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')

# ---------- 模块定位 ----------
mod_positions = []
for i, line in enumerate(lines):
    m = re.match(r'^### (M\d+)[｜|](.+)$', line)
    if m:
        mod_positions.append((i, m.group(1), m.group(2).strip()))

# ---------- 第0章导学 ----------
ch0_start = part1_start = None
for i, line in enumerate(lines):
    if line.startswith('## 第 0 章'):
        ch0_start = i
    if line.startswith('## 第一部分'):
        part1_start = i
        break
ch0_lines = lines[ch0_start:part1_start]
ch0_title = re.match(r'^## 第 0 章[｜|](.+)$', ch0_lines[0]).group(1).strip()
ch0_items = [re.match(r'^(\d+)\.\s+(.+)$', l).group(2).strip() for l in ch0_lines if re.match(r'^(\d+)\.\s+(.+)$', l)]
# 导学说明文字（第0章标题到知识点列表之间的段落）
ch0_intro = []
for l in ch0_lines[1:]:
    if re.match(r'^\d+\.\s+', l):
        break
    s = l.strip()
    if s and not s.startswith('#'):
        ch0_intro.append(s)

# ---------- 题目 ----------
problems = []
for line in lines:
    m = re.match(r'^(\d+)\. \[ \] \*\*(简单|中等|困难)\*\* \[LC (\d+)[｜|]([^\]]+)\]\(([^)]+)\) — (.+)$', line)
    if m:
        problems.append({
            'seq': int(m.group(1)), 'diff': m.group(2), 'lc': m.group(3),
            'name': m.group(4).strip(), 'url': m.group(5), 'desc': m.group(6).strip(),
        })

# ---------- 综合复习边界 ----------
review_start = None
for i, line in enumerate(lines):
    if line.startswith('## 综合复习与模拟训练'):
        review_start = i
        break

def parse_units(block_lines):
    units = []
    cur = None
    for line in block_lines:
        m = re.match(r'^(\d+)\. \*\*(.+?)\*\*\s*$', line)
        if m:
            cur = {'title': m.group(2).strip(), 'lo': None, 'hi': None, 'note': ''}
            units.append(cur)
            continue
        if cur is not None:
            m2 = re.match(r'^\s+-\s*(.+)$', line)
            if m2:
                content = m2.group(1).strip()
                # 捕获题单序号范围
                r = re.match(r'题单序号[:：]\s*(\d+)\s*[—–-]\s*(\d+)', content)
                if r:
                    cur['lo'], cur['hi'] = int(r.group(1)), int(r.group(2))
                # 完整说明行（题单序号+力扣题号+共N题）
                if content.startswith('题单序号'):
                    cur['note'] = content
    return units

# ---------- 解析所有模块 ----------
groups = []
# 导学
groups.append({'num': 0, 'title': '第 0 章｜' + ch0_title, 'chapters': [
    {'title': ch0_title, 'kind': 'ch0', 'items': ch0_items, 'intro': ch0_intro}
]})
for idx, (pos, code, title) in enumerate(mod_positions):
    end = mod_positions[idx+1][0] if idx+1 < len(mod_positions) else (review_start if review_start else len(lines))
    block = lines[pos:end]
    units = parse_units(block)
    chapters = []
    for u in units:
        probs = [p for p in problems if u['lo'] is not None and u['lo'] <= p['seq'] <= u['hi']] if u['lo'] is not None else []
        chapters.append({'title': u['title'], 'kind': 'unit', 'problems': probs, 'note': u['note']})
    groups.append({'num': idx + 1, 'title': '%s｜%s' % (code, title), 'chapters': chapters})

# ---------- 生成 HTML ----------
def unit_html(ch):
    """学习单元章节正文"""
    parts = ['<h2>%s</h2>' % esc(ch['title'])]
    if ch['note']:
        parts.append('<p>%s</p>' % esc(ch['note']))
    if ch['problems']:
        items = []
        for p in ch['problems']:
            diff = p['diff']
            items.append(
                '<li><strong>%s</strong> <a href="%s" target="_blank" rel="noopener noreferrer">LC %s｜%s</a> — %s</li>'
                % (esc(diff), esc(p['url']), esc(p['lc']), esc(p['name']), esc(p['desc']))
            )
        parts.append('<ol>%s</ol>' % ''.join(items))
    return '\n'.join(parts)

def ch0_html(ch):
    """导学章节正文"""
    parts = ['<h2>%s</h2>' % esc(ch['title'])]
    if ch['intro']:
        segs = []
        for s in ch['intro']:
            s = esc(s)
            s = re.sub(r'\*\*(.+?)\*\*', r'<strong>\1</strong>', s)
            segs.append(s)
        parts.append('<p>%s</p>' % '<br/>'.join(segs))
    if ch['items']:
        parts.append('<ol>' + ''.join('<li>%s</li>' % esc(s) for s in ch['items']) + '</ol>')
    return '\n'.join(parts)

# ---------- 写 course.json 和章节文件 ----------
course_dir = os.path.join(BASE, COURSE_SLUG)
ch_dir = os.path.join(course_dir, 'chapters')
os.makedirs(ch_dir, exist_ok=True)

course_groups = []
total = 0
for g in groups:
    gch = []
    for ci, ch in enumerate(g['chapters'], 1):
        slug = '%d-%d' % (g['num'], ci)
        html = ch0_html(ch) if ch['kind'] == 'ch0' else unit_html(ch)
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
    course_groups.append({'title': g['title'], 'chapters': gch})

course = {
    'slug': COURSE_SLUG,
    'title': '算法',
    'description': '以 Java 系统学习数据结构与算法：25 个专题模块、300 道力扣题，从暴力推导优化，覆盖数组、哈希、双指针、二分、链表、树、堆、回溯、贪心、图与动态规划。',
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

print('[OK] 生成 %s：%d 组 %d 章' % (COURSE_SLUG, len(course_groups), total))
print('题目总数:', sum(len(ch['problems']) for g in groups for ch in g['chapters'] if ch['kind']=='unit'))
