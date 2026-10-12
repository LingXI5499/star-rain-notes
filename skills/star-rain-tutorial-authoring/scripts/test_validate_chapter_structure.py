#!/usr/bin/env python3
"""Regression tests for subject-neutral structure checks, not tutorial facts."""

from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from validate_chapter_structure import headings, validate


def chapter(title: str = "平均数与中位数", example: str = "") -> str:
    """Use a short fixture; length warnings are expected, not test failures."""
    return f'''---
title: "{title}"
summary: "理解本章概念的适用条件，并通过独立练习检查学习结果。"
---

## 一、学习目标

- 能说明本章的核心概念与适用条件。
- 能独立完成给定材料的练习并核对结果。

## 二、核心知识

### 2.1 基础关系

介绍概念与成立条件。
{example}

### 2.2 应用边界

比较不同场景中的选择与限制。

## 三、问题

1. 概念之间有什么关系？
2. 使用方法需要什么前提？
3. 哪种情况下结果会变化？
4. 如何核对结果？
5. 何时不应使用该方法？

## 四、练习题

### 4.1 应用与验证

使用给定材料完成任务，并写出过程与核对方法。
'''


class ChapterValidatorTests(unittest.TestCase):
    def check(self, text: str) -> tuple[list[str], list[str]]:
        """Each case uses its own UTF-8 file and leaves no repository fixtures."""
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "chapter.md"
            path.write_text(text, encoding="utf-8")
            return validate(path)

    def test_non_programming_topic_needs_no_code(self) -> None:
        errors, warnings = self.check(chapter())
        self.assertEqual([], errors)
        self.assertFalse(any("代码" in item or "Java" in item for item in warnings))

    def test_language_framework_database_and_tool_topics(self) -> None:
        cases = (
            ("Python 列表", "```python\n# 新建列表供本例使用\nitems = [1, 2]\n```"),
            ("Vue 响应式状态", "```typescript\n// 为当前视图维护计数\nconst count = ref(0)\n```"),
            ("SQL 查询", "```sql\n-- 只取当前示例需要的字段\nSELECT id FROM items;\n```"),
            ("Git 安装", "```sh\ngit --version\n```"),
            ("电子表格筛选", "依次选择数据范围、筛选条件，核对保留的记录。"),
            ("JavaSE 变量", "```java\n// 保存当前计数\nint count = 1;\n```"),
        )
        for title, example in cases:
            with self.subTest(topic=title):
                errors, warnings = self.check(chapter(title, example))
                self.assertEqual([], errors)
                self.assertFalse(any("代码块没有注释" in item for item in warnings))

    def test_source_comment_reminder_supports_multiple_languages(self) -> None:
        for language, code in (("python", "items = []"), ("sql", "SELECT 1;"),
                               ("typescript", "const count = 1;"), ("java", "int count = 1;")):
            with self.subTest(language=language):
                errors, warnings = self.check(chapter(example=f"```{language}\n{code}\n```"))
                self.assertEqual([], errors)
                self.assertTrue(any(f"{language} 代码块没有注释" in item for item in warnings))

    def test_data_or_command_block_needs_no_comment(self) -> None:
        for language in ("json", "text", "sh", ""):
            with self.subTest(language=language):
                errors, warnings = self.check(chapter(example=f"```{language}\n1\n```"))
                self.assertEqual([], errors)
                self.assertFalse(any("代码块没有注释" in item for item in warnings))

    def test_headings_in_backtick_example_are_ignored(self) -> None:
        errors, _ = self.check(chapter(example="```python\n# 例子里的注释\n## 不是目录\n```"))
        self.assertEqual([], errors)

    def test_headings_in_tilde_example_are_ignored(self) -> None:
        errors, _ = self.check(chapter(example="~~~text\n# 示例标题\n~~~"))
        self.assertEqual([], errors)

    def test_long_fence_can_contain_short_fences(self) -> None:
        errors, _ = self.check(chapter(example="````markdown\n```python\n# 示例\n```\n````"))
        self.assertEqual([], errors)

    def test_fence_closer_may_be_longer(self) -> None:
        errors, _ = self.check(chapter(example="~~~text\n# 示例\n~~~~"))
        self.assertEqual([], errors)

    def test_unclosed_or_mismatched_fences_fail(self) -> None:
        for example in ("```text\n例子", "```text\n例子\n~~~", "````text\n例子\n```"):
            with self.subTest(example=example):
                errors, _ = self.check(chapter(example=example))
                self.assertTrue(any("围栏未闭合" in item for item in errors))

    def test_missing_section_fails(self) -> None:
        errors, _ = self.check(chapter().replace("## 三、问题", "## 三、总结"))
        self.assertTrue(any("四个 H2" in item for item in errors))

    def test_body_h1_fails(self) -> None:
        errors, _ = self.check(chapter() + "\n# 重复页面标题\n")
        self.assertTrue(any("正文出现 H1" in item for item in errors))

    def test_numbering_gap_fails(self) -> None:
        errors, _ = self.check(chapter().replace("2.2 应用边界", "2.3 应用边界"))
        self.assertTrue(any("连续编号" in item for item in errors))

    def test_orphan_h4_fails(self) -> None:
        errors, _ = self.check(chapter().replace("### 2.1 基础关系", "#### 2.1.1 基础关系"))
        self.assertTrue(any("缺少本节 H3 父级" in item for item in errors))

    def test_h4_cannot_inherit_parent_from_previous_section(self) -> None:
        errors, _ = self.check(chapter().replace("## 三、问题", "## 三、问题\n\n#### 2.2.1 越界标题"))
        self.assertTrue(any("缺少本节 H3 父级" in item for item in errors))

    def test_h4_parent_must_match(self) -> None:
        errors, _ = self.check(chapter(example="#### 2.2.1 错误父级"))
        self.assertTrue(any("与父级不一致" in item for item in errors))

    def test_valid_h4(self) -> None:
        errors, _ = self.check(chapter(example="#### 2.1.1 成立条件\n说明条件。"))
        self.assertEqual([], errors)

    def test_empty_metadata_fails(self) -> None:
        for value in ('""', "''", "   "):
            with self.subTest(value=value):
                text = chapter().replace('title: "平均数与中位数"', f"title: {value}")
                errors, _ = self.check(text)
                self.assertTrue(any("有效的 title" in item for item in errors))

    def test_utf8_bom_is_accepted(self) -> None:
        errors, _ = self.check("\ufeff" + chapter())
        self.assertEqual([], errors)

    def test_headings_allow_three_space_indent(self) -> None:
        self.assertEqual([(1, 2, "一、学习目标")], headings(["   ## 一、学习目标"]))

    def test_cli_exit_codes(self) -> None:
        script = Path(__file__).with_name("validate_chapter_structure.py")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "chapter.md"
            for text, expected in ((chapter(), 0), (chapter() + "\n# H1\n", 1)):
                path.write_text(text, encoding="utf-8")
                result = subprocess.run([sys.executable, str(script), str(path)],
                                        capture_output=True, check=False)
                self.assertEqual(expected, result.returncode)
            missing = Path(directory) / "missing.md"
            result = subprocess.run([sys.executable, str(script), str(missing)],
                                    capture_output=True, check=False)
            self.assertEqual(2, result.returncode)


if __name__ == "__main__":
    unittest.main()
