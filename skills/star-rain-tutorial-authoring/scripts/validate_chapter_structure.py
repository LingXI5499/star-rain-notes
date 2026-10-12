#!/usr/bin/env python3
"""Check the default Star Rain four-section tutorial format for any subject.

This checks Markdown structure, not subject accuracy or execution results.
Custom tutorial formats must be reviewed against their own requirements.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


CHINESE_NUMBERS = {
    "一": 1,
    "二": 2,
    "三": 3,
    "四": 4,
    "五": 5,
    "六": 6,
    "七": 7,
    "八": 8,
    "九": 9,
    "十": 10,
}
H2_PATTERN = re.compile(r"^([一二三四五六七八九十]+)、\s*(.+)$")
DECIMAL_PATTERN = re.compile(r"^(\d+(?:\.\d+){1,})\s+(.+)$")
BANNED_TEMPLATE_TITLES = {
    "定义",
    "作用",
    "语法",
    "应用场景",
    "代码示例",
    "示例",
    "运行结果",
    "注意事项",
    "一句话解释",
    "一句话讲清",
    "常见错误与易混点",
    "常见错误大全",
    "最终认知模型",
}


def markdown_parts(
    lines: list[str],
) -> tuple[list[tuple[int, int, str]], list[tuple[str, str]], list[str]]:
    """Exclude fenced examples from headings, including nested fence text.

    A closing fence uses the opening character and at least its length.
    This matters for shell comments, Markdown examples and non-Java snippets.
    """
    found: list[tuple[int, int, str]] = []
    blocks: list[tuple[str, str]] = []
    errors: list[str] = []
    fence: tuple[str, int, int] | None = None
    language = ""
    content: list[str] = []
    for line_no, line in enumerate(lines, 1):
        marker = re.match(r"^ {0,3}(`{3,}|~{3,})(.*)$", line)
        if fence is not None:
            char, length, _ = fence
            if (marker and marker.group(1)[0] == char
                    and len(marker.group(1)) >= length
                    and not marker.group(2).strip()):
                blocks.append((language, "\n".join(content)))
                fence = None
            else:
                content.append(line)
            continue
        if marker:
            info = marker.group(2).strip()
            # Backticks in the info string cannot start a backtick fence.
            if marker.group(1)[0] == "`" and "`" in info:
                continue
            fence = (marker.group(1)[0], len(marker.group(1)), line_no)
            language = info.split()[0].lower() if info else ""
            content = []
            continue
        match = re.match(r"^ {0,3}(#{1,6})\s+(.+)$", line)
        if match:
            found.append((line_no, len(match.group(1)), match.group(2).strip()))
    if fence is not None:
        errors.append(f"第 {fence[2]} 行开始的代码围栏未闭合。")
    return found, blocks, errors


def headings(lines: list[str]) -> list[tuple[int, int, str]]:
    """Return navigation headings for callers that only need the outline."""
    return markdown_parts(lines)[0]


def parse_chinese_number(value: str) -> int | None:
    if value in CHINESE_NUMBERS:
        return CHINESE_NUMBERS[value]
    if value.startswith("十") and len(value) == 2:
        return 10 + CHINESE_NUMBERS.get(value[1], 0)
    if value.endswith("十") and len(value) == 2:
        return CHINESE_NUMBERS.get(value[0], 0) * 10
    if "十" in value and len(value) == 3:
        return CHINESE_NUMBERS.get(value[0], 0) * 10 + CHINESE_NUMBERS.get(value[2], 0)
    return None


def visible_title(title: str) -> str:
    match = DECIMAL_PATTERN.match(title)
    return match.group(2).strip() if match else title.strip()


def validate_frontmatter(text: str, errors: list[str], warnings: list[str]) -> None:
    if not text.startswith("---\n"):
        errors.append("缺少 YAML frontmatter。")
        return
    end = text.find("\n---\n", 4)
    if end == -1:
        errors.append("YAML frontmatter 未闭合。")
        return
    frontmatter = text[4:end]
    keys = re.findall(r"(?m)^([A-Za-z_][\w-]*):", frontmatter)
    if keys != ["title", "summary"]:
        errors.append(f"元数据必须且只能按顺序包含 title、summary；当前为 {keys}。")
    for key in ("title", "summary"):
        value = re.search(rf"(?m)^{key}:[ \t]*(.*)$", frontmatter)
        if not value or not value.group(1).strip().strip("\"'").strip():
            errors.append(f"frontmatter 缺少有效的 {key}。")
    title_match = re.search(r'(?m)^title:\s*["\']?(.*?)["\']?\s*$', frontmatter)
    summary_match = re.search(r'(?m)^summary:\s*["\']?(.*?)["\']?\s*$', frontmatter)
    if title_match:
        title = title_match.group(1).strip()
        if len(title) > 30:
            warnings.append(f"标题共 {len(title)} 个字符，建议压缩为简短主题名称。")
    if summary_match:
        summary = summary_match.group(1).strip()
        if len(summary) < 15:
            warnings.append("摘要过短，请同时说明文章范围和学习结果。")
        elif len(summary) > 120:
            warnings.append(f"摘要共 {len(summary)} 个字符，建议压缩为 1～2 句。")


def validate(path: Path) -> tuple[list[str], list[str]]:
    text = path.read_text(encoding="utf-8-sig")
    lines = text.splitlines()
    errors: list[str] = []
    warnings: list[str] = []

    validate_frontmatter(text, errors, warnings)
    found, code_blocks, fence_errors = markdown_parts(lines)
    errors.extend(fence_errors)
    if not found:
        return errors + ["正文没有标题。"], warnings

    if any(level == 1 for _, level, _ in found):
        errors.append("正文出现 H1；title 字段才是页面唯一 H1。")
    for line_no, level, title in found:
        if level not in (2, 3, 4):
            errors.append(f"第 {line_no} 行使用 H{level}；正文标题只允许 H2～H4。")
        if visible_title(title) in BANNED_TEMPLATE_TITLES:
            errors.append(f"第 {line_no} 行把写作标签做成了标题：{title}")
        number = DECIMAL_PATTERN.match(title)
        if number and number.group(1).count(".") >= 3:
            errors.append(f"第 {line_no} 行编号层级过深：{title}")

    h2_items: list[tuple[int, int, str]] = []
    for index, (line_no, level, title) in enumerate(found):
        if level != 2:
            continue
        match = H2_PATTERN.match(title)
        if not match:
            errors.append(f"第 {line_no} 行 H2 应使用“一、标题”形式：{title}")
            continue
        number = parse_chinese_number(match.group(1))
        if number is None:
            errors.append(f"第 {line_no} 行无法识别 H2 编号：{title}")
            continue
        h2_items.append((index, number, match.group(2).strip()))

    if not h2_items:
        return errors + ["没有有效的 H2。"], warnings

    actual_h2_numbers = [item[1] for item in h2_items]
    if actual_h2_numbers != list(range(1, len(h2_items) + 1)):
        errors.append(f"H2 必须从一开始连续编号；当前为 {actual_h2_numbers}。")

    h2_names = [item[2] for item in h2_items]
    h3_by_section: dict[str, list[tuple[int, int, str]]] = {}
    body_by_section: dict[str, str] = {}
    for position, (heading_index, section_number, section_name) in enumerate(h2_items):
        next_index = h2_items[position + 1][0] if position + 1 < len(h2_items) else len(found)
        children = [item for item in found[heading_index + 1:next_index] if item[1] == 3]
        h3_by_section[section_name] = children
        start_line = found[heading_index][0]
        end_line = found[next_index][0] if next_index < len(found) else len(lines) + 1
        body_by_section[section_name] = "\n".join(lines[start_line:end_line - 1])
        expected = [f"{section_number}.{i}" for i in range(1, len(children) + 1)]
        actual: list[str] = []
        for line_no, _, title in children:
            match = DECIMAL_PATTERN.match(title)
            if not match or match.group(1).count(".") != 1:
                errors.append(
                    f"第 {line_no} 行 H3 应使用 {section_number}.1、{section_number}.2…：{title}"
                )
                continue
            actual.append(match.group(1))
        if actual != expected:
            errors.append(
                f"H2「{section_name}」下的 H3 必须从 {section_number}.1 连续编号；当前为 {actual}。"
            )

    required = ["学习目标", "核心知识", "问题", "练习题"]
    if h2_names != required:
        errors.append(f"正文必须且只能依次包含四个 H2：{required}；当前为 {h2_names}。")

    core_children = h3_by_section.get("核心知识", [])
    if not 2 <= len(core_children) <= 6:
        warnings.append(f"核心知识点数量为 {len(core_children)}；通常建议 2～6 个。")

    goal_count = len(re.findall(r"(?m)^-\s+", body_by_section.get("学习目标", "")))
    if not 2 <= goal_count <= 4:
        warnings.append(f"学习目标共 {goal_count} 条；通常建议保留 2～4 条可验证目标。")

    question_count = len(re.findall(r"(?m)^\d+[.)、]\s*", body_by_section.get("问题", "")))
    if not 5 <= question_count <= 10:
        warnings.append(f"问题共 {question_count} 个；通常建议保留 5～10 个关键问题。")

    for index, (line_no, level, title) in enumerate(found):
        if level != 4:
            continue
        match = DECIMAL_PATTERN.match(title)
        if not match or match.group(1).count(".") != 2:
            errors.append(f"第 {line_no} 行 H4 应使用 2.1.1 形式：{title}")
            continue
        # Stop at H2, so an H4 cannot inherit an H3 from the previous section.
        parent = next((item for item in reversed(found[:index]) if item[1] <= 3), None)
        if parent is None or parent[1] != 3:
            errors.append(f"第 {line_no} 行 H4 缺少本节 H3 父级：{title}")
        else:
            parent_match = DECIMAL_PATTERN.match(parent[2])
            if not parent_match or not match.group(1).startswith(parent_match.group(1) + "."):
                errors.append(f"第 {line_no} 行 H4 编号与父级不一致：{title}")

    h3_count = sum(level == 3 for _, level, _ in found)
    h4_count = sum(level == 4 for _, level, _ in found)
    if h3_count > 10:
        warnings.append(f"正文有 {h3_count} 个 H3，请确认每个都是可独立导航的知识点。")
    if h4_count > 4:
        warnings.append(f"正文有 {h4_count} 个 H4，可能拆分过细。")

    # A heuristic reminder for source code, never a requirement to add code.
    # Commands, JSON, data and unrecognized languages receive no comment check.
    comment_markers = {
        "java": ("//", "/*"), "javascript": ("//", "/*"), "js": ("//", "/*"),
        "typescript": ("//", "/*"), "ts": ("//", "/*"),
        "c": ("//", "/*"), "cpp": ("//", "/*"), "c++": ("//", "/*"),
        "csharp": ("//", "/*"), "c#": ("//", "/*"),
        "go": ("//", "/*"), "rust": ("//", "/*"), "kotlin": ("//", "/*"),
        "python": ("#",), "py": ("#",), "ruby": ("#",),
        "sql": ("--", "/*"),
    }
    for index, (language, block) in enumerate(code_blocks, 1):
        markers = comment_markers.get(language)
        if markers and not any(marker in block for marker in markers):
            warnings.append(f"第 {index} 个 {language} 代码块没有注释，请确认代码是否足够自解释。")

    if len(text) > 12000:
        errors.append(f"正文共 {len(text)} 字符，超过 12,000 的结构上限。")
    elif len(text) > 8000:
        warnings.append(f"正文共 {len(text)} 字符，超过 8,000 字符复核线。")
    elif len(text) < 1800:
        warnings.append(f"正文共 {len(text)} 字符，请确认没有因压缩遗漏必要知识。")

    return errors, warnings


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: validate_chapter_structure.py <chapter.md>", file=sys.stderr)
        return 2
    path = Path(sys.argv[1])
    if not path.is_file():
        print(f"ERROR: file not found: {path}", file=sys.stderr)
        return 2
    errors, warnings = validate(path)
    for item in errors:
        print(f"ERROR: {item}")
    for item in warnings:
        print(f"WARNING: {item}")
    if errors:
        print(f"FAIL: {len(errors)} error(s), {len(warnings)} warning(s)")
        return 1
    print(f"PASS: 0 errors, {len(warnings)} warning(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
