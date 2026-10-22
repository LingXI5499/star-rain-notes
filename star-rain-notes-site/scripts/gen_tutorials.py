# -*- coding: utf-8 -*-
import json, re, os

BASE = r"E:/star-rain/star-rain-notes-site/src/content-data/tutorials"
SRC = r"F:/work/星雨笔录/教程"

def read(p):
    with open(p, encoding='utf-8') as f:
        return f.read()

def clean_title(t):
    return re.sub(r'[（(]P\d+(?:/P\d+)?[)）]\s*$', '', t).strip()

def parse_java_grouped(text):
    groups = []
    cur = None
    for line in text.split('\n'):
        line = line.rstrip()
        m = re.match(r'^##\s+(?:分组|Group)\s*(\d+)\s*[｜|]\s*(.+)$', line)
        if m:
            cur = {'title': clean_title(m.group(2)), 'chapters': []}
            groups.append(cur)
            continue
        m = re.match(r'^-\s+(\d+)\.(\d+)\s+(.+)$', line) or re.match(r'^(\d+)\.(\d+)\s+(.+)$', line)
        if m and cur is not None:
            cur['chapters'].append({'title': clean_title(m.group(3))})
    return groups

def parse_agent(text):
    tutorials = []
    cur_t = None; cur_g = None
    for line in text.split('\n'):
        line = line.rstrip()
        m = re.match(r'^#\s+教程([一二三四五六七八九十\d]+)\s*[｜|]\s*(.+)$', line)
        if m:
            cur_t = {'title': m.group(2).strip(), 'groups': []}
            tutorials.append(cur_t); cur_g = None; continue
        m = re.match(r'^##\s+分组\s*(\d+)\s*[｜|]\s*(.+)$', line)
        if m and cur_t is not None:
            cur_g = {'title': clean_title(m.group(2)), 'chapters': []}
            cur_t['groups'].append(cur_g); continue
        m = re.match(r'^-\s+(\d+)\.(\d+)\.(\d+)\s+(.+)$', line)
        if m and cur_g is not None:
            cur_g['chapters'].append({'title': m.group(4).strip()})
    return tutorials

def make_chapter(slug, title, course_slug):
    return {'slug': slug, 'title': title, 'description': title,
            'path': '/tutorials/%s/%s/' % (course_slug, slug), 'wordCount': 0, 'hasBody': True}

def build_course(slug, title, description, category, categorySlug, categoryId, groups):
    course_groups = []
    for gi, g in enumerate(groups, 1):
        chapters = [make_chapter('%d-%d' % (gi, ci), ch['title'], slug) for ci, ch in enumerate(g['chapters'], 1)]
        course_groups.append({'title': g['title'], 'chapters': chapters})
    chapterCount = sum(len(g['chapters']) for g in groups)
    return {'slug': slug, 'title': title, 'description': description,
            'category': category, 'categorySlug': categorySlug, 'publishedAt': '2026-09-22',
            'chapterCount': chapterCount, 'wordCount': 0, 'categoryId': categoryId,
            'groups': course_groups, 'path': '/tutorials/%s/' % slug}

def write_course(course, clean_chapters=False):
    d = os.path.join(BASE, course['slug'])
    ch_dir = os.path.join(d, 'chapters')
    os.makedirs(ch_dir, exist_ok=True)
    if clean_chapters:
        for f in os.listdir(ch_dir):
            os.remove(os.path.join(ch_dir, f))
    with open(os.path.join(d, 'course.json'), 'w', encoding='utf-8') as f:
        json.dump(course, f, ensure_ascii=False, indent=2); f.write('\n')
    for g in course['groups']:
        for ch in g['chapters']:
            hp = os.path.join(ch_dir, ch['slug'] + '.html')
            if not os.path.exists(hp):
                with open(hp, 'w', encoding='utf-8') as f:
                    f.write('<p>待编写</p>\n')
    return course

CAT_JAVA = ('Java全栈知识体系', 'javaquanzhan', '1')
CAT_AGENT = ('智能体开发知识体系', '484381842269', '4')

enterprise = parse_java_grouped(read(os.path.join(SRC, 'Java全栈知识体系', 'Java企业级.md')))
micro = parse_java_grouped(read(os.path.join(SRC, 'Java全栈知识体系', '分布式系统与微服务.md')))
mq = parse_java_grouped(read(os.path.join(SRC, 'Java全栈知识体系', '缓存消息队列.md')))
ai = parse_java_grouped(read(os.path.join(SRC, 'Java全栈知识体系', '星雨笔录-Java全栈与AI应用开发学习路线-基础大纲.md')))
agent = parse_agent(read(os.path.join(SRC, '智能体开发知识体系', '98E9A8ACE595-Python-Agent99E8BDBDE5808FE699A8E493B3%BB-V4.0.md')))

courses = []
courses.append(build_course('java-enterprise-development', 'Java 企业级开发',
    'Java 技术体系第二层：覆盖企业级开发工具链、Web/HTTP、Vue、数据库与 JDBC、Servlet、Spring、MVC、MyBatis、Spring Boot、安全、测试、部署与运维。',
    *CAT_JAVA, enterprise))
courses.append(build_course('java-microservices-distributed', 'Java 微服务与分布式',
    'Java 技术体系第三层：分布式系统核心问题、服务边界、Spring Cloud 生态、容错治理、数据一致性与可观测性。',
    *CAT_JAVA, micro))
courses.append(build_course('java-redis-mq', 'Redis 与消息队列',
    'Java 后端中间件进阶：Redis 数据模型、缓存工程与一致性、消息队列通用原理与 RocketMQ/Kafka/RabbitMQ 实践。',
    *CAT_JAVA, mq))
courses.append(build_course('java-ai-application-development', 'Java AI 应用开发',
    'Java 技术体系第四层：在 Spring Boot 之上接入 LLM、Prompt 工程、Spring AI、RAG、Tool Calling、MCP 与 Agent。',
    *CAT_JAVA, ai))

agent_slugs = ['agent-fundamentals', 'agent-core-protocols', 'agent-frameworks-multiagent', 'agent-advanced-production']
agent_descs = [
    'Python Agent 工程基础、LLM 调用、Agent 原理与从零实现 MiniAgent。',
    '工具系统、上下文与记忆、RAG、MCP、Skills、A2A 等 Agent 核心能力与协议生态。',
    '深入学习 LangGraph，比较主流 Python 框架，掌握状态化编排与多智能体协作。',
    'Harness、Coding Agent、专项智能体、源码分析、安全、评测、部署与综合项目。',
]
for slug, desc, t in zip(agent_slugs, agent_descs, agent):
    courses.append(build_course(slug, t['title'], desc, *CAT_AGENT, t['groups']))

for c in courses:
    clean = c['slug'] in ('java-enterprise-development', 'java-microservices-distributed', 'java-ai-application-development')
    write_course(c, clean_chapters=clean)
    print('[OK] %s: %d 组 %d 章 (clean=%s)' % (c['slug'], len(c['groups']), c['chapterCount'], clean))

print('\n总计生成', len(courses), '门课程')
