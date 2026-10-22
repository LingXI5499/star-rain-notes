# -*- coding: utf-8 -*-
import json, os

BASE = r"E:/star-rain/star-rain-notes-site/src/content-data/tutorials"

def load(slug):
    with open(os.path.join(BASE, slug, 'course.json'), encoding='utf-8') as f:
        return json.load(f)

def save(course):
    d = os.path.join(BASE, course['slug'])
    with open(os.path.join(d, 'course.json'), 'w', encoding='utf-8') as f:
        json.dump(course, f, ensure_ascii=False, indent=2); f.write('\n')

def recount(course):
    course['chapterCount'] = sum(len(g['chapters']) for g in course['groups'])
    return course

# 1) 读取企业级，切出分组 13~17（index 12~16）
ent = load('java-enterprise-development')
keep = ent['groups'][:12]          # 分组 1~12
move = ent['groups'][12:]          # 分组 13~17（测试/部署/Docker/CI-CD/可观测）

ent['groups'] = keep
ent = recount(ent)
ent['description'] = ('Java 技术体系第二层：覆盖企业级开发工具链、Web/HTTP、Vue、数据库与 JDBC、'
                      'Servlet、Spring、MVC、MyBatis、Spring Boot、业务分层与身份安全。')
save(ent)
print('[OK] 企业级保留 %d 组 %d 章' % (len(ent['groups']), ent['chapterCount']))

# 2) 新建「Java 交付、部署与运维」，slug 重新编号为 1~5
new_slug = 'java-delivery-deployment-ops'
for gi, g in enumerate(move, 1):
    for ci, ch in enumerate(g['chapters'], 1):
        new_cslug = '%d-%d' % (gi, ci)
        ch['slug'] = new_cslug
        ch['path'] = '/tutorials/%s/%s/' % (new_slug, new_cslug)

new_course = {
    'slug': new_slug,
    'title': 'Java 交付、部署与运维',
    'description': ('Java 技术体系配套工程化教程：覆盖测试体系与交付质量、Linux/Nginx 部署、'
                    'Docker 与 Compose、CI/CD 版本发布、可观测性与故障排查。'),
    'category': 'Java全栈知识体系',
    'categorySlug': 'javaquanzhan',
    'publishedAt': '2026-09-22',
    'chapterCount': sum(len(g['chapters']) for g in move),
    'wordCount': 0,
    'categoryId': '1',
    'groups': move,
    'path': '/tutorials/%s/' % new_slug,
}
new_course = recount(new_course)

# 写 course.json + 生成章节占位符
d = os.path.join(BASE, new_slug)
ch_dir = os.path.join(d, 'chapters')
os.makedirs(ch_dir, exist_ok=True)
save(new_course)
for g in new_course['groups']:
    for ch in g['chapters']:
        hp = os.path.join(ch_dir, ch['slug'] + '.html')
        with open(hp, 'w', encoding='utf-8') as f:
            f.write('<p>待编写</p>\n')

print('[OK] 新建 %s：%d 组 %d 章' % (new_slug, len(new_course['groups']), new_course['chapterCount']))
print()
for i, g in enumerate(move, 1):
    print('  %d. %s (%d章)' % (i, g['title'], len(g['chapters'])))
