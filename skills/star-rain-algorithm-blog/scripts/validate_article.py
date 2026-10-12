#!/usr/bin/env python3
"""星雨笔录算法题解 Markdown 静态结构验收（仅使用 Python 标准库）。

用法：python scripts/validate_article.py article.md [--languages cpp,python] [--strict-comments] [--json]

注意：此脚本不会编译/运行代码、验证算法正确性或测量覆盖率。
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import asdict, dataclass, field
from pathlib import Path

EXPECTED_LABELS = ["C", "C++", "Java", "Python", "JavaScript"]
EXPECTED_LANGS = ["c", "cpp", "java", "python", "javascript"]
LANGUAGE_LABELS = dict(zip(EXPECTED_LANGS, EXPECTED_LABELS))
LANGUAGE_LABELS.update(go="Go", rust="Rust", kotlin="Kotlin", typescript="TypeScript", csharp="C#")
METHOD_HEADER = re.compile(r"^##\s+[^\n]*解法[一二三四五六七八九十\d]+[：:]", re.M)
H2 = re.compile(r"^##\s+(.+?)\s*$", re.M)
H3 = re.compile(r"^###\s+(.+?)\s*$", re.M)
GROUP_START = re.compile(r"^:::\s+code-group(?:\s+(.*?))?\s*$")
FENCE_OPEN = re.compile(r"^\s*(`{3,}|~{3,})([\w#+-]*)\s*$")
FENCE_CLOSE = re.compile(r"^\s*(`{3,}|~{3,})\s*$")


@dataclass
class CodePanel:
    language: str
    lines: list[str] = field(default_factory=list)
    start_line: int = 0


@dataclass
class CodeGroup:
    start_line: int
    end_line: int = 0
    labels: list[str] = field(default_factory=list)
    panels: list[CodePanel] = field(default_factory=list)


@dataclass
class Report:
    file: str
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    groups: int = 0
    methods: int = 0
    code_panels: int = 0

    @property
    def ok(self) -> bool:
        return not self.errors

    def error(self, message: str):
        self.errors.append(message)

    def warn(self, message: str):
        self.warnings.append(message)


def scan_groups(lines: list[str], report: Report) -> list[CodeGroup]:
    groups: list[CodeGroup] = []
    group: CodeGroup | None = None
    fence: str | None = None
    panel: CodePanel | None = None
    outer_fence: str | None = None

    for lineno, line in enumerate(lines, 1):
        stripped = line.strip()
        if group is None:
            # A code-group example inside an ordinary Markdown fence is displayed source,
            # not a real container. Ignore its headings and nested marker-looking lines.
            if outer_fence is not None:
                close = FENCE_CLOSE.fullmatch(stripped)
                if close and close.group(1)[0] == outer_fence[0] and len(close.group(1)) >= len(outer_fence):
                    outer_fence = None
                continue
            outside = FENCE_OPEN.fullmatch(stripped)
            if outside:
                outer_fence = outside.group(1)
                continue
            match = GROUP_START.match(stripped)
            if match:
                labels_text = (match.group(1) or "").strip()
                group = CodeGroup(lineno, labels=[x.strip() for x in labels_text.split("|") if x.strip()])
                groups.append(group)
            elif stripped.startswith("::: code-group"):
                report.error(f"第 {lineno} 行：code-group 开始标记不是独立有效行")
            continue

        if fence is not None:
            close = FENCE_CLOSE.fullmatch(stripped)
            if close and close.group(1)[0] == fence[0] and len(close.group(1)) >= len(fence):
                fence = None
                panel = None
            elif panel is not None:
                panel.lines.append(line)
            continue

        if stripped == ":::":
            group.end_line = lineno
            group = None
            continue

        match = FENCE_OPEN.fullmatch(stripped)
        if match:
            fence = match.group(1)
            panel = CodePanel(match.group(2).lower(), start_line=lineno)
            group.panels.append(panel)
            continue

        if stripped and not stripped.startswith("<!--"):
            report.error(f"第 {lineno} 行：代码组中出现代码围栏之外的内容：{stripped[:65]}")

    if fence is not None:
        report.error("文件结尾仍有未关闭的代码围栏（```）")
    if group is not None:
        report.error(f"第 {group.start_line} 行：代码组未以独立的 ::: 正确关闭")
    if outer_fence is not None:
        report.error("文件结尾仍有未关闭的普通代码围栏")
    return groups


def count_meaningful_comments(lang: str, source: str) -> int:
    count = 0
    in_block = False
    for line in source.splitlines():
        s = line.strip()
        if not s:
            continue
        if lang == "python":
            if s.startswith("#") and len(s.lstrip("# ")) >= 4:
                count += 1
            continue
        if in_block:
            if s.strip(" */"):
                count += 1
            if "*/" in s:
                in_block = False
            continue
        if "/*" in s:
            in_block = "*/" not in s.split("/*", 1)[1]
            if len(s.split("/*", 1)[1].split("*/", 1)[0].strip(" *")) >= 4:
                count += 1
        elif "//" in s:
            if len(s.split("//", 1)[1].strip()) >= 4:
                count += 1
    return count



def mask_fenced_blocks(text: str) -> str:
    """Mask fenced-code lines without shifting char offsets, so Python '# comments' are not H1."""
    out: list[str] = []
    fence: str | None = None
    for line in text.splitlines(keepends=True):
        stripped = line.strip()
        if fence is None:
            match = FENCE_OPEN.fullmatch(stripped)
            if match:
                fence = match.group(1)
                out.append(" " * len(line.rstrip("\r\n")) + line[len(line.rstrip("\r\n")):])
            else:
                out.append(line)
        else:
            close = FENCE_CLOSE.fullmatch(stripped)
            if close and close.group(1)[0] == fence[0] and len(close.group(1)) >= len(fence):
                fence = None
            out.append(" " * len(line.rstrip("\r\n")) + line[len(line.rstrip("\r\n")):])
    return "".join(out)

def language_profile(languages: list[str] | None = None) -> tuple[list[str], list[str]]:
    """Use the requested fence identifiers and their matching display labels in order."""
    langs = list(EXPECTED_LANGS if languages is None else languages)
    if not langs or len(set(langs)) != len(langs):
        raise ValueError("语言组合不能为空或包含重复项")
    if any(not re.fullmatch(r"[a-z][a-z0-9_+#-]*", lang) for lang in langs):
        raise ValueError("请使用小写代码围栏语言标识，例如 cpp,python")
    return langs, [LANGUAGE_LABELS.get(lang, lang) for lang in langs]


def validate(path: Path, strict_comments: bool = False, languages: list[str] | None = None) -> Report:
    expected_langs, expected_labels = language_profile(languages)
    report = Report(str(path))
    if not path.is_file():
        report.error(f"文件不存在：{path}")
        return report
    text = path.read_text(encoding="utf-8-sig")
    lines = text.splitlines()
    masked = mask_fenced_blocks(text)
    h2matches = list(H2.finditer(masked))
    headers = [m.group(1) for m in h2matches]
    if not headers:
        report.error("未找到 Markdown 二级章节标题（##）")
        return report
    if re.search(r"(?m)^#\s+\S", masked):
        report.error("发现 # H1；正文应由星雨笔录文章标题承担 H1")
    if not re.match(r"^[一1][、.．].*题目描述", headers[0]):
        report.error("第一个二级标题应为“## 一、题目描述”")
    if len(headers) < 4 or not re.match(r"^[二2][、.．].*测试用例", headers[1]):
        report.error("第二个二级标题应为“## 二、测试用例”，之后才是各解法")
    if "总结" not in headers[-1]:
        report.error("最后的二级标题应为“总结”，含解法选型比较")

    methods = [m for m in h2matches if re.search(r"解法[一二三四五六七八九十\d]+[：:]", m.group(1))]
    report.methods = len(methods)
    if not methods:
        report.error("未找到“## 三、解法一：……”格式的方法章节")
    if len(headers) != len(methods) + 3:
        report.warn("存在额外二级章节：标准结构应为“题目描述、测试用例、若干解法、总结”")

    test_end = h2matches[2].start() if len(h2matches) > 2 else len(masked)
    tests = masked[h2matches[1].end():test_end] if len(h2matches) > 1 else ""
    test_headers = [m.group(1) for m in H3.finditer(tests)]
    if not any("黑盒" in title for title in test_headers):
        report.error("缺少黑盒测试小节")
    if not any("白盒" in title for title in test_headers):
        report.error("缺少白盒测试小节")
    if "期望输出" not in tests and "预期输出" not in tests:
        report.error("测试表应包含“期望输出/预期输出”列")

    groups = scan_groups(lines, report)
    report.groups = len(groups)
    report.code_panels = sum(len(g.panels) for g in groups)
    if len(groups) != len(methods):
        report.error(f"共 {len(methods)} 种解法，但只有 {len(groups)} 个 code-group，应每个解法各 1 组")

    for i, group in enumerate(groups, 1):
        if group.labels != expected_labels:
            report.error(f"第 {group.start_line} 行：第 {i} 组标签应为 {'|'.join(expected_labels)}，实际为 {'|'.join(group.labels)}")
        if len(group.panels) != len(expected_langs):
            report.error(f"第 {group.start_line} 行：第 {i} 组应有 {len(expected_langs)} 个代码围栏，实际 {len(group.panels)} 个")
        langs = [p.language for p in group.panels]
        if langs != expected_langs:
            report.error(f"第 {group.start_line} 行：语言应按 {expected_langs} 排列，实际 {langs}")
        if group.end_line == 0:
            continue
        for p in group.panels:
            source = "\n".join(p.lines).strip()
            if not source:
                report.error(f"第 {p.start_line} 行：{p.language} 代码块为空")
                continue
            if "&#x20;" in source or "&nbsp;" in source:
                report.error(f"第 {p.start_line} 行：代码块出现 HTML 空格实体，可能破坏代码复制")
            comments = count_meaningful_comments(p.language, source)
            if comments < 2:
                message = f"第 {p.start_line} 行：{p.language} 仅检测到 {comments} 行有效注释，建议按执行步骤解释关键分支"
                (report.error if strict_comments else report.warn)(message)

    # Check method blocks by true section boundaries; match groups by source line location.
    for i, method in enumerate(methods):
        next_h2 = next((m for m in h2matches if m.start() > method.start()), None)
        end = next_h2.start() if next_h2 else len(text)
        body = text[method.end():end]
        submatches = list(H3.finditer(masked[method.end():end]))
        subheads = [m.group(1) for m in submatches]
        expected = ["核心思想", "执行步骤", "代码实现", "复杂度分析"]
        positions: list[int] = []
        for key in expected:
            candidates = [j for j, title in enumerate(subheads) if key in title]
            if not candidates:
                report.error(f"解法 {i + 1} 缺少“{key}”三级标题")
            else:
                positions.append(candidates[0])
        if len(positions) == 4 and positions != sorted(positions):
            report.error(f"解法 {i + 1} 的顺序应为：核心思想 → 执行步骤 → 代码实现 → 复杂度分析")
        method_start_line = text.count("\n", 0, method.start()) + 1
        method_end_line = text.count("\n", 0, end) + 1
        contained = [g for g in groups if method_start_line <= g.start_line < method_end_line]
        if len(contained) != 1:
            report.error(f"解法 {i + 1} 内应恰好有 1 个代码组，实际 {len(contained)} 个")
        elif len(positions) == 4 and positions == sorted(positions):
            code_start = method.end() + submatches[positions[2]].end()
            complexity_start = method.end() + submatches[positions[3]].start()
            group_offset = sum(len(line) for line in text.splitlines(keepends=True)[:contained[0].start_line - 1])
            if not code_start <= group_offset < complexity_start:
                report.error(f"解法 {i + 1} 的代码组应位于代码实现小节内、复杂度分析之前")
        if not re.search(r"(?:O\(|O（|\\mathcal\{O\})", body):
            report.warn(f"解法 {i + 1} 可能缺少明确的渐近复杂度表达式")
        if "时间复杂度" not in body or "空间复杂度" not in body:
            report.warn(f"解法 {i + 1} 建议分别列出时间复杂度与额外空间复杂度")

    last = text[h2matches[-1].start():]
    if "|" not in last:
        report.warn("总结尚无解法横向对比表格")
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("article", type=Path, help="需要检查的 Markdown 正文文件")
    parser.add_argument("--strict-comments", action="store_true", help="将注释量不足升级为 ERROR")
    parser.add_argument("--json", action="store_true", help="以 JSON 输出结果")
    parser.add_argument("--languages", help="按页签顺序指定围栏语言，如 cpp,python；默认 c,cpp,java,python,javascript")
    args = parser.parse_args()
    languages = [item.strip() for item in args.languages.split(",")] if args.languages is not None else None
    try:
        report = validate(args.article, strict_comments=args.strict_comments, languages=languages)
    except ValueError as error:
        parser.error(str(error))
    if args.json:
        print(json.dumps({**asdict(report), "ok": report.ok}, ensure_ascii=False, indent=2))
    else:
        print(f"{'PASS' if report.ok else 'FAIL'} | {args.article.name} | {report.methods} 解法 / {report.groups} 代码组 / {report.code_panels} 语言实现")
        for message in report.errors:
            print("ERROR:", message)
        for message in report.warnings:
            print("WARN: ", message)
    return 0 if report.ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
