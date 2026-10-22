# -*- coding: utf-8 -*-
"""为各教程写入 sortOrder，控制教程中心展示顺序（按学习顺序）。"""
import json, os

BASE = r"E:/star-rain/star-rain-notes-site/src/content-data"

# 分类展示顺序（与 categories.json 的排列一致）
CATEGORY_ORDER = ['1', '2', '6', '7', '4', '3']

# 每个分类内教程的学习顺序（slug 列表）；未列出的排在后面，保持原有相对顺序
SLUG_ORDER = {
    '1': [  # Java全栈知识体系：循序渐进
        'java-se',
        'java-enterprise-development',
        'java-delivery-deployment-ops',
        'java-redis-mq',
        'java-microservices-distributed',
        'java-ai-application-development',
    ],
    '2': [  # 计算机基础知识：算法 → 网络 → 后续数据结构/组成原理/OS/数据库/设计模式
        'algorithm',
        'network',
    ],
    '4': [  # 智能体开发知识体系
        'agent-fundamentals',
        'agent-core-protocols',
        'agent-frameworks-multiagent',
        'agent-advanced-production',
    ],
}

def main():
    catalog = json.load(open(os.path.join(BASE, 'catalog.json'), encoding='utf-8'))
    courses = catalog['courses']

    # 记录各分类内未显式排序教程的原相对顺序
    leftovers = {}
    for c in courses:
        cid = c.get('categoryId')
        if cid not in leftovers:
            leftovers[cid] = []
        explicit = any(c['slug'] in s for s in SLUG_ORDER.values())
        if not explicit:
            leftovers[cid].append(c['slug'])

    def sort_key(c):
        cid = c.get('categoryId')
        cat_rank = CATEGORY_ORDER.index(cid) if cid in CATEGORY_ORDER else len(CATEGORY_ORDER)
        explicit = SLUG_ORDER.get(cid, [])
        if c['slug'] in explicit:
            in_rank = explicit.index(c['slug'])
        else:
            rest = [s for s in leftovers.get(cid, []) ]
            if c['slug'] in rest:
                in_rank = len(explicit) + rest.index(c['slug'])
            else:
                in_rank = len(explicit) + len(rest) + 100
        return (cat_rank * 1000 + in_rank)

    # 写入每个 course.json 的 sortOrder（从 1 开始的全局序号）
    ranked = sorted(courses, key=sort_key)
    for i, c in enumerate(ranked, 1):
        c['sortOrder'] = i
        path = os.path.join(BASE, 'tutorials', c['slug'], 'course.json')
        data = json.load(open(path, encoding='utf-8'))
        data['sortOrder'] = i
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2); f.write('\n')

    print('已写入 %d 门课程的 sortOrder' % len(ranked))
    print()
    print('全局顺序预览（前 12 门）:')
    for c in ranked[:12]:
        print('  %2d. [%s] %s' % (c['sortOrder'], c.get('categoryId'), c['title']))

if __name__ == '__main__':
    main()
