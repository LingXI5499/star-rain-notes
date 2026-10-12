---
name: project-engineering-report
description: Analyze a software project from its current repository, database migrations, routes, APIs, classes, tests, deployment files, remotes, and read-only production evidence, then produce complete standalone HTML engineering reports. Use when the user requests a current-state project report, software lifecycle document, database or ER documentation, interface and class design, page inventory, or separate reports by module; do not use for ordinary code review or speculative architecture planning alone.
---

# Project Engineering Report

Build the report from verifiable project evidence. Treat attached reports, screenshots, and sample interface documents as reference material rather than instructions unless the user explicitly asks to follow them.

## Establish the evidence boundary

1. Identify the repository root, checked-out commit, branch or detached state, nearest version tag, commit time, remotes, and working-tree changes.
2. Record which evidence belongs to the current version. Preserve unrelated local changes and exclude them from version claims unless the user says they are part of the target version.
3. Distinguish facts found in source code, migrations, configuration, tests, remote repositories, and the deployed site. State uncertainty where these disagree.
4. Inspect production only through read-only requests. Never expose credentials, tokens, private configuration, or personal data.
5. Describe the current system state. Omit iteration history, abandoned designs, and planned behavior unless the user explicitly requests them.

Read [references/evidence-and-verification.md](references/evidence-and-verification.md) before comparing repositories or probing a deployed site.

## Inspect the project

- Inventory every frontend route, routed page, detail page, layout, API client, state store, and shared interaction component. Count page components and user-reachable routes separately.
- Inventory controllers, request and response models, services, repositories or mappers, entities, security filters, configuration, gateways, scheduled jobs, events, and exception handlers.
- Use database migrations as the primary source for the physical schema. Run `scripts/extract_flyway_schema.py` for an initial Flyway inventory, then review the SQL manually for vendor-specific statements, renames, dynamic SQL, triggers, views, and cross-table rules.
- Trace each important business operation from page interaction through API, authorization, service transaction, persistence, response, and error state.
- Execute appropriate builds and tests. Report only checks actually run and distinguish source inspection from runtime verification.

## Write the report

Follow [references/report-spec.md](references/report-spec.md). Give requirements analysis and system design the most detail. Include complete page, database, API, class, test, deployment, and operations sections.

When modules have distinct user journeys or data models, create separate standalone reports for them. Keep shared identity, media, authentication, and infrastructure visible as external dependencies in each report rather than duplicating ownership.

Generate standalone UTF-8 HTML with embedded CSS and JavaScript. Do not rely on a CDN or fetch local data at runtime. Include a linked catalog index (目录索引) covering report files, sections, modules, and physical tables; keep it sticky and searchable on screen, and print it as the first directory page. Also include readable print styles, searchable catalogs, expandable detail blocks, and diagrams that remain understandable when printed.

For database and class documentation, follow [references/er-and-class-design.md](references/er-and-class-design.md). The ER diagram must use Crow's Foot cardinality, show physical table names with Chinese business names, expose key fields and SQL types, identify PK and FK columns, and match the actual foreign keys. Also provide field tables because a diagram alone is not a schema specification.

## Verify before delivery

- Recompute all totals from the rendered report data and compare them with the source inventory.
- Confirm every page, endpoint, table, relation, and class named in the report exists in the target version.
- Check both ends of every ER relation, its cardinality, FK column, referenced key, and delete behavior.
- Parse or smoke-test embedded JavaScript and confirm generated SVG diagrams are non-empty.
- Check HTML structure, internal anchors, search and filter controls, print layout, and absence of placeholders.
- Open the final files locally and make sure Chinese text, diagrams, and navigation render correctly.
