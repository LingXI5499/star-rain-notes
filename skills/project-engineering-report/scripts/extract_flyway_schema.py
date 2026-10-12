#!/usr/bin/env python3
"""Create a reviewable JSON inventory from common MySQL Flyway migrations.

This helper is intentionally conservative and is not a complete SQL parser. Review
its warnings and manually inspect dynamic SQL, renames, triggers, views, procedures,
and vendor-specific syntax before using the result in a report.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any


IDENT = r"`?([A-Za-z_][A-Za-z0-9_$]*)`?"
CREATE_RE = re.compile(
    rf"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?{IDENT}\s*\((.*?)\)\s*(?:ENGINE\b[^;]*|;)",
    re.IGNORECASE | re.DOTALL,
)
ALTER_RE = re.compile(rf"ALTER\s+TABLE\s+{IDENT}\s+(.*?);", re.IGNORECASE | re.DOTALL)
TABLE_COMMENT_RE = re.compile(r"COMMENT\s*=\s*'((?:''|[^'])*)'", re.IGNORECASE)
COLUMN_RE = re.compile(
    rf"^\s*`?([A-Za-z_][A-Za-z0-9_$]*)`?\s+"
    r"([A-Za-z]+(?:\s+[A-Za-z]+)?(?:\s*\([^)]*\))?(?:\s+UNSIGNED)?)\s*(.*)$",
    re.IGNORECASE | re.DOTALL,
)
PK_RE = re.compile(r"PRIMARY\s+KEY\s*\(([^)]*)\)", re.IGNORECASE)
UNIQUE_RE = re.compile(
    rf"UNIQUE\s+(?:KEY|INDEX)?\s*(?:`?[A-Za-z_][A-Za-z0-9_$]*`?\s*)?\(([^)]*)\)",
    re.IGNORECASE,
)
FK_RE = re.compile(
    rf"(?:CONSTRAINT\s+`?([A-Za-z_][A-Za-z0-9_$]*)`?\s+)?"
    rf"FOREIGN\s+KEY\s*\(([^)]*)\)\s+REFERENCES\s+{IDENT}\s*\(([^)]*)\)"
    r"(?:\s+ON\s+DELETE\s+(CASCADE|RESTRICT|SET\s+NULL|NO\s+ACTION))?"
    r"(?:\s+ON\s+UPDATE\s+(CASCADE|RESTRICT|SET\s+NULL|NO\s+ACTION))?",
    re.IGNORECASE | re.DOTALL,
)
COMMENT_RE = re.compile(r"\bCOMMENT\s+'((?:''|[^'])*)'", re.IGNORECASE)
DEFAULT_RE = re.compile(
    r"\bDEFAULT\s+((?:'(?:(?:'')|[^'])*')|(?:\([^)]*\))|[^\s,]+)", re.IGNORECASE
)

NON_COLUMN_PREFIXES = (
    "primary key",
    "foreign key",
    "constraint",
    "unique key",
    "unique index",
    "key ",
    "index ",
    "check ",
    "fulltext ",
    "spatial ",
)


def split_sql_items(text: str) -> list[str]:
    """Split a comma-separated SQL list while respecting quotes and parentheses."""
    items: list[str] = []
    start = 0
    depth = 0
    quote: str | None = None
    escaped = False
    for index, char in enumerate(text):
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                if index + 1 < len(text) and text[index + 1] == quote:
                    continue
                quote = None
        elif char in ("'", '"', "`"):
            quote = char
        elif char == "(":
            depth += 1
        elif char == ")":
            depth = max(0, depth - 1)
        elif char == "," and depth == 0:
            items.append(text[start:index].strip())
            start = index + 1
    tail = text[start:].strip()
    if tail:
        items.append(tail)
    return items


def names(fragment: str) -> list[str]:
    return [part.strip().strip("`").split()[0] for part in fragment.split(",")]


def parse_column(fragment: str) -> dict[str, Any] | None:
    if fragment.lower().lstrip().startswith(NON_COLUMN_PREFIXES):
        return None
    match = COLUMN_RE.match(fragment)
    if not match:
        return None
    name, sql_type, tail = match.groups()
    default_match = DEFAULT_RE.search(tail)
    comment_match = COMMENT_RE.search(tail)
    return {
        "name": name,
        "type": " ".join(sql_type.upper().split()),
        "nullable": not bool(re.search(r"\bNOT\s+NULL\b", tail, re.IGNORECASE)),
        "default": default_match.group(1) if default_match else None,
        "auto_increment": bool(re.search(r"\bAUTO_INCREMENT\b", tail, re.IGNORECASE)),
        "comment": comment_match.group(1).replace("''", "'") if comment_match else None,
        "primary_key": bool(re.search(r"\bPRIMARY\s+KEY\b", tail, re.IGNORECASE)),
        "unique": bool(re.search(r"\bUNIQUE\b", tail, re.IGNORECASE)),
    }


def table_template(name: str) -> dict[str, Any]:
    return {
        "name": name,
        "comment": None,
        "columns": [],
        "primary_key": [],
        "unique_keys": [],
        "foreign_keys": [],
        "migrations": [],
    }


def add_foreign_keys(table: dict[str, Any], fragment: str, migration: str) -> None:
    for match in FK_RE.finditer(fragment):
        constraint, child_cols, parent, parent_cols, on_delete, on_update = match.groups()
        relation = {
            "name": constraint,
            "columns": names(child_cols),
            "referenced_table": parent,
            "referenced_columns": names(parent_cols),
            "on_delete": (on_delete or "DATABASE DEFAULT").upper(),
            "on_update": (on_update or "DATABASE DEFAULT").upper(),
            "migration": migration,
        }
        if relation not in table["foreign_keys"]:
            table["foreign_keys"].append(relation)


def apply_create(schema: dict[str, dict[str, Any]], sql: str, migration: str) -> None:
    for match in CREATE_RE.finditer(sql):
        table_name, body = match.group(1), match.group(2)
        table = schema.setdefault(table_name, table_template(table_name))
        table["migrations"].append(migration)
        full_statement = match.group(0)
        comment_match = TABLE_COMMENT_RE.search(full_statement)
        if comment_match:
            table["comment"] = comment_match.group(1).replace("''", "'")
        for item in split_sql_items(body):
            column = parse_column(item)
            if column:
                existing = next((c for c in table["columns"] if c["name"] == column["name"]), None)
                if existing:
                    existing.update(column)
                else:
                    table["columns"].append(column)
                continue
            pk = PK_RE.search(item)
            if pk:
                table["primary_key"] = names(pk.group(1))
            unique = UNIQUE_RE.search(item)
            if unique:
                cols = names(unique.group(1))
                if cols not in table["unique_keys"]:
                    table["unique_keys"].append(cols)
            add_foreign_keys(table, item, migration)
        for column in table["columns"]:
            if column["primary_key"] and column["name"] not in table["primary_key"]:
                table["primary_key"].append(column["name"])


def apply_alter(schema: dict[str, dict[str, Any]], sql: str, migration: str, warnings: list[str]) -> None:
    for match in ALTER_RE.finditer(sql):
        table_name, body = match.groups()
        table = schema.setdefault(table_name, table_template(table_name))
        table["migrations"].append(migration)
        add_foreign_keys(table, body, migration)
        for action in split_sql_items(body):
            add = re.match(r"\s*ADD\s+(?:COLUMN\s+)?(.+)$", action, re.IGNORECASE | re.DOTALL)
            modify = re.match(r"\s*MODIFY\s+(?:COLUMN\s+)?(.+)$", action, re.IGNORECASE | re.DOTALL)
            drop = re.match(rf"\s*DROP\s+(?:COLUMN\s+)?{IDENT}", action, re.IGNORECASE)
            if add:
                column = parse_column(add.group(1))
                if column:
                    existing = next((c for c in table["columns"] if c["name"] == column["name"]), None)
                    if existing:
                        existing.update(column)
                    else:
                        table["columns"].append(column)
            elif modify:
                column = parse_column(modify.group(1))
                if column:
                    existing = next((c for c in table["columns"] if c["name"] == column["name"]), None)
                    if existing:
                        existing.update(column)
                    else:
                        table["columns"].append(column)
            elif drop:
                dropped = drop.group(1)
                table["columns"] = [c for c in table["columns"] if c["name"] != dropped]
            elif not FK_RE.search(action) and not re.match(
                r"\s*(ADD|DROP)\s+(CONSTRAINT|PRIMARY|UNIQUE|KEY|INDEX)", action, re.IGNORECASE
            ):
                warnings.append(f"Review ALTER action in {migration} for {table_name}: {action[:120]}")


def version_key(path: Path) -> tuple[Any, ...]:
    prefix = path.name.split("__", 1)[0].lstrip("Vv")
    return tuple(int(part) if part.isdigit() else part for part in re.split(r"[._-]", prefix))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("migration_dir", type=Path, help="Directory containing V*.sql migrations")
    parser.add_argument("--prefix", help="Regex selecting table names after the complete schema is parsed")
    parser.add_argument("--output", type=Path, help="Write JSON to this file instead of stdout")
    args = parser.parse_args()

    files = sorted(args.migration_dir.rglob("V*.sql"), key=version_key)
    if not files:
        parser.error(f"No V*.sql files found under {args.migration_dir}")

    schema: dict[str, dict[str, Any]] = {}
    warnings: list[str] = []
    for path in files:
        sql = path.read_text(encoding="utf-8-sig", errors="replace")
        apply_create(schema, sql, path.name)
        apply_alter(schema, sql, path.name, warnings)

    selected = schema
    if args.prefix:
        pattern = re.compile(args.prefix)
        selected = {name: table for name, table in schema.items() if pattern.search(name)}

    relations = []
    for table in selected.values():
        for fk in table["foreign_keys"]:
            relations.append({"child_table": table["name"], **fk})

    result = {
        "migration_directory": str(args.migration_dir.resolve()),
        "migration_files": len(files),
        "table_count": len(selected),
        "column_count": sum(len(table["columns"]) for table in selected.values()),
        "relation_count": len(relations),
        "tables": sorted(selected.values(), key=lambda table: table["name"]),
        "relations": relations,
        "warnings": warnings,
    }
    rendered = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output:
        args.output.write_text(rendered + "\n", encoding="utf-8")
    else:
        print(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
