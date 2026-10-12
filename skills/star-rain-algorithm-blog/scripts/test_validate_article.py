#!/usr/bin/env python3
"""Regression tests for article structure; these do not claim to run article algorithms."""
from __future__ import annotations

import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from validate_article import LANGUAGE_LABELS, validate

LANGS = ("c", "cpp", "java", "python", "javascript")
CODE = {
    "c": "// 初始最大值取首项，约定输入非空\nint maximum(const int *a, int n) {\n    int best = a[0];\n    // 逐项维护当前已读前缀的最大值\n    for (int i = 1; i < n; i++) if (a[i] > best) best = a[i];\n    return best;\n}",
    "cpp": "// 输入契约为非空整数序列\n#include <vector>\nint maximum(const std::vector<int>& a) {\n    int best = a[0];\n    // 更新已读前缀的最大值\n    for (int value : a) if (value > best) best = value;\n    return best;\n}",
    "java": "// 调用方提供非空整数数组\nclass Solution {\n    int maximum(int[] a) {\n        int best = a[0];\n        // 维护已读前缀的最大值\n        for (int value : a) if (value > best) best = value;\n        return best;\n    }\n}",
    "python": "# 输入序列至少包含一个元素\ndef maximum(a):\n    best = a[0]\n    # 每轮更新已读前缀的最大值\n    for value in a:\n        if value > best:\n            best = value\n    return best",
    "javascript": "// 非空数组中的元素为安全整数\nfunction maximum(a) {\n    let best = a[0];\n    // 逐项更新已读前缀的最大值\n    for (const value of a) if (value > best) best = value;\n    return best;\n}",
}


def make_group(languages=LANGS) -> str:
    labels = "|".join(LANGUAGE_LABELS.get(lang, lang) for lang in languages)
    bodies = [f"```{lang}\n{CODE[lang]}\n```" for lang in languages]
    return f"::: code-group {labels}\n\n" + "\n\n".join(bodies) + "\n\n:::"


def make_article(languages=LANGS, two_methods=True) -> str:
    chunks = [
        "## 一、题目描述\n\n自拟题：求非空整数序列的最大值；无平台题号，返回最大元素。",
        "## 二、测试用例\n\n### 2.1 黑盒测试\n\n| 输入 | 期望输出 |\n|---|---|\n| [3, -1, 7] | 7 |\n\n### 2.2 白盒测试\n\n严格大于时更新答案，相等时保持原值。",
    ]
    for number, cn, method in [(3, "三", "一"), (4, "四", "二")][:2 if two_methods else 1]:
        chunks.append(f"""## {cn}、解法{method}：前缀最大值

### {number}.1 核心思想

逐项维护已读前缀的最大值。

### {number}.2 执行步骤

1. 取首项初始化答案。
2. 遍历并在更大元素出现时更新。
3. 返回最大值。

### {number}.3 代码实现

{make_group(languages)}

### {number}.4 复杂度分析

时间复杂度 O(n)，额外空间复杂度 O(1)。""")
    chunks.append(("## 五、总结" if two_methods else "## 四、总结") + "\n\n| 解法 | 时间复杂度 |\n|---|---|\n| 前缀最大值 | O(n) |")
    return "\n\n".join(chunks) + "\n"


class ArticleValidationTest(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name) / "article.md"
        self.original = make_article()

    def check(self, text, **kwargs):
        self.path.write_text(text, encoding="utf-8")
        return validate(self.path, **kwargs)

    def test_default_five_languages(self):
        report = self.check(self.original, strict_comments=True)
        self.assertTrue(report.ok, report.errors)
        self.assertEqual((report.methods, report.groups, report.code_panels), (2, 2, 10))

    def test_non_platform_single_method(self):
        report = self.check(make_article(two_methods=False))
        self.assertTrue(report.ok, report.errors)
        self.assertEqual(report.methods, 1)

    def test_requested_language_subset(self):
        report = self.check(make_article(("cpp", "python")), languages=["cpp", "python"], strict_comments=True)
        self.assertTrue(report.ok, report.errors)
        self.assertEqual(report.code_panels, 4)

    def test_subset_does_not_silently_pass_default_profile(self):
        self.assertFalse(self.check(make_article(("cpp", "python"))).ok)

    def test_standard_input_output_single_language(self):
        text = make_article(("cpp",), two_methods=False)
        program = '#include <iostream>\n// 首项初始化，输入保证 n 大于零\nint main() { int n, x, best; std::cin >> n >> best;\n// 读完剩余元素并维护最大值\nfor (int i=1; i<n; i++) { std::cin >> x; if (x>best) best=x; }\nstd::cout << best << "\\n"; }'
        report = self.check(text.replace(CODE["cpp"], program), languages=["cpp"], strict_comments=True)
        self.assertTrue(report.ok, report.errors)

    def test_invalid_labels(self):
        self.assertFalse(self.check(self.original.replace("C|C++|Java|Python|JavaScript", "C|Java|Python", 1)).ok)

    def test_invalid_language_order(self):
        self.assertFalse(self.check(self.original.replace("```cpp", "```java", 1)).ok)

    def test_missing_required_subsection(self):
        self.assertFalse(self.check(self.original.replace("执行步骤", "运行笔记", 1)).ok)

    def test_reordered_subsections(self):
        wrong = self.original.replace("### 3.2 执行步骤", "### 3.2 代码实现", 1).replace("### 3.3 代码实现", "### 3.3 执行步骤", 1)
        self.assertFalse(self.check(wrong).ok)

    def test_code_group_outside_code_subsection(self):
        group = make_group()
        wrong = self.original.replace(group, "", 1).replace("### 3.4 复杂度分析", "### 3.4 复杂度分析\n\n" + group, 1)
        self.assertFalse(self.check(wrong).ok)

    def test_unclosed_group(self):
        self.assertFalse(self.check(self.original.replace("\n\n:::\n\n### 3.4", "\n\n### 3.4", 1)).ok)

    def test_unclosed_ordinary_fence(self):
        self.assertFalse(self.check(self.original + "\n```text\nunclosed").ok)

    def test_literal_code_group_in_fenced_example_is_not_counted(self):
        text = self.original.replace("## 三、", "````markdown\n::: code-group C|Python\n```c\nint demo;\n```\n:::\n````\n\n## 三、", 1)
        report = self.check(text)
        self.assertTrue(report.ok, report.errors)
        self.assertEqual(report.groups, 2)

    def test_python_comments_are_not_article_h1(self):
        self.assertTrue(self.check(self.original).ok)
        self.assertFalse(self.check("# 重复标题\n" + self.original).ok)

    def test_code_comments_do_not_supply_missing_test_sections(self):
        wrong = self.original.replace("### 2.1 黑盒测试", "### 2.1 输入分组").replace("### 2.2 白盒测试", "### 2.2 分支列表")
        wrong = wrong.replace("# 输入序列", "# 黑盒 白盒 输入序列")
        self.assertFalse(self.check(wrong).ok)

    def test_html_space_entity_in_source(self):
        self.assertFalse(self.check(self.original.replace("int best = a[0]", "int&nbsp;best = a[0]", 1)).ok)

    def test_missing_file_returns_error(self):
        self.assertFalse(validate(self.path).ok)

    def test_empty_and_duplicate_profiles_are_rejected(self):
        for languages in [[], ["cpp", "cpp"], ["cpp", ""]]:
            with self.subTest(languages=languages), self.assertRaises(ValueError):
                self.check(self.original, languages=languages)


if __name__ == "__main__":
    unittest.main(verbosity=2)
